package net.xianyu.prinegorerouse.client;

import mods.flammpfeil.slashblade.client.renderer.entity.DriveRenderer;
import mods.flammpfeil.slashblade.client.renderer.entity.SummonedSwordRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.client.renderer.entity.NRDriveRenderer;
import net.xianyu.prinegorerouse.client.renderer.entity.NRJudgementCutRenderer;
import net.xianyu.prinegorerouse.client.renderer.entity.AbsolutePhantomRenderer;
import net.xianyu.prinegorerouse.client.renderer.entity.FateEyeRenderer;
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
            event.registerEntityRenderer(NrEntitiesRegistry.Storm_Sword, SummonedSwordRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.Enchanted_Sword, SummonedSwordRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.DriveEx, NRDriveRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.FireDrive, DriveRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.ShinyDrive, DriveRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.NRDrive, NRDriveRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.NRSDrive, DriveRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.LineDrive, NRDriveRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.NRBlisteringSword, SummonedSwordRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.Countable_Sword, SummonedSwordRenderer::new);
             event.registerEntityRenderer(NrEntitiesRegistry.Reverse_Sword, SummonedSwordRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.NRJudgementCut, NRJudgementCutRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.Absolute_Phantom, AbsolutePhantomRenderer::new);
            event.registerEntityRenderer(NrEntitiesRegistry.Fate_Eye, FateEyeRenderer::new);
        }
    }

