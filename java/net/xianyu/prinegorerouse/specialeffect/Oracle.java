package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

@EventBusSubscriber
public class Oracle extends SpecialEffect {

    public Oracle() {
        super(1, true, true);
    }

    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())) {
            Player player = (Player) event.getEntity();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Oracle.get(), level)) {
                if (player.isUsingItem()) {
                    player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 4));
                }
            }
        }
    }
}
