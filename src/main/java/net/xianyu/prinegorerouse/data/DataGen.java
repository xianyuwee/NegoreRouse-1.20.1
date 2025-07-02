package net.xianyu.prinegorerouse.data;

import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.data.builtin.NRBladeRecipeGen;
import net.xianyu.prinegorerouse.data.builtin.NrBladeBuiltInRegistry;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.Set;
import java.util.concurrent.CompletableFuture;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGen {

    @SubscribeEvent
    public static void dataGen(GatherDataEvent event) {
        DataGenerator dataGenerator = event.getGenerator();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        PackOutput packOutput = dataGenerator.getPackOutput();
        dataGenerator.addProvider(event.includeServer(), new NRBladeRecipeGen(packOutput));
        final RegistrySetBuilder NrBladeBuilder = new RegistrySetBuilder().add(SlashBladeDefinition.REGISTRY_KEY,
                NrBladeBuiltInRegistry::registerAll);
        dataGenerator.addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, NrBladeBuilder, Set.of(prinegorerouse.MOD_ID)));
    }
}
    