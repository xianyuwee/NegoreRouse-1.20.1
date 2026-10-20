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
                        ItemStack blade = entry.getValue().getBlade();
                        // getBlade() 可能因为该刀定义的物理物品无法解析（注册 id 与定义 name 不一致）而返回空栈。
                        // Forge 的创造栏校验要求 stack.getCount() 必须为 1，而 ItemStack#setCount 对空栈是空操作
                        // （内部 if(!isEmpty()) 会跳过），因此必须先跳过空栈，否则会抛
                        // "The stack count must be 1" 导致打开创造栏崩溃。
                        if (blade.isEmpty()) {
                            LOGGER.warn("Skip slashblade creative entry (empty blade): {}", entry.getKey());
                            return;
                        }
                        LOGGER.info("Registering Slashblade: {}", entry.getKey());
                        // 拔刀剑的刀可堆叠时 getBlade() 可能带回大于 1 的数量，这里强制置 1 以通过 Forge 校验。
                        blade.setCount(1);
                        output.accept(blade);
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
