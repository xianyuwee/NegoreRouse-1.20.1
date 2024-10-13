package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;


@Mod.EventBusSubscriber
public class Porgatory extends SpecialEffect {
    public Porgatory() {
        super(1,true,true);
    }

    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if(state.hasSpecialEffect(NrSpecialEffectsRegistry.Porgatory.getId())) {
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if(SpecialEffect.isEffective(NrSpecialEffectsRegistry.Porgatory.get(),level)) {
                event.getTarget().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,20));
            }
        }
    }
}
