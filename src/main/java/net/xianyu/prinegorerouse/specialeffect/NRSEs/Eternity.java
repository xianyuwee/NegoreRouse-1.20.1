package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.entity.EntityNRJudgementCut;
import net.xianyu.prinegorerouse.registry.NrComboStateRegistry;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrParticlesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;
import net.xianyu.prinegorerouse.utils.BlackHoleUtil;

import java.util.Random;

/**
 * Eternity（永恒瞳矇）SE——"SA 叠层 → SE 爆发"一体化机制（自原 Eternity/OverTheHorizon/BlackHoleAttack
 * 三处合并至此）。
 * <p>叠层：任意 SA（特殊斩，非 Fail 型）命中施放即对统一索敌目标（锁定→准星→最近，见
 * {@link BlackHoleUtil#selectTarget}）+1 层；<b>OverTheHorizon 翻倍（+2）</b>；上限
 * {@link BlackHoleUtil#MAX_STACKS} 层。层数在目标身上以黑洞粒子可视化，层满时动态化。</p>
 * <p>爆发（已激活）：命中时目标减半当前血量并消耗全部层数生成次元斩（行为同旧版）。
 * 未激活：<b>不再直接断裂</b>，改为每次命中额外消耗 1 点耐久；刀真正断裂时
 * （{@link SlashBladeEvent.BreakEvent}）补偿 <b>+1000 耀魂</b>，且 {@value #BREAK_REPAIR_PCT}% 概率
 * 直接回满耐久。</p>
 */
@Mod.EventBusSubscriber
public class Eternity extends NRSpecialEffect {
    public Eternity() {
        super(200, false, false);
    }

    static Random random = new Random();

    /** 断裂补偿耀魂数。 */
    private static final int BREAK_SOUL = 1000;
    /** 断裂时回满耐久的概率（百分比）。 */
    private static final int BREAK_REPAIR_PCT = 25;
    /** 黑洞可视化粒子广播间隔（tick）。 */
    private static final int VIZ_INTERVAL = 4;

    // ============================================================
    // SA 叠层：任意 SA 施放（非 Fail）即叠层，OTA 翻倍
    // ============================================================
    @SubscribeEvent
    public static void onPerformSlashArt(SlashBladeEvent.PerformSlashArtEvent event) {
        // Fail 型（ rank 不足等情况）也会触发本事件，须按类型过滤
        if (event.getType() == SlashArts.ArtsType.Fail) {
            return;
        }
        ISlashBladeState state = event.getSlashBladeState();
        if (state == null || !state.hasSpecialEffect(NrSpecialEffectsRegistry.Eternity.getId())) {
            return;
        }
        LivingEntity user = event.getEntityLiving();
        Level level = user.level();
        if (level.isClientSide() || !(user instanceof Player)) {
            return;
        }
        // 统一索敌（本刀状态里的重锋锁定优先）
        LivingEntity target = BlackHoleUtil.selectTarget(user, state);
        if (target == null) {
            return;
        }
        boolean overTheHorizon = NrComboStateRegistry.OVER_THE_HORIZON.getId().equals(event.getComboState());
        BlackHoleUtil.addBlackHoleCount(target, overTheHorizon ? 2 : 1);
    }

    // ============================================================
    // 命中：已激活→半血+次元斩爆发；未激活→耐久消耗+1（替代旧的直接断裂）
    // ============================================================
    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        // 1. 快速判效：无Eternity特效直接返回
        if (!state.hasSpecialEffect(NrSpecialEffectsRegistry.Eternity.getId())) {
            return;
        }
        // 2. 校验攻击者是玩家（非玩家直接返回）
        Entity user = event.getUser();
        if (!(user instanceof Player player)) {
            return;
        }
        // 3. 未激活：不再 setBroken，改为额外 +1 耐久消耗（耐久耗尽走正常断裂、由 onBladeBreak 补偿）
        if (!NRSpecialEffect.isAquaActive(event.getBlade(), NrSpecialEffectsRegistry.Eternity.getId())) {
            event.getBlade().hurtAndBreak(1, player, e -> {
            });
            return;
        }
        // 4. 核心校验：先处理原始Entity（解决EnderDragonPart类型转换问题）
        Entity rawTarget = event.getTarget();
        if (rawTarget == null || rawTarget.level().isClientSide()) {
            return;
        }
        // 4.1 先获取主实体（处理末影龙部件）
        Entity mainTarget = BlackHoleUtil.mergeToMain(rawTarget);
        // 4.2 再校验是否为LivingEntity
        if (!(mainTarget instanceof LivingEntity target)) {
            return;
        }
        // 女仆/宠物保护：不对主人自己的驯服生物扣半血与释放次元斩
        if (MaidCompat.isOwnedBy(target, player)) {
            return;
        }
        // 5. 扣血逻辑（仅对LivingEntity生效，避免类型错误）
        float currentHp = target.getHealth();
        target.setHealth(currentHp * 0.5f);
        // 6. 生成次元斩（传入玩家+主实体，核心修复点）
        spawnJudgementCut(player, target);
    }

    // ============================================================
    // 断裂补偿：+1000 耀魂，概率回满耐久
    // ============================================================
    @SubscribeEvent
    public static void onBladeBreak(SlashBladeEvent.BreakEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state == null || !state.hasSpecialEffect(NrSpecialEffectsRegistry.Eternity.getId())) {
            return;
        }
        state.setProudSoulCount(state.getProudSoulCount() + BREAK_SOUL);
        if (random.nextInt(100) < BREAK_REPAIR_PCT) {
            ItemStack blade = event.getBlade();
            blade.setDamageValue(0);
            state.setBroken(false);
        }
    }

    // ============================================================
    // 层数可视化：定期为观察名单中的叠层实体广播黑洞粒子（免客户端同步 NBT）
    // ============================================================
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        if (event.getServer().getTickCount() % VIZ_INTERVAL != 0) {
            return;
        }
        BlackHoleUtil.WATCHED.entrySet().removeIf(entry -> {
            Entity entity = entry.getValue();
            int count = BlackHoleUtil.getBlackHoleCount(entity);
            // 清理：死亡/被移除/层数归零/世界已卸载
            if (entity == null || !entity.isAlive() || entity.isRemoved() || count <= 0
                    || !(entity.level() instanceof ServerLevel sl)) {
                return true;
            }
            emitBlackHoleParticles(sl, entity, count);
            return false;
        });
    }

    /**
     * 单个叠层实体的黑洞粒子表现：
     * 中心核（始终 1 颗，随层数增大）＋ 环绕粒子（层数越多越多、越快）；
     * 层满时双环反向旋转并追加末影粒子涡流——即"黑洞动态化"。
     */
    private static void emitBlackHoleParticles(ServerLevel level, Entity entity, int count) {
        double cx = entity.getX();
        double cy = entity.getY() + entity.getBbHeight() * 0.5;
        double cz = entity.getZ();
        float ratio = Math.min(1.0F, count / (float) BlackHoleUtil.MAX_STACKS);

        // 中心核：尺寸随层数增长（velY 通道传尺寸）
        level.sendParticles(NrParticlesRegistry.BLACK_HOLE.get(),
                cx, cy, cz, 1, 0.0D, 0.18 + 0.30 * ratio, 0.0D, 0.0D);

        // 环绕带：层数-1 颗（1 层只有核；轨道半径随层数微增）
        double ringR = 0.45 + 0.10 * count;
        int orbiters = count - 1;
        for (int i = 0; i < orbiters; i++) {
            double ang = Math.PI * 2.0 * i / Math.max(1, orbiters) + level.random.nextDouble() * 0.3;
            level.sendParticles(NrParticlesRegistry.BLACK_HOLE.get(),
                    cx + ringR * Math.cos(ang), cy, cz + ringR * Math.sin(ang),
                    1, ringR * Math.cos(ang), 0.10 + 0.06 * ratio, ringR * Math.sin(ang), 0.0D);
        }

        // 层满：双层反向快环 + 紫色涡流余韵
        if (count >= BlackHoleUtil.MAX_STACKS) {
            double fastR = 0.95;
            for (int i = 0; i < 6; i++) {
                double ang = Math.PI * 2.0 * i / 6.0;
                // 正环
                level.sendParticles(NrParticlesRegistry.BLACK_HOLE.get(),
                        cx + fastR * Math.cos(ang), cy + 0.25, cz + fastR * Math.sin(ang),
                        1, fastR * Math.cos(ang), 0.22, fastR * Math.sin(ang), 0.0D);
                // 倒环（另一半平面，视觉上反向）
                level.sendParticles(NrParticlesRegistry.BLACK_HOLE.get(),
                        cx - fastR * Math.cos(ang), cy - 0.25, cz - fastR * Math.sin(ang),
                        1, -fastR * Math.cos(ang), 0.22, -fastR * Math.sin(ang), 0.0D);
            }
            level.sendParticles(ParticleTypes.PORTAL, cx, cy, cz, 6, 0.6, 0.6, 0.6, 0.0);
        }
    }

    // 生成次元斩（抽离方法，新增player参数，核心修复）
    private static void spawnJudgementCut(Player player, LivingEntity target) {
        // 严格校验：玩家和目标都必须存活、非空
        if (player == null || !player.isAlive() || target == null || !target.isAlive()) return;
        int blackHoleCount = BlackHoleUtil.getBlackHoleCount(target);
        if (blackHoleCount <= 0) return;
        // 创建次元斩实体（增加属性上限，避免数值异常）
        EntityNRJudgementCut judgementCut = new EntityNRJudgementCut(NrEntitiesRegistry.NRJudgementCut, target.level());
        judgementCut.setLifetime(Math.min(blackHoleCount * 20, 200)); // 生命周期上限10秒
        judgementCut.setDamage(Math.min(blackHoleCount * 5, 50));     // 伤害上限50
        judgementCut.setCycleHit(true);
        // 核心修复：射击者设置为释放技能的玩家，而非被攻击目标
        judgementCut.setShooter(player);
        judgementCut.setPos(target.position());
        // 缩放下限保证低层数可见（实体内另有 clamp 兜底）
        judgementCut.setRenderScale(0.5f + blackHoleCount * 0.1f);
        judgementCut.setColor(0x4B0082);
        // 安全校验：世界非客户端、实体已正确初始化再加入世界
        if (!target.level().isClientSide()) {
            target.level().addFreshEntity(judgementCut);
        }
        BlackHoleUtil.resetBlackHoleCount(target);
    }
}
