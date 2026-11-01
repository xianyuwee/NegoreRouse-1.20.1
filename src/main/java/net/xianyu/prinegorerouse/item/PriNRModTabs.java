package net.xianyu.prinegorerouse.item;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.xianyu.prinegorerouse.prinegorerouse;

public class PriNRModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, prinegorerouse.MOD_ID);

    private static void fillBlades(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        SlashBlade.getSlashBladeDefinitionRegistry(parameters.holders()).listElements()
                .filter(entry -> {
                    ResourceLocation location = entry.key().location();
                    return location != null && location.getNamespace().equals(prinegorerouse.MOD_ID);
                })
                .sorted(SlashBladeDefinition.COMPARATOR)
                .forEach(entry -> {
                    ItemStack blade = entry.value().getBlade();
                    if (blade.isEmpty()) {
                        prinegorerouse.LOGGER.warn("Skip slashblade creative entry (empty blade): {}", entry.key().location());
                        return;
                    }
                    blade.setCount(1);
                    output.accept(blade);
                });
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
                        fillBlades(itemDisplayParameters, output);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
