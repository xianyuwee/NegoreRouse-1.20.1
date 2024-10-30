package net.xianyu.prinegorerouse.compat;


import mods.flammpfeil.slashblade.compat.playerAnim.PlayerAnimationOverrider;
import mods.flammpfeil.slashblade.compat.playerAnim.VmdAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.xianyu.prinegorerouse.registry.NrComboStateRegistry;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class PlayerAnimationRegisterEvent {
    private static final ResourceLocation MotionLocation = new ResourceLocation("slashblade", "model/pa/player_motion.vmd");

    public PlayerAnimationRegisterEvent() {
    }

    @SubscribeEvent
    public static void onRegisterPlayerAnim(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("playeranimator")) {
            PlayerAnimationOverrider.getInstance().getAnimation().put(NrComboStateRegistry.ZENITH12TH.getId(), new VmdAnimation(MotionLocation, 400.0, 488.0,false));
        }
    }
}
