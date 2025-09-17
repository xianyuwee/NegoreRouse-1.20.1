package net.xianyu.prinegorerouse;

import com.google.common.base.CaseFormat;
import com.mojang.logging.LogUtils;
import java.util.function.Supplier;
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
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.IConfigSpec;
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

@Mod("prinegorerouse")
public class prinegorerouse {
  public static final String MOD_ID = "prinegorerouse";
  
  public static final Logger LOGGER = LogUtils.getLogger();
  
  public static ResourceLocation prefix(String path) {
    return new ResourceLocation("prinegorerouse", path);
  }
  
  public prinegorerouse() {
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
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, (IConfigSpec)NRConfig.COMMON_CONFIG);
    WeaponSystem.initialize();
  }
  
  @SubscribeEvent
  public void onConfigLoad(ModConfigEvent.Loading event) {
    if (event.getConfig().getType() == ModConfig.Type.COMMON)
      applyDifficultySettings(); 
  }
  
  private void applyDifficultySettings() {
    int difficulty = ((Integer)NRConfig.DIFFICULTY.get()).intValue();
    WeaponSystem.setDifficulty(difficulty);
    LOGGER.info("= {}", Integer.valueOf(difficulty));
  }
  
  private void commonSetup(FMLCommonSetupEvent event) {}
  
  private void addCreative(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.COMBAT) {
      event.accept((Supplier)ModItems.CHRONOS);
      event.accept((Supplier)ModItems.EREBUS);
      event.accept((Supplier)ModItems.PROTOGENOI);
      event.accept((Supplier)ModItems.ARITEMIS);
      event.accept((Supplier)ModItems.CHAOS);
      event.accept((Supplier)ModItems.HERCULES);
      event.accept((Supplier)ModItems.FATESTAR);
      event.accept((Supplier)ModItems.TARTARUS);
    } 
  }
  
  public void register(RegisterEvent event) {
    NrEntitiesRegistry.register(event);
  }
  
  private void onCrafting(PlayerEvent.ItemCraftedEvent event) {
    Player player = event.getEntity();
    ItemStack result = event.getCrafting();
    if (BladeCraftEvent.isSpecialBlade(result))
      BladeCraftEvent.setBladeOwner(result, player); 
  }
  
  @EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
  public static class RegistryEvents {}
  
  private static String classToString(Class<? extends Entity> entityClass) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName())
      .replace("entity_", "");
  }
  
  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {}
  
  @EventBusSubscriber(modid = "prinegorerouse", bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {}
  }
}