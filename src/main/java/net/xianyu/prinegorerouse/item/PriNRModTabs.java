package net.xianyu.prinegorerouse.item;

import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.Map;
import java.util.logging.Logger;

import static com.mojang.text2speech.Narrator.LOGGER;

public class PriNRModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, prinegorerouse.MOD_ID);
    private static void fillBlades(CreativeModeTab.Output output) {
        if (Minecraft.getInstance().getConnection() != null ){
            BladeModelManager.getClientSlashBladeRegistry()
                    .entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .filter(entry -> {
                        ResourceLocation location = ResourceLocation.tryParse(entry.getKey().location().toString());
                        return location != null && location.getNamespace().equals(prinegorerouse.MOD_ID);
                    })
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        LOGGER.info("Registering Slashblade: {}", entry.getKey());
                        output.accept(entry.getValue().getBlade());
                    });
        }
    }
    public static final RegistryObject<CreativeModeTab> PRINEGOREROUSE_TAB = CREATIVE_MODE_TABS.register("prinegorerouse",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.FATESTAR.get()))
                    .title(Component.translatable("creativetab.prinegorerouse"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.CHRONOS.get());
                        output.accept(ModItems.EREBUS.get());
                        output.accept(ModItems.PROTOGENOI.get());
                        output.accept(ModItems.ARITEMIS.get());
                        output.accept(ModItems.CHAOS.get());
                        output.accept(ModItems.HERCULES.get());
                        output.accept(ModItems.FATESTAR.get());
                        output.accept(ModItems.TARTARUS.get());
                        fillBlades(output);
                    })

                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
