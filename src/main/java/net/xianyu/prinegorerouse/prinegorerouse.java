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


        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NRConfig.COMMON_CONFIG);
        // 监听配置事件
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

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

    // 修正后的配方热刷新方法
    private void refreshBladeRecipes() {
        // 1. 获取服务端实例
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("配方刷新失败：未获取到服务端实例");
            return;
        }
        RecipeManager recipeManager = server.getRecipeManager();

        // 2. 过滤并移除当前MOD的旧配方
        List<Recipe<?>> allRecipes = new ArrayList<>(recipeManager.getRecipes());
        List<Recipe<?>> modOldRecipes = allRecipes.stream()
                .filter(recipe -> recipe.getId().getNamespace().equals(MOD_ID))
                .collect(Collectors.toList());

        // 构造移除旧配方后的新配方列表
        List<Recipe<?>> remainingRecipes = allRecipes.stream()
                .filter(recipe -> !recipe.getId().getNamespace().equals(MOD_ID))
                .collect(Collectors.toList());

        // 3. 生成新配方并添加到列表
        List<Recipe<?>> newBladeRecipes = new ArrayList<>();
        NRBladeRuntimeRecipeRegistry.reloadBladeRecipes((finishedRecipe) -> {
            // 将FinishedRecipe转换为Recipe实例
            Recipe<?> recipe = RecipeManager.fromJson(finishedRecipe.getId(), finishedRecipe.serializeRecipe());
            if (recipe != null) {
                newBladeRecipes.add(recipe);
            }
        });

        // 4. 合并剩余配方 + 新配方，批量替换（服务端主线程执行）
        server.execute(() -> {
            remainingRecipes.addAll(newBladeRecipes);
            recipeManager.replaceRecipes(remainingRecipes);

            LOGGER.info("已清除{}个旧配方，重新注册{}个新配方（难度：{}）",
                    modOldRecipes.size(), newBladeRecipes.size(), WeaponSystem.getDifficulty());

            // 5. 通知所有在线客户端同步配方（替代不存在的 broadcastRecipeUpdates）
            // 核心：遍历所有在线玩家，发送配方更新数据包
            List<Recipe<?>> allNewRecipes = new ArrayList<>(recipeManager.getRecipes());
            ClientboundUpdateRecipesPacket updatePacket = new ClientboundUpdateRecipesPacket(allNewRecipes);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(updatePacket);
            }
        });
    }

    // 修改applyDifficultySettings方法，新增配方刷新触发
    private void applyDifficultySettings() {
        int difficulty = NRConfig.DIFFICULTY.get();
        WeaponSystem.setDifficulty(difficulty);
        LOGGER.info("武器系统配置已更新: 难度 = {}", difficulty);

        // 新增：配置更新时触发配方热刷新
        refreshBladeRecipes();
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

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {}

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {}
    }
}