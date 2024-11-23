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
        super(1, false, true);
    }

    static Random random = new Random();

    @SubscribeEvent
    public static void OnSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Clear.getId())) {
            Player player = (Player) event.getEntity();
            int level = player.experienceLevel;
            if (!SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Clear.get(), level)) {
                if(player.isUsingItem()) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20, 2));
                }
            } else {
                if(player.isUsingItem()) {
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
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Clear.get(), level)) {
                Level level1 = event.getUser().level();
                double decimal = random.nextDouble();
                if (level1.isDay()) {
                    int blade_damage = state.getDamage();
                    state.setDamage(blade_damage + 1);
                    if (decimal-0.2d <= 0.000001f) {
                        ((Player) event.getUser()).giveExperiencePoints(200);
                    }
                }
                else {
                    if (decimal-0.5d <= 0.00001f) {
                        int blade_damage = state.getDamage();
                        state.setDamage(blade_damage - 2);
                        Entity entity = event.getTarget();
                        Level explosion = event.getUser().level();
                        Explosion explosion1 = explosion.explode(null, entity.getEyePosition().x,
                                entity.getEyePosition().y, entity.getEyePosition().z, 0.1f, true, Level.ExplosionInteraction.NONE);
                        explosion1.explode();
                    }
                }
            }
        }
    }
}
