package net.xianyu.prinegorerouse.client;

import mods.flammpfeil.slashblade.client.renderer.entity.DriveRenderer;
import mods.flammpfeil.slashblade.client.renderer.entity.SummonedSwordRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

    @Mod.EventBusSubscriber(
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = {Dist.CLIENT},
            modid = "prinegorerouse"
    )
    public class NRClient {
        public NRClient() {
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(NrEntitiesRegistry.BlisteringSword, SummonedSwordRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.Zenith12th_Sword, SummonedSwordRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.Storm_Sword, SummonedSwordRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.DriveEx, DriveRenderer::new);

        }
    }

