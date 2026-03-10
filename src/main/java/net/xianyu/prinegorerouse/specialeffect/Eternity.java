package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.entity.EntityNRJudgementCut;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.utils.BlackHoleUtil;

@Mod.EventBusSubscriber
public class Eternity extends SpecialEffect {
    public Eternity() {
        super(200, false, false);
    }

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
        // 3. 校验特效等级有效性
        int level = player.experienceLevel;
        if (!SpecialEffect.isEffective(NrSpecialEffectsRegistry.Eternity.get(), level)) {
            state.setBroken(true);
            return;
        }
        // 4. 核心校验：先处理原始Entity（解决EnderDragonPart类型转换问题）
        Entity rawTarget = event.getTarget();
        if (rawTarget == null || rawTarget.level().isClientSide()) {
            return;
        }
        // 4.1 先获取主实体（处理末影龙部件）
        Entity mainTarget = getMainEntity(rawTarget);
        // 4.2 再校验是否为LivingEntity
        if (!(mainTarget instanceof LivingEntity target)) {
            return;
        }
        // 5. 扣血逻辑（仅对LivingEntity生效，避免类型错误）
        float currentHp = target.getHealth();
        target.setHealth(currentHp * 0.5f);
        // 6. 生成次元斩（传入玩家+主实体，核心修复点）
        spawnJudgementCut(player, target);
    }

    // 新增：通用多部分实体处理（提取为独立方法）
    private static Entity getMainEntity(Entity entity) {
        if (entity instanceof EnderDragonPart part) {
            return part.getParent(); // 返回末影龙主实体
        }
        return entity;
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
        judgementCut.setRenderScale(Math.min(blackHoleCount * 0.1f, 1.0f)); // 缩放上限1.0
        judgementCut.setColor(0x4B0082);
        // 安全校验：世界非客户端、实体已正确初始化再加入世界
        if (!target.level().isClientSide()) {
            target.level().addFreshEntity(judgementCut);
        }
        BlackHoleUtil.resetBlackHoleCount(target);
    }
}