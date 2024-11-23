package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.slasharts.Drive;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

@Mod.EventBusSubscriber
public class BurstDriveUpdate extends SpecialEffect {
    public BurstDriveUpdate() {
        super(30,false,true);
    }

    @SubscribeEvent
    public static void onDoingSlash(SlashBladeEvent.DoSlashEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.BurstDriveUpdate.getId())) {
            if (!(event.getUser() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect)NrSpecialEffectsRegistry.BurstDriveUpdate.get(), level)) {
                Drive.doSlash(player, event.getRoll(), 10, Vec3.ZERO, false, event.getDamage(), 1.5F);
            }
        }
    }
}
