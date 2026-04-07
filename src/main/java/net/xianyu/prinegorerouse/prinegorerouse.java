package net.xianyu.prinegorerouse;

import com.google.common.base.CaseFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.data.NRBladeRuntimeRecipeRegistry;
import net.xianyu.prinegorerouse.data.WeaponSystem;
import net.xianyu.prinegorerouse.event.BladeCraftEvent;
import net.xianyu.prinegorerouse.item.ModItems;
import net.xianyu.prinegorerouse.item.PriNRModTabs;
import net.xianyu.prinegorerouse.registry.NrComboStateRegistry;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSlashArtRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.event.NRCommands;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        modEventBus.addListener(NRBladeRuntimeRecipeRegistry::onCommonSetup);

        MinecraftForge.EVENT_BUS.addListener(this::onWorldLoad);


        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NRConfig.COMMON_CONFIG);
        // 监听配置事件
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        // 注册配方系统
        modEventBus.register(NRBladeRuntimeRecipeRegistry.class);

        // 初始化武器系统（加载基础值）
        WeaponSystem.initialize();

    }

    // 配置加载时更新难度
    @SubscribeEvent
    public void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            applyDifficultySettings();
        }
    }

    // 配置重新加载时更新难度
    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            applyDifficultySettings();
        }
    }

    // 修改applyDifficultySettings方法，新增配方刷新触发
    private void applyDifficultySettings() {
        int difficulty = NRConfig.DIFFICULTY.get();
        WeaponSystem.setDifficulty(difficulty);
        LOGGER.info("武器系统配置已更新: 难度 = {}", difficulty);
    }

    // 新增方法
    private void onWorldLoad(net.minecraftforge.event.level.LevelEvent.Load event) {
        // 只在服务端世界加载时执行
        if (event.getLevel().isClientSide()) return;

        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            RecipeManager recipeManager = serverLevel.getServer().getRecipeManager();

            // 检查是否已经注入过（避免每个维度加载都重复注入）
            // 这里我们用一个简单的静态标记
            if (!net.xianyu.prinegorerouse.data.NRBladeRuntimeRecipeRegistry.hasInjectedOnce) {
                net.xianyu.prinegorerouse.data.NRBladeRuntimeRecipeRegistry.hasInjectedOnce = true;
                serverLevel.getServer().execute(() -> {
                    net.xianyu.prinegorerouse.data.NRBladeRuntimeRecipeRegistry.refreshRecipes(recipeManager);
                });
            }
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {}

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
    public static class RegistryEvents {}

    private static String classToString(Class<? extends Entity> entityClass) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName())
                .replace("entity_", "");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {}
    }
}