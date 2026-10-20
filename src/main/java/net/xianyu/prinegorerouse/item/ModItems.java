package net.xianyu.prinegorerouse.item;

import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;
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

    public static final RegistryObject<ItemFEBlade> ARITEMIS_S_BLADE = ITEMS.register("aritemiss_blade",
            () -> new ItemFEBlade(new ItemTierSlashBlade(40, 36.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> AEON_BLADE = ITEMS.register("aeon_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(40, 300.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> ANANKE_BLADE = ITEMS.register("ananke_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(100, 46.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> ANTAUGE_BLADE = ITEMS.register("antauge_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 45.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> ARITEMIS_BLADE = ITEMS.register("aritemis_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 22.0F), 4, -2.4F, new Item.Properties()));

    // 卡俄斯使用 FE 子类：吞噬清耀（Clear）后为当前刀提供 FE 条（显示层按携带 Clear 特效门控）
    public static final RegistryObject<ItemFEChaosBlade> CHAOS_BLADE = ITEMS.register("chaos_blade",
            () -> new ItemFEChaosBlade(new ItemTierSlashBlade(50, 51.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> CHRONOS_BLADE = ITEMS.register("chronos_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 25.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> CHRONOSN_BLADE = ITEMS.register("chronosn_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 35.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> CHRONOSSY_BLADE = ITEMS.register("chronossy_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(2, 0.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> DELIGUN_BLADE = ITEMS.register("deligun_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 37.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> EREBUS_BLADE = ITEMS.register("erebus_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 35.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> HERCULES_BLADE = ITEMS.register("hercules_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 27.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> NIER_BLADE = ITEMS.register("nier_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 19.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> NYX_BLADE = ITEMS.register("nyx_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 25.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> PROTOGENOI_BLADE = ITEMS.register("protogenoi_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(100, 60.0F), 4, -2.4F, new Item.Properties()));

    public static final RegistryObject<ItemNRBlade> TARTARUS_BLADE = ITEMS.register("tartarus_blade",
            () -> new ItemNRBlade(new ItemTierSlashBlade(50, 35.0F), 4, -2.4F, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
