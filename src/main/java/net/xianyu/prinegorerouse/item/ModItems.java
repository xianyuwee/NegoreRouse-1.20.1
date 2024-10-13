package net.xianyu.prinegorerouse.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xianyu.prinegorerouse.prinegorerouse;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, prinegorerouse.MOD_ID);

    public static final RegistryObject<Item> CHRONOS = ITEMS.register("chronos",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> EREBUS = ITEMS.register("erebus",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PROTOGENOI = ITEMS.register("protogenoi",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FATESTAR = ITEMS.register("fatestar",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CHAOS = ITEMS.register("chaos",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HERCULES = ITEMS.register("hercules",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ARITEMIS = ITEMS.register("aritemis",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TARTARUS = ITEMS.register("tartarus",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
