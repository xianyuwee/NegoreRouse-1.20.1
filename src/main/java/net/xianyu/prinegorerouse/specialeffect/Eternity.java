package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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
        super(200,false,false);
    }


    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if(state.hasSpecialEffect(NrSpecialEffectsRegistry.Eternity.getId())) {
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if(!SpecialEffect.isEffective(NrSpecialEffectsRegistry.Eternity.get(),level)) {
                event.getSlashBladeState().setBroken(true);
            }
            else {
                float hp = event.getTarget().getHealth();
                event.getTarget().setHealth(0.5f * hp);

                if (event.getTarget() instanceof LivingEntity) {
                    LivingEntity target = event.getTarget();
                    // 只在服务端执行
                    if (target.level() instanceof ServerLevel) {
                        spawnJudgementCut(target);
                    }
                }
            }
        }
    }

    // 新增方法：生成次元斩
    private static void spawnJudgementCut(LivingEntity target) {
        // 检查目标是否存活且有blackhole层数
        if (!target.isAlive()) return;

        int blackHoleCount = BlackHoleUtil.getBlackHoleCount(target);
        if (blackHoleCount <= 0) return;

        // 创建次元斩实体
        EntityNRJudgementCut judgementCut =
                new EntityNRJudgementCut(NrEntitiesRegistry.NRJudgementCut, target.level());

        // 设置次元斩属性
        judgementCut.setLifetime(blackHoleCount * 20);
        judgementCut.setDamage(blackHoleCount * 5);
        judgementCut.setCycleHit(true);
        judgementCut.setShooter(target);
        judgementCut.setPos(target.position());
        judgementCut.setRenderScale(blackHoleCount * 0.1f);
        judgementCut.setColor(0x4B0082);

        // 添加到世界
        target.level().addFreshEntity(judgementCut);

        // 重置blackhole层数（可选）
        BlackHoleUtil.resetBlackHoleCount(target);
    }
}