package net.xianyu.prinegorerouse.data;

import mods.flammpfeil.slashblade.data.builtin.SlashBladeBuiltInRegistry;
import mods.flammpfeil.slashblade.recipe.RequestDefinition;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipeBuilder;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.item.ModItems;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.data.builtin.NrBladeBuiltInRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

// 适配旧版本Forge：移除RecipeOutput/ItemLike依赖
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NRBladeRuntimeRecipeRegistry extends RecipeProvider implements IConditionBuilder { // 新增IConditionBuilder以使用条件构造方法

    public NRBladeRuntimeRecipeRegistry(PackOutput output) {
        super(output);
    }

//    static {
//        FMLJavaModLoadingContext.get().getModEventBus().addListener(NRBladeRuntimeRecipeRegistry::onCommonSetup);
//    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // 修复报错1：FMLEnvironment.getSide() → FMLEnvironment.side()（新版Forge API变更）
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            event.enqueueWork(() -> {
                // 1. 初始化武器系统 + 读取配置难度（核心：难度=基础值×配置系数）
                WeaponSystem.initialize();
                int configDifficulty = NRConfig.DIFFICULTY.get(); // 从配置读取正整数难度
                WeaponSystem.setDifficulty(configDifficulty);
            });
        }
    }

    // 新增：抽离配方注册核心逻辑为静态方法
    public static void reloadBladeRecipes(Consumer<FinishedRecipe> recipeConsumer) {
        // 1. 读取最新难度配置
        int configDifficulty = NRConfig.DIFFICULTY.get();
        WeaponSystem.setDifficulty(configDifficulty);

        // 2. 获取所有武器的实时合成要求（荣耀/杀敌/锻造数）
        WeaponSystem.WeaponRequirement aeon = WeaponSystem.getRequirementForWeapon("AEON_BLADE");
        WeaponSystem.WeaponRequirement ananke = WeaponSystem.getRequirementForWeapon("ANANKE_BLADE");
        WeaponSystem.WeaponRequirement antauge = WeaponSystem.getRequirementForWeapon("ANTAUGE_BLADE");
        WeaponSystem.WeaponRequirement aritemis = WeaponSystem.getRequirementForWeapon("ARITEMIS_BLADE");
        WeaponSystem.WeaponRequirement chaos = WeaponSystem.getRequirementForWeapon("CHAOS_BLADE");
        WeaponSystem.WeaponRequirement chronos = WeaponSystem.getRequirementForWeapon("CHRONOS_BLADE");
        WeaponSystem.WeaponRequirement chronosn = WeaponSystem.getRequirementForWeapon("CHRONOSN_BLADE");
        WeaponSystem.WeaponRequirement erebus = WeaponSystem.getRequirementForWeapon("EREBUS_BLADE");
        WeaponSystem.WeaponRequirement hercules = WeaponSystem.getRequirementForWeapon("HERCULES_BLADE");
        WeaponSystem.WeaponRequirement nier = WeaponSystem.getRequirementForWeapon("NIER_BLADE");
        WeaponSystem.WeaponRequirement protogenoi = WeaponSystem.getRequirementForWeapon("PROTOGENOI_BLADE");
        WeaponSystem.WeaponRequirement tartarus = WeaponSystem.getRequirementForWeapon("TARTARUS_BLADE");

        // 3. 重新注册所有武器配方
        registerAeonBlade(aeon, recipeConsumer);
        registerAnankeBlade(ananke, recipeConsumer);
        registerAntaugeBlade(antauge, recipeConsumer);
        registerAritemisBlade(aritemis, recipeConsumer);
        registerChaosBlade(chaos, recipeConsumer);
        registerChronosBlade(chronos, recipeConsumer);
        registerChronosnBlade(chronosn, recipeConsumer);
        registerErebusBlade(erebus, recipeConsumer);
        registerHerculesBlade(hercules, recipeConsumer);
        registerNierBlade(nier, recipeConsumer);
        registerProtogenoiBlade(protogenoi, recipeConsumer);
        registerTartarusBlade(tartarus, recipeConsumer);
    }


    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        reloadBladeRecipes(consumer);
    }

    // AEON_BLADE：仅调整proudSoul/killCount/refineCount，材料不变
    private static void registerAeonBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "aeon_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.AEON_BLADE.location())
                .pattern("FMM")
                .pattern("SBS")
                .pattern("MMF")
                // 材料不变（与原配方一致）
                .define('F', SlashBladeItems.PROUDSOUL_SPHERE.get())
                .define('S', ModItems.PROTOGENOI.get())
                .define('M', ModItems.FATESTAR.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.DELIGUN_BLADE.location())
                                // 动态调整合成要求（核心：基础值×难度）
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                // 修复报错2：替换() -> true为合法的CriterionTriggerInstance（使用IConditionBuilder的has方法）
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // ANANKE_BLADE配方
    private static void registerAnankeBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "ananke_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.ANANKE_BLADE.location())
                .pattern("PMR")
                .pattern("CBC")
                .pattern("WSP")
                // 材料不变
                .define('M', Items.OBSIDIAN)
                .define('W', Items.GOLDEN_SWORD)
                .define('S', Items.GOLD_BLOCK)
                .define('R', Items.DIAMOND_SWORD)
                .define('P', ModItems.PROTOGENOI.get())
                .define('C', ModItems.FATESTAR.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.NYX_BLADE.location())
                                // 动态合成要求
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // ANTAUGE_BLADE配方
    private static void registerAntaugeBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "antauge_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.ANTAUGE_BLADE.location())
                .pattern("FPM")
                .pattern("RBW")
                .pattern("SCK")
                .define('M', Items.NETHER_STAR)
                .define('S', Items.EXPERIENCE_BOTTLE)
                .define('W', Items.IRON_INGOT)
                .define('R', Items.GLOWSTONE)
                .define('P', ModItems.CHAOS.get())
                .define('F', SlashBladeItems.PROUDSOUL_SPHERE.get())
                .define('K', SlashBladeItems.PROUDSOUL_INGOT.get())
                .define('C', ModItems.HERCULES.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(SlashBladeBuiltInRegistry.MURAMASA.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // ARITEMIS_BLADE配方
    private static void registerAritemisBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "aritemis_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.ARITEMIS_BLADE.location())
                .pattern(" SW")
                .pattern("SBS")
                .pattern("MS ")
                .define('M', Items.WATER_BUCKET)
                .define('S', ModItems.ARITEMIS.get())
                .define('W', Items.DIAMOND_BLOCK)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(SlashBladeBuiltInRegistry.MURAMASA.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // CHAOS_BLADE配方
    private static void registerChaosBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "chaos_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.CHAOS_BLADE.location())
                .pattern(" SM")
                .pattern("RBP")
                .pattern("WSC")
                .define('M', Items.NETHER_STAR)
                .define('S', ModItems.CHAOS.get())
                .define('W', SlashBladeItems.PROUDSOUL_TINY.get())
                .define('R', Items.LAPIS_BLOCK)
                .define('P', ModItems.PROTOGENOI.get())
                .define('C', Items.GOLD_BLOCK)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.CHRONOS_BLADE.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // CHRONOS_BLADE配方
    private static void registerChronosBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "chronos_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.CHRONOS_BLADE.location())
                .pattern("WP ")
                .pattern("SBM")
                .pattern(" PR")
                .define('S', Items.DIAMOND)
                .define('W', SlashBladeItems.PROUDSOUL_SPHERE.get())
                .define('R', SlashBladeItems.PROUDSOUL_INGOT.get())
                .define('P', ModItems.CHRONOS.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.NIER_BLADE.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .define('M', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(SlashBladeBuiltInRegistry.TUKUMO.location())
                                .proudSoul(0)
                                .killCount(0)
                                .refineCount(0)
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // CHRONOSN_BLADE配方
    private static void registerChronosnBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "chronosn_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.CHRONOSN_BLADE.location())
                .pattern(" W ")
                .pattern("PBM")
                .pattern(" W ")
                .define('M', Items.LAPIS_BLOCK)
                .define('W', ModItems.CHRONOS.get())
                .define('P', Items.OBSIDIAN)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.EREBUS_BLADE.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // EREBUS_BLADE配方
    private static void registerErebusBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "erebus_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.EREBUS_BLADE.location())
                .pattern("WS ")
                .pattern("PBM")
                .pattern(" SR")
                .define('S', ModItems.EREBUS.get())
                .define('W', SlashBladeItems.PROUDSOUL_SPHERE.get())
                .define('R', SlashBladeItems.PROUDSOUL_INGOT.get())
                .define('P', Items.OBSIDIAN)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.CHRONOS_BLADE.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // HERCULES_BLADE配方
    private static void registerHerculesBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "hercules_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.HERCULES_BLADE.location())
                .pattern(" SW")
                .pattern("SBS")
                .pattern("MS ")
                .define('M', Items.LAVA_BUCKET)
                .define('S', ModItems.HERCULES.get())
                .define('W', SlashBladeItems.PROUDSOUL.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // NIER_BLADE配方
    private static void registerNierBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "nier_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.NIER_BLADE.location())
                .pattern("RS ")
                .pattern("SBS")
                .pattern(" WP")
                .define('S', ModItems.CHRONOS.get())
                .define('W', Items.ENDER_EYE)
                .define('R', Items.ENDER_PEARL)
                .define('P', Items.EMERALD)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(SlashBladeBuiltInRegistry.MURAMASA.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // PROTOGENOI_BLADE配方
    private static void registerProtogenoiBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "protogenoi_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.PROTOGENOI_BLADE.location())
                .pattern("SPS")
                .pattern("WBW")
                .pattern("SPS")
                .define('S', Items.ENCHANTING_TABLE)
                .define('W', SlashBladeItems.PROUDSOUL_SPHERE.get())
                .define('P', ModItems.PROTOGENOI.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.CHRONOSN_BLADE.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // TARTARUS_BLADE配方
    private static void registerTartarusBlade(WeaponSystem.WeaponRequirement req, Consumer<FinishedRecipe> consumer) {
        ResourceLocation recipeId = new ResourceLocation(prinegorerouse.MOD_ID, "tartarus_blade");
        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.TARTARUS_BLADE.location())
                .pattern("QPS")
                .pattern("DBW")
                .pattern("CPX")
                .define('S', Items.OBSIDIAN)
                .define('W', Items.NETHER_WART)
                .define('P', ModItems.TARTARUS.get())
                .define('X', Items.BLAZE_ROD)
                .define('C', Items.WITHER_SKELETON_SKULL)
                .define('D', Items.NETHERRACK)
                .define('Q', Items.GHAST_TEAR)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.EREBUS_BLADE.location())
                                .proudSoul(req.getProudSoul())
                                .killCount(req.getKillCount())
                                .refineCount(req.getRefineCount())
                                .build()
                ))
                .unlockedBy("has_slashblade", has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer);
    }

    // 兼容IConditionBuilder的has方法（修复报错2的关键）
    private static @NotNull CriterionTriggerInstance has(Item item) {
        return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }
}