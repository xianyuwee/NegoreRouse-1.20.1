package net.xianyu.prinegorerouse.client.renderer;

import mods.flammpfeil.slashblade.client.renderer.entity.DriveRenderer;
import mods.flammpfeil.slashblade.client.renderer.entity.SummonedSwordRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.client.renderer.entity.Drive_5yeRenderer;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

import static net.xianyu.prinegorerouse.prinegorerouse.MOD_ID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = MOD_ID)
public class NRClient {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NrEntitiesRegistry.BlisteringSword, SummonedSwordRenderer::new);
        event.registerEntityRenderer(NrEntitiesRegistry.Zenith12th_Sword, SummonedSwordRenderer::new);
        event.registerEntityRenderer(NrEntitiesRegistry.Storm_Sword, SummonedSwordRenderer::new);
        event.registerEntityRenderer(NrEntitiesRegistry.DriveEx, DriveRenderer::new);
        event.registerEntityRenderer(NrEntitiesRegistry.Drive5_ye, Drive_5yeRenderer::new);
    }
}
