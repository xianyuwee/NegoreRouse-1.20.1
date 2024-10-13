package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;


@Mod.EventBusSubscriber
public class Phantom extends SpecialEffect {
    public Phantom() {
        super(1, false, false);
    }


    @SubscribeEvent
    public static void onSlashBladeHit (SlashBladeEvent.HitEvent event){
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Phantom.getId())) {
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Phantom.get(), level)) {
                float maxHealth = event.getTarget().getMaxHealth();
                float health = event.getTarget().getHealth();
                float heal = maxHealth - health;
                event.getUser().heal(heal);
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 1));
            }
        }
    }


    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Phantom.getId())) {
            Player player = (Player) event.getEntity();
            Entity entity = event.getEntity();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective(NrSpecialEffectsRegistry.Phantom.get(), level)) {
                if (event.getLevel() instanceof ServerLevel) {
                    ServerLevel serverLevel1 = (ServerLevel) event.getLevel();
                    ServerLevel serverLevel2 = (ServerLevel) event.getLevel();
                    if (event.getSlashBladeState().hasChangedActiveState()) {
                        if (player.isUsingItem()) {
                            if (!(entity.hurtMarked)) {
                                if (serverLevel1.isNight()) {
                                    serverLevel2.setDayTime(1000);
                                } else if (serverLevel1.isDay()) {
                                    serverLevel2.setDayTime(13000);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
