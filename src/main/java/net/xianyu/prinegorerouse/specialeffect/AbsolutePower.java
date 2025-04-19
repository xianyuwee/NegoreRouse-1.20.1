package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.data.builtin.SlashBladeBuiltInRegistry;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

import java.util.Random;

@EventBusSubscriber
public class AbsolutePower extends SpecialEffect {

    public AbsolutePower() {
        super(50, true, true);
    }

    public static Random random = new Random();

    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.AbsolutePower.getId())) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected())) {
                return;
            }
            float decimal = random.nextFloat();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.AbsolutePower.get(), level)) {
                if (player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 5));
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 3));
                } else if (player.isUsingItem() && player.getOffhandItem().getHoverName().equals(event.getBlade().getHoverName()) && NRConfig.OFFHAND_CAN_ACTIVE.get().equals(true)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20,3));
                    if (decimal - 0.3F <= 0.00001F && player.isAlive()) {
                        player.heal(player.getMaxHealth() * 0.1F);
                    }
                }
            }
        }
    }
}
