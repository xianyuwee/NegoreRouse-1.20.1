package net.xianyu.prinegorerouse.client.back;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.prinegorerouse;

/**
 * 把 {@link StarAnchorStore#reload} 挂到客户端资源加载流程：首次进客户端世界、F3+T、切资源包都会触发。
 * <p>真正的重扫由 reload 内部的版本门控决定——mod 版本没变时只读缓存文件、不重扫 OBJ。</p>
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StarAnchorReloader {

    private StarAnchorReloader() {
    }

    @SubscribeEvent
    public static void onRegisterListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                StarAnchorStore.reload(resourceManager);
            }

            @Override
            public String getName() {
                return prinegorerouse.MOD_ID + ":back_star_anchor";
            }
        });
    }
}
