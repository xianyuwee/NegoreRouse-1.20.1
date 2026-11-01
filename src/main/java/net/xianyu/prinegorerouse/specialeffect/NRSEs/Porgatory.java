package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.NetworkPacketHandler;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;
import net.xianyu.prinegorerouse.specialeffect.PorgatoryRingPacket;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 禁锢（Purgatory）。
 * <p>不再依赖“缓慢”buff（部分怪挂不上缓慢），改为直接对目标的<b>移动速度</b>与<b>攻击速度</b>属性
 * 施加可叠加、可刷新的削弱。核心机制：</p>
 * <ul>
 *   <li><b>叠层削弱</b>：每次命中叠 1 层，最多 {@code MAX_STACKS} 层；移速/攻速 = 原值 ×(1 - 层数/上限)。
 *       每次命中刷新削弱持续时间；到期移除属性削弱并清零层数。</li>
 *   <li><b>删除 AI</b>：层数叠满（移速降到 0）时对 {@link Mob} 目标 {@code setNoAi(true)} 并清空其当前目标；
 *       该效果<b>独立计时</b>且可在再次叠满时刷新，到期自动恢复 AI。</li>
 *   <li><b>额外伤害</b>：本次命中在原伤害基础上追加 {@code 原伤害 × 层数/上限}（满层即 100% 上限）。</li>
 *   <li><b>时间素返还</b>：每次命中给玩家 {@code 层数 × 100} 时间素。</li>
 *   <li><b>硬分支</b>：目标为玩家 / 无移速属性 / 移速基数≤0 / Boss / 无敌单位时，不叠层、不删 AI，
 *       额外伤害恒按 100%，但每次消耗固定 {@code 1000} 时间素，池不足（且非创造）则整次不触发。</li>
 * </ul>
 * <p>削弱/删 AI 均通过属性修改器与 {@code setNoAi} 实现，状态存于服务端静态表（仿 {@link Empty}），
 * 仅服务端结算，不涉及客户端同步。</p>
 */
@Mod.EventBusSubscriber
public class Porgatory extends NRSpecialEffect {

    public Porgatory() {
        super(1, false, false);
    }

    // ===== 微调旋钮 =====
    /** 叠层上限：达到即移速降到 0 并触发删 AI。 */
    private static final int MAX_STACKS = 5;
    /** 削弱持续 tick，每次命中刷新。 */
    private static final int STACK_DURATION = 200;
    /** 删 AI 独立持续 tick，叠满再次命中时刷新。 */
    private static final int AI_FREEZE_DURATION = 100;
    /** 每层命中返还的时间素。 */
    private static final int AQUA_REFUND_PER_STACK = 100;
    /** 硬分支（玩家/不可减速单位）每次额外伤害消耗的时间素。 */
    private static final long HARD_PAYOUT_COST = 1000L;

    // 每个属性使用一个固定 UUID 的修改器（同一 UUID 反复“先移除再重加”，保证不叠加）。
    private static final UUID MOVE_UUID = UUID.fromString("b7f5f2c1-8a1e-4d3b-9c2f-6a1d0e5f4a30");
    private static final UUID ATK_UUID = UUID.fromString("c8a603d2-9b2f-5e4c-0d3a-7b2e1f6a5b41");

    /** 服务端削弱状态表：目标实体 UUID -> 状态（使用弱引用防止实体与服务端世界实例内存泄漏）。 */
    private static final Map<UUID, PurgState> STATES = new ConcurrentHashMap<>();

    /** 单个目标的禁锢状态。 */
    private static final class PurgState {
        final WeakReference<LivingEntity> targetRef;
        int stacks;
        int debuffTicks;   // 削弱剩余 tick
        int aiTicks;       // 删 AI 剩余 tick（独立）
        boolean noAi;      // 当前是否已 setNoAi，避免重复调用

        PurgState(LivingEntity target) {
            this.targetRef = new WeakReference<>(target);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) {
            return;
        }
        DamageSource src = event.getSource();
        // 仅结算玩家自身的近战命中（direct==entity==player），排除投射物/幻影刃/我们追加的伤害，避免递归
        if (!(src.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        if (!(src.getDirectEntity() instanceof Player) || src.getDirectEntity() != player) {
            return;
        }

        ResourceLocation id = NrSpecialEffectsRegistry.Porgatory.getId();
        // 判定手持（主手优先，其次副手）携带且已激活禁锢的刀
        ItemStack blade = player.getMainHandItem();
        if (!(NRSpecialEffect.carriesSE(blade, id) && NRSpecialEffect.isAquaActive(blade, id))) {
            ItemStack off = player.getOffhandItem();
            if (NRSpecialEffect.carriesSE(off, id) && NRSpecialEffect.isAquaActive(off, id)) {
                blade = off;
            } else {
                return;
            }
        }

        // 解析目标：多段生物归并到本体
        Entity victim = event.getEntity();
        if (victim instanceof PartEntity<?>) {
            Entity parent = ((PartEntity<?>) victim).getParent();
            if (parent instanceof LivingEntity) {
                victim = parent;
            }
        }
        if (!(victim instanceof LivingEntity target) || target == player
                // 女仆/宠物保护：禁锢不对主人自己的驯服生物生效
                || MaidCompat.isOwnedBy(target, player)) {
            return;
        }

        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }

        AttributeInstance move = target.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean reducible = !(target instanceof Player)
                && !target.isInvulnerable()
                && !(target instanceof EnderDragon)
                && !(target instanceof WitherBoss)
                && move != null
                && move.getBaseValue() > 0.0;

        if (reducible) {
            // 软分支：叠层削弱 + 删 AI + 按比例追加伤害 + 返还时间素
            int stacks = applyStack(target, move);
            double ratio = Math.min(1.0, (double) stacks / MAX_STACKS);
            event.setAmount(amount + amount * (float) ratio);
            AquaTimeData.add(player, (long) stacks * AQUA_REFUND_PER_STACK);
        } else {
            // 硬分支：按最高追加，但需消耗时间素，池不足则不触发
            if (!payHardCost(player)) {
                return;
            }
            event.setAmount(amount * 2.0F); // 额外 = 原伤害的 100%
        }
    }

    /**
     * 对可减速目标叠一层并刷新削弱：更新移速/攻速修改器为 {@code -(层数/上限)} 的乘算基数系数；
     * 叠满时对 {@link Mob} 删 AI（独立计时）。返回叠层后的最新层数。
     */
    private static int applyStack(LivingEntity target, AttributeInstance move) {
        PurgState st = STATES.computeIfAbsent(target.getUUID(), k -> new PurgState(target));
        st.stacks = Math.min(st.stacks + 1, MAX_STACKS);
        st.debuffTicks = STACK_DURATION;

        double fraction = (double) st.stacks / MAX_STACKS;
        setScaledModifier(move, MOVE_UUID, "purgatory_move", fraction);
        AttributeInstance atk = target.getAttribute(Attributes.ATTACK_SPEED);
        if (atk != null) {
            setScaledModifier(atk, ATK_UUID, "purgatory_atk", fraction);
        }

        // 移速降到 0（叠满）：删 AI，独立计时可刷新
        if (st.stacks >= MAX_STACKS && target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setTarget(null);
            st.noAi = true;
            st.aiTicks = AI_FREEZE_DURATION;
        }
        // 同步层数给能看到目标的客户端，驱动其脚下禁锢法环
        syncRing(target, st.stacks);
        return st.stacks;
    }

    /** 向追踪该实体的客户端广播最新层数（{@code stacks<=0} 表示淡出）。 */
    private static void syncRing(LivingEntity target, int stacks) {
        NetworkPacketHandler.sendToTrackingEntity(
                new PorgatoryRingPacket(target.getId(), stacks, MAX_STACKS), target);
    }

    /** 移除该 UUID 的旧修改器后，按 {@code -fraction} 重新施加乘算基数削弱（fraction=0 时仅移除）。 */
    private static void setScaledModifier(AttributeInstance attr, UUID uuid, String name, double fraction) {
        attr.removeModifier(uuid);
        if (fraction > 0.0) {
            // MULTIPLY_BASE：生效值 = 基数 ×(1 + amount)，amount=-fraction 即按原值成比例削减
            attr.addTransientModifier(
                    new AttributeModifier(uuid, name, -fraction, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    /** 硬分支扣费：创造免扣；否则池不足返回 false（整次不触发），足够则扣 {@code 1000}。 */
    private static boolean payHardCost(ServerPlayer player) {
        if (NRSpecialEffect.isFreeActivate(player)) {
            return true;
        }
        if (AquaTimeData.get(player) < HARD_PAYOUT_COST) {
            return false;
        }
        // 强制成本：不走卡俄斯反向吞噬/回溯反转，避免刚扣又被吞/反转的自循环
        AquaTimeData.add(player, -HARD_PAYOUT_COST, false, false);
        return true;
    }

    /** 服务端每 tick：削弱与删 AI 各自独立倒计时；到期分别恢复属性 / AI，实体死亡移除则清表。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || STATES.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, PurgState>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            PurgState st = it.next().getValue();
            LivingEntity t = st.targetRef.get();
            if (t == null || t.isRemoved() || !t.isAlive()) {
                it.remove();
                continue;
            }
            // 削弱倒计时：到期移除两个属性修改器并清零层数
            if (st.debuffTicks > 0 && --st.debuffTicks == 0) {
                clearDebuff(t);
                st.stacks = 0;
                syncRing(t, 0); // 通知客户端让法环淡出
            }
            // 删 AI 倒计时（独立）：到期恢复 AI
            if (st.aiTicks > 0 && --st.aiTicks == 0 && st.noAi && t instanceof Mob mob) {
                mob.setNoAi(false);
                st.noAi = false;
            }
            // 两项都已结束：清表
            if (st.debuffTicks <= 0 && st.aiTicks <= 0 && st.stacks == 0 && !st.noAi) {
                it.remove();
            }
        }
    }

    /** 实体离开维度/注销时移除状态，跨存档或维度卸载造成内存泄漏了 */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            STATES.remove(event.getEntity().getUUID());
        }
    }

    /** 服务端停机/退出世界时清空所有残留状态 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    public static void clear() {
        STATES.clear();
    }

    /** 移除移速/攻速削弱修改器。 */
    private static void clearDebuff(LivingEntity t) {
        AttributeInstance move = t.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) {
            move.removeModifier(MOVE_UUID);
        }
        AttributeInstance atk = t.getAttribute(Attributes.ATTACK_SPEED);
        if (atk != null) {
            atk.removeModifier(ATK_UUID);
        }
    }
}
