package net.xianyu.prinegorerouse.registry.item;


import com.google.common.eventbus.Subscribe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import static mods.flammpfeil.slashblade.SlashBlade.MODID;
import static net.xianyu.prinegorerouse.item.ModItems.ITEMS;


public class NrItem extends Item {

    public static Item chronos;
    public static Item erebus;
    public static Item protogenoi;
    public static Item fatestar;
    public static Item aritemis;
    public static Item hercules;
    public static Item chaos;
    public static Item tartarus;

    public NrItem(Item.Properties a){
        super(a);
    }

    @Subscribe
    public void register(RegisterEvent event) {

        chronos = new NrItem(new Properties());
        erebus = new NrItem(new Properties());
        protogenoi = new NrItem(new Properties());
        fatestar = new NrItem(new Properties());
        aritemis = new NrItem(new Properties());
        hercules = new NrItem(new Properties());
        chaos = new NrItem(new Properties());
        tartarus = new NrItem(new Properties());

        event.register(ForgeRegistries.Keys.ITEMS,
                helper -> {
                    helper.register(new ResourceLocation(MODID,"chronos"), chronos);
                    helper.register(new ResourceLocation(MODID,"erebus"), erebus);
                    helper.register(new ResourceLocation(MODID,"protogenoi"), protogenoi);
                    helper.register(new ResourceLocation(MODID,"fatestar"), fatestar);
                    helper.register(new ResourceLocation(MODID,"aritemis"), aritemis);
                    helper.register(new ResourceLocation(MODID,"hercules"), hercules);
                    helper.register(new ResourceLocation(MODID,"chaos"), chaos);
                    helper.register(new ResourceLocation(MODID,"tartarus"),tartarus);
                });
    }
}