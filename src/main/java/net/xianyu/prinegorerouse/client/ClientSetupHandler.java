package net.xianyu.prinegorerouse.client;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModel;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.xianyu.prinegorerouse.item.ModItems;

import static mods.flammpfeil.slashblade.client.ClientHandler.bakeBlade;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class ClientSetupHandler {
    @SubscribeEvent
    public static void setModelUser(final FMLClientSetupEvent event) {
        // FMLClientSetupEvent 会在多个 mod 之间并行执行，而 ItemProperties.register 内部会修改
        // 共享的静态 Map。并行执行时会触发 ConcurrentModificationException，因此把注册排队到主线程执行。
        event.enqueueWork(() -> ModItems.ITEMS.getEntries().forEach(blade -> {
            if (blade.get() instanceof ItemSlashBlade) {
                ItemProperties.register(blade.get(), SlashBlade.prefix("user"), (stack, level, entity, seed) -> {
                    BladeModel.user = entity;
                    return 0;
                });
            }
        }));
    }

    @SubscribeEvent
    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(InputHandler.KEY_CHARGE);
        event.register(InputHandler.KEY_EDIT_HUD);
        event.register(InputHandler.KEY_SE_MANAGE);
    }


    @SubscribeEvent()
    public static void baked(final ModelEvent.ModifyBakingResult event) {
        ModItems.ITEMS.getEntries().forEach(blade -> {
            if (blade.get() instanceof ItemSlashBlade) {
                bakeBlade(blade.get(), event);
            }
        });
    }
}
