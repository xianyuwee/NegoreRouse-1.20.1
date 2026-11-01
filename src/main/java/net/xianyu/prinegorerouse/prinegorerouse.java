package net.xianyu.prinegorerouse;

import com.google.common.base.CaseFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.xianyu.prinegorerouse.entity.EntityStormSword;
import net.xianyu.prinegorerouse.specialeffect.ChaosBladeHandler;
import net.xianyu.prinegorerouse.specialeffect.ChaosHandler;
import net.xianyu.prinegorerouse.specialeffect.NRSEs.Empty;
import net.xianyu.prinegorerouse.specialeffect.NRSEs.Fate;
import net.xianyu.prinegorerouse.specialeffect.NRSEs.Porgatory;
import net.xianyu.prinegorerouse.utils.BlackHoleUtil;
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
import net.minecraftforge.registries.RegistryObject;
import net.xianyu.prinegorerouse.config.NRClientConfig;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.data.DifficultySavedData;
import net.xianyu.prinegorerouse.data.NRBladeRuntimeRecipeRegistry;
import net.xianyu.prinegorerouse.data.WeaponSystem;
import net.xianyu.prinegorerouse.event.BladeCraftEvent;
import net.xianyu.prinegorerouse.item.ItemFEBlade;
import net.xianyu.prinegorerouse.item.ModItems;
import net.xianyu.prinegorerouse.item.PriNRModTabs;
import net.xianyu.prinegorerouse.registry.NrComboStateRegistry;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrParticlesRegistry;
import net.xianyu.prinegorerouse.registry.NrSlashArtRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import org.slf4j.Logger;

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
        NrParticlesRegistry.PARTICLES.register(modEventBus);
        NrSlashArtRegistry.NR_SLASH_ARTS.register(modEventBus);
        NrComboStateRegistry.NR_COMBO_STATE.register(modEventBus);

        modEventBus.addListener(NRBladeRuntimeRecipeRegistry::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
//      ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());

        MinecraftForge.EVENT_BUS.addListener(this::onWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onDatapackSync);

        // 注册配置（其余通用开关仍走 toml；难度已改为世界存档数据，不再依赖配置文件）
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NRConfig.COMMON_CONFIG);
        // 时间素 HUD 位置为本地视觉偏好且需游戏内拖拽写入，放客户端配置
        NRClientConfig.register();

        // 注册配方系统
        modEventBus.register(NRBladeRuntimeRecipeRegistry.class);

        // 初始化武器系统（加载基础值）
        WeaponSystem.initialize();

    }

    // 服务端世界加载时：从世界存档读取难度（新存档默认 5）并注入一次动态配方。
    // 难度不再依赖 toml 配置，改由 DifficultySavedData 随存档持久化。
    private void onWorldLoad(net.minecraftforge.event.level.LevelEvent.Load event) {
        // 只在服务端世界加载时执行
        if (event.getLevel().isClientSide()) return;

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // 检查是否已经注入过（避免每个维度加载都重复注入）
            if (NRBladeRuntimeRecipeRegistry.hasInjectedOnce) return;
            NRBladeRuntimeRecipeRegistry.hasInjectedOnce = true;

            MinecraftServer server = serverLevel.getServer();
            ServerLevel overworld = server.overworld();
            ServerLevel targetLevel = overworld != null ? overworld : serverLevel;

            RecipeManager recipeManager = server.getRecipeManager();
            // 读取本存档记忆的难度并应用到运行时（首次进世界 / 新建世界时为默认 5）
            int difficulty = DifficultySavedData.get(targetLevel).getDifficulty();
            WeaponSystem.setDifficulty(difficulty);

            server.execute(() ->
                    NRBladeRuntimeRecipeRegistry.refreshRecipes(recipeManager));
        }
    }

    // 服务端停止/退出世界：重置配方注入状态、难度与全局静态缓存，防内存泄漏与单机切世界配方/难度失效
    private void onServerStopped(ServerStoppedEvent event) {
        NRBladeRuntimeRecipeRegistry.resetInjection();
        WeaponSystem.reset();
        ChaosHandler.clearDefinitionsCache();
        ChaosHandler.clearThreadLocals();
        ChaosBladeHandler.clear();
        Empty.clear();
        Porgatory.clear();
        BlackHoleUtil.clear();
        Fate.clear();
        EntityStormSword.clear();
    }

    // 数据包同步/重载事件：执行 /reload 时重置定义缓存、重新应用难度并热重载配方与同步客户端
    private void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null && event.getPlayerList() != null && event.getPlayerList().getServer() != null) {
            ChaosHandler.clearDefinitionsCache();
            NRBladeRuntimeRecipeRegistry.resetInjection();
            MinecraftServer server = event.getPlayerList().getServer();
            ServerLevel overworld = server.overworld();
            if (overworld != null) {
                int difficulty = DifficultySavedData.get(overworld).getDifficulty();
                WeaponSystem.setDifficulty(difficulty);
            }
            RecipeManager recipeManager = server.getRecipeManager();
            server.execute(() -> {
                NRBladeRuntimeRecipeRegistry.refreshRecipes(recipeManager);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    NetworkPacketHandler.sendToClientPlayer(new BladeRecipeReloadPacket(), player);
                }
            });
        }
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

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkPacketHandler::registerMessage);
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
        // 捕获客户端配置实例，供 HUD 位置拖拽后落盘。
        @SubscribeEvent
        public static void onConfigLoad(ModConfigEvent.Loading event) {
            NRClientConfig.capture(event.getConfig());
        }
    }

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