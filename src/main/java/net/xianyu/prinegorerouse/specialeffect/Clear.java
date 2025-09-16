package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

import java.util.Random;


@Mod.EventBusSubscriber
public class Clear extends SpecialEffect {
    public Clear() {
        super(1, false, false);
    }

    static Random random = new Random();

    @SubscribeEvent
    public static void OnSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Clear.getId())) {
            // 已存在Player类型判断，无需修改
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected())) {
                return;
            }
            int level = player.experienceLevel;
            if (!SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Clear.get(), level)) {
                if(player.isUsingItem()) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20, 2));
                }
            } else {
                if(player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                }
            }
        }
    }


    @SubscribeEvent
    public static void OnSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Clear.getId())) {
            // 修复核心：先判断event.getUser()是否为Player，非Player直接返回，避免强制转换异常
            if (!(event.getUser() instanceof Player))
                return;
            
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Clear.get(), level)) {
                // 复用已验证类型的player对象，替代重复的event.getUser()调用
                Level level1 = player.level();
                double decimal = random.nextDouble();
                if (level1.isDay()) {
                    int blade_damage = state.getDamage();
                    state.setDamage(blade_damage + 1);
                    if (decimal - 0.2d <= 0.000001f) {
                        // 直接使用player对象，无需再次强制转换
                        player.giveExperiencePoints(200);
                    }
                }
                else {
                    if (decimal - 0.5d <= 0.00001f) {
                        int blade_damage = state.getDamage();
                        state.setDamage(blade_damage - 2);
                        Entity entity = event.getTarget();
                        // 复用player对象获取level，保持类型安全
                        Level explosionLevel = player.level();
                        Explosion explosion1 = explosionLevel.explode(
                                null, 
                                entity.getEyePosition().x,
                                entity.getEyePosition().y, 
                                entity.getEyePosition().z, 
                                0.1f, 
                                true, 
                                Level.ExplosionInteraction.NONE
                        );
                        explosion1.explode();
                    }
                }
            }
        }
    }
}
