package net.xianyu.prinegorerouse.specialeffect;


import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

import javax.swing.text.JTextComponent;


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
                event.getUser().setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            else {
                float hp = event.getTarget().getHealth();
                event.getTarget().setHealth(0.5f * hp);
            }
        }
    }
}
