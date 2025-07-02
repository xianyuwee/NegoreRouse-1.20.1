package net.xianyu.prinegorerouse;

import com.google.common.base.CaseFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.data.WeaponSystem;
import net.xianyu.prinegorerouse.event.BladeCraftEvent;
import net.xianyu.prinegorerouse.item.ModItems;
import net.xianyu.prinegorerouse.item.PriNRModTabs;
import net.xianyu.prinegorerouse.registry.NrComboStateRegistry;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSlashArtRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(prinegorerouse.MOD_ID)
public class prinegorerouse {

    public static final String MOD_ID = "prinegorerouse";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MOD_ID,path);
    }

    public prinegorerouse()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        PriNRModTabs.register(modEventBus);

        ModItems.register(modEventBus);

        modEventBus.addListener(this::register);

        modEventBus.addListener(this::commonSetup);

        forgeBus.addListener(this::onCrafting);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        NrSpecialEffectsRegistry.SPECIAL_EFFECT.register(modEventBus);

        NrSlashArtRegistry.NR_SLASH_ARTS.register(modEventBus);

        NrComboStateRegistry.NR_COMBO_STATE.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NRConfig.COMMON_CONFIG);

        WeaponSystem.initialize();
    }

    @SubscribeEvent
    public void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            // 配置加载完成后执行
            applyDifficultySettings();
        }
    }

    private void applyDifficultySettings() {
        int difficulty = NRConfig.DIFFICULTY.get();
        // 在这里应用难度设置到游戏系统
        // 例如：更新配方要求、调整游戏参数等
        WeaponSystem.setDifficulty(difficulty);
        // 日志记录配置更新
        LOGGER.info("武器系统配置已更新: 难度 = {}", difficulty);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {}

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.CHRONOS);
            event.accept(ModItems.EREBUS);
            event.accept(ModItems.PROTOGENOI);
            event.accept(ModItems.ARITEMIS);
            event.accept(ModItems.CHAOS);
            event.accept(ModItems.HERCULES);
            event.accept(ModItems.FATESTAR);
            event.accept(ModItems.TARTARUS);
        }
    }

    public void register(RegisterEvent event) {
        NrEntitiesRegistry.register(event);
    }

    private void onCrafting(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        ItemStack result = event.getCrafting();

        if (BladeCraftEvent.isSpecialBlade(result)) {
            BladeCraftEvent.setBladeOwner(result, player);
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {

    }

    private static String classToString(Class<? extends Entity> entityClass) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName())
                .replace("entity_", "");
    }



    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {}

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {}

    }

}
