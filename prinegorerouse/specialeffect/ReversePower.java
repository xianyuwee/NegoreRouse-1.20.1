package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

@EventBusSubscriber
public class ReversePower extends SpecialEffect {

    public ReversePower() {
        super(35, true, true);
    }

    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if(state.hasSpecialEffect(NrSpecialEffectsRegistry.ReversePower.getId())) {
            if(!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            int level = player.experienceLevel;
            if(SpecialEffect.isEffective((SpecialEffect)NrSpecialEffectsRegistry.ReversePower.get(),level)) {
                if(player.isUsingItem()){
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 3));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.UNLUCK,40,5));
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,40,1));
                }
            }
        }
    }
}
