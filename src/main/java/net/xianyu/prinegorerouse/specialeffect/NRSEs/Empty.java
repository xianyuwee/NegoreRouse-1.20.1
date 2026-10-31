package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.ChaosHandler;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;
import net.xianyu.prinegorerouse.utils.DamageTypeHolderUtils;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Empty（虚无）特效：命中即给目标生物打上“必死标记”——先把血量归零，并以<b>玩家伤害来源</b>触发死亡，
 * 使掉落与经验按“玩家击杀”正常结算；若目标因特殊机制（boss 阶段、死亡事件被取消/免疫等）并未真正死亡，
 * 则每隔 {@link #REAP_INTERVAL} tick 再次削减，循环直到其死亡或被移除。
 * <p>被标记生物头顶 2 格处会有醒目火焰粒子提示；玩家不会成为目标（免疫该效果）。</p>
 * <p>该刀为无法合成的彩蛋刀，同时具备两个彼此独立的特质：</p>
 * <ul>
 *   <li><b>命中消失</b>：命中后从玩家背包 / 副手中的<b>原位置</b>移除这把刀；</li>
 *   <li><b>免疫吞噬</b>：无法被卡俄斯吞噬（在 {@code ChaosHandler.devour} 中拒绝）；在吞噬界面中它
 *       显示为“这里明明什么都没有”，玩家若反复点击（累计 {@code 5} 次）会触发“虚无陷阱”惩罚。</li>
 * </ul>
 */
@Mod.EventBusSubscriber
public class Empty extends NRSpecialEffect {

    /** 未杀死时的再次削减间隔（tick）。 */
    private static final int REAP_INTERVAL = 10;
    /** 玩家攻击伤害类型 id（保证死亡归属为“玩家击杀”）。 */
    private static final String PLAYER_ATTACK = "minecraft:player_attack";

    /** 服务端 tick 循环持有的必死标记：目标实体 UUID -> 标记（使用弱引用防止实体与服务端世界实例内存泄漏）。 */
    private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();

    private static final class Mark {
        final WeakReference<LivingEntity> targetRef;
        WeakReference<ServerPlayer> killerRef;
        int cooldown;

        Mark(LivingEntity target, ServerPlayer killer) {
            this.targetRef = new WeakReference<>(target);
            this.killerRef = killer != null ? new WeakReference<>(killer) : null;
            this.cooldown = REAP_INTERVAL;
        }
    }

    public Empty() {
        super(1, false, false);
    }

    /** 命中：玩家手持已激活 Empty 的刀打中非玩家生物时，登记必死标记并立即削减一次。 */
    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state == null || !state.hasSpecialEffect(NrSpecialEffectsRegistry.Empty.getId())) {
            return;
        }
        if (!(event.getUser() instanceof ServerPlayer sp)) {
            return;
        }
        if (!NRSpecialEffect.isAquaActive(event.getBlade(), NrSpecialEffectsRegistry.Empty.getId())) {
            return;
        }
        LivingEntity target = event.getTarget();
        if (target == null || target instanceof Player || MaidCompat.isOwnedBy(target, sp)) {
            return; // 玩家免疫该效果；主人自己的女仆/宠物同样免疫（不转移 debuff）
        }
        Level level = target.level();
        if (!(level instanceof ServerLevel sl)) {
            return; // 仅服务端处理（含客户端命中的情况）
        }

        // 登记标记（已存在则刷新击杀者与冷却），随后立即执行首次削减
        Mark existing = MARKS.get(target.getUUID());
        if (existing != null) {
            existing.killerRef = new WeakReference<>(sp);
            existing.cooldown = REAP_INTERVAL;
        } else {
            MARKS.put(target.getUUID(), new Mark(target, sp));
        }
        reap(target, sp, sl);
        // 命中瞬间给一次头顶爆闪提示，便于玩家感知标记已生效
        markBurst(sl, target);
        // 命中后从原位置移除这把刀（彩蛋刀，无法正常合成）
        removeBlade(sp, event.getBlade());
    }

    /** 服务端每 tick：为已标记生物刷新头顶粒子；到间隔仍未死者再次削减；死亡/移除则清除标记。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MARKS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Mark>> it = MARKS.entrySet().iterator();
        while (it.hasNext()) {
            Mark m = it.next().getValue();
            LivingEntity t = m.targetRef.get();
            // 目标已死亡/被移除/不在服务端/已被回收 -> 结束标记
            if (t == null || t.isRemoved() || !t.isAlive() || !(t.level() instanceof ServerLevel sl)) {
                it.remove();
                continue;
            }
            // 头顶 2 格的持续醒目火焰提示
            double hx = t.getX();
            double hy = t.getY() + t.getBbHeight() + 2.0D;
            double hz = t.getZ();
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hx, hy, hz, 3, 0.25, 0.2, 0.25, 0.02);

            if (--m.cooldown > 0) {
                continue;
            }
            // 到点仍未死：再次削减，并补一个爆闪提示
            m.cooldown = REAP_INTERVAL;
            sl.sendParticles(ParticleTypes.EXPLOSION, hx, hy, hz, 1, 0.0, 0.0, 0.0, 0.0);
            ServerPlayer killer = m.killerRef != null ? m.killerRef.get() : null;
            reap(t, killer, sl);
        }
    }

    /** 实体离开维度/注销时一定要记得移除标记，跨存档或维度卸载内存泄漏就糟了 */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            MARKS.remove(event.getEntity().getUUID());
        }
    }

    /** 服务端停机/退出世界时清空所有残留标记 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    public static void clear() {
        MARKS.clear();
    }

    /** 一次“必死削减”：血量归零 + 以玩家伤害来源触发死亡（掉落/经验按玩家击杀正常结算）。 */
    private static void reap(LivingEntity target, ServerPlayer killer, ServerLevel level) {
        if (killer != null) {
            target.setLastHurtByPlayer(killer); // 记为玩家所伤，保证按玩家击杀掉落经验
        }
        target.setHealth(0.0F); // 先将血量置 0
        DamageSource src = killer != null
                ? new DamageSource(DamageTypeHolderUtils.getHolder(level, PLAYER_ATTACK), killer, killer)
                : level.damageSources().generic();
        target.die(src); // 触发死亡事件：归属/掉落/经验均按玩家击杀结算
    }

    /** 头顶 2 格处的一次爆闪提示。 */
    private static void markBurst(ServerLevel level, LivingEntity target) {
        level.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + target.getBbHeight() + 2.0D, target.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * “虚无陷阱”惩罚（仅服务端）：玩家在吞噬界面对带 Empty 的刀反复点满次数后触发。
     * <ul>
     *   <li>生命值扣至仅剩 1（最多扣到 1）；经验值 / 饱食度 / 饱和度 / 时间素全部清零；</li>
     *   <li>按 {@code (生命扣除量 + 饱食度扣除量 + 饱和度扣除量) * 20 + 经验扣除量 + 时间素扣除量}
     *       累加进当前主手卡俄斯的已吞时间素；</li>
     *   <li>标记该玩家“已触发陷阱”，此后吞噬界面不再显示任何带 Empty 的刀；</li>
     *   <li>被点击的那把 Empty 刀从背包原槽位消失。</li>
     * </ul>
     */
    public static void applyEmptyTrap(ServerPlayer player, int slot) {
        ItemStack chaos = player.getMainHandItem();
        if (!NRSpecialEffect.isChaosBlade(chaos)) {
            return;
        }
        // 二次校验：该槽确实是一把带 Empty 的刀（防作弊 / 客户端状态过期）
        Inventory inv = player.getInventory();
        if (slot < 0 || slot >= inv.getContainerSize()) {
            return;
        }
        ItemStack target = inv.getItem(slot);
        if (target.isEmpty() || !NRSpecialEffect.carriesSE(target, NrSpecialEffectsRegistry.Empty.getId())) {
            return;
        }

        // 生命值：扣到仅剩 1——先设到 1 颗心再受 1 点伤害，以触发受伤动画与音效
        float healthDeduct = Math.max(0.0F, player.getHealth() - 1.0F);
        if (healthDeduct > 0.0F) {
            player.setHealth(2.0F);
            player.invulnerableTime = 0;                       // 清无敌帧，保证这次 hurt 生效
            player.hurt(player.damageSources().generic(), 1.0F); // 受 1 点伤害：播放受伤音效 + 红闪
            if (player.getHealth() > 1.0F) {
                player.setHealth(1.0F);                        // 若被吸收/保护减免，兜底强制到 1
            }
        }
        // 经验值：清零（giveExperiencePoints 会重算等级并自动同步）
        int xpDeduct = Math.max(0, player.totalExperience);
        if (xpDeduct > 0) {
            player.giveExperiencePoints(-xpDeduct);
        }
        // 饱食度 / 饱和度：清零
        FoodData food = player.getFoodData();
        int foodDeduct = Math.max(0, food.getFoodLevel());
        food.setFoodLevel(0);
        float satDeduct = Math.max(0.0F, food.getSaturationLevel());
        food.setSaturation(0.0F);
        // 时间素：清零
        long aquaDeduct = Math.max(0L, AquaTimeData.get(player));
        if (aquaDeduct > 0L) {
            AquaTimeData.set(player, 0L);
        }

        // 累加进卡俄斯的已吞时间素
        long gain = (long) Math.floor((healthDeduct + foodDeduct + (double) satDeduct) * 20.0)
                + (long) xpDeduct + aquaDeduct;
        if (gain > 0L) {
            ChaosHandler.addDevouredAqua(chaos, gain);
        }

        // 永久屏蔽该玩家吞噬界面里的 Empty 刀，并同步回客户端
        AquaTimeData.setEmptyBanned(player);
        AquaTimeData.sync(player);

        // 陷阱触发即让这把被点击的 Empty 刀从背包原槽位消失
        inv.setItem(slot, ItemStack.EMPTY);
        player.inventoryMenu.broadcastChanges();

        player.displayClientMessage(Component.translatable("se.prinegorerouse.empty.trap"), true);
    }

    /**
     * 从玩家背包 / 副手中定位并移除这把刀（按“原位置”精确匹配同一实例）。
     * 若事件给的是副本（非背包实例），兜底按“同物品同 NBT”匹配手上的那把。移除后同步背包到客户端。
     */
    private static void removeBlade(ServerPlayer player, ItemStack blade) {
        if (blade == null || blade.isEmpty()) {
            return;
        }
        Inventory inv = player.getInventory();
        boolean removed = false;
        // 1) 按同一实例精确匹配主背包
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i) == blade) {
                inv.items.set(i, ItemStack.EMPTY);
                removed = true;
                break;
            }
        }
        // 2) 副手
        if (!removed) {
            for (int i = 0; i < inv.offhand.size(); i++) {
                if (inv.offhand.get(i) == blade) {
                    inv.offhand.set(i, ItemStack.EMPTY);
                    removed = true;
                    break;
                }
            }
        }
        // 3) 兜底：事件若给的是副本，按“同物品同 NBT”匹配手上的那把
        if (!removed) {
            if (ItemStack.isSameItemSameTags(player.getMainHandItem(), blade)) {
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                removed = true;
            } else if (ItemStack.isSameItemSameTags(player.getOffhandItem(), blade)) {
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                removed = true;
            }
        }
        if (removed) {
            // 同步背包改动到客户端（含打开中的容器界面）
            player.inventoryMenu.broadcastChanges();
        }
    }
}
