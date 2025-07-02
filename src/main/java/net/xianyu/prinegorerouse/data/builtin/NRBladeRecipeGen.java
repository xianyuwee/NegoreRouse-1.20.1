package net.xianyu.prinegorerouse.data.builtin;

import mods.flammpfeil.slashblade.data.builtin.SlashBladeBuiltInRegistry;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.recipe.RequestDefinition;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipeBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.xianyu.prinegorerouse.data.WeaponSystem;
import net.xianyu.prinegorerouse.item.ModItems;

import java.util.function.Consumer;

public class NRBladeRecipeGen extends RecipeProvider implements IConditionBuilder {

    private static final int DEFAULT_DIFFICULTY = 5;

    public NRBladeRecipeGen(PackOutput output) {
        super(output);
    }

    //标准：始源，万古 基底 10000荣耀 1000杀敌 10锻
    //     阿南刻 5000荣耀 1000杀敌 10锻
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {

        int difficulty = DEFAULT_DIFFICULTY;

        WeaponSystem.WeaponRequirement aeonRequirement =
                WeaponSystem.getRequirementForWeapon("AEON_BLADE");
        WeaponSystem.WeaponRequirement anankeRequirement =
                WeaponSystem.getRequirementForWeapon("ANANKE_BLADE");
        WeaponSystem.WeaponRequirement antaugeRequirement =
                WeaponSystem.getRequirementForWeapon("ANTAUGE_BLADE");
        WeaponSystem.WeaponRequirement aritemisRequirement =
                WeaponSystem.getRequirementForWeapon("ARITEMIS_BLADE");
        WeaponSystem.WeaponRequirement chaosRequirement =
                WeaponSystem.getRequirementForWeapon("CHAOS_BLADE");
        WeaponSystem.WeaponRequirement chronosRequirement =
                WeaponSystem.getRequirementForWeapon("CHRONOS_BLADE");
        WeaponSystem.WeaponRequirement chronosnRequirement =
                WeaponSystem.getRequirementForWeapon("CHRONOSN_BLADE");
        WeaponSystem.WeaponRequirement erebusRequirement =
                WeaponSystem.getRequirementForWeapon("EREBUS_BLADE");
        WeaponSystem.WeaponRequirement herculesRequirement =
                WeaponSystem.getRequirementForWeapon("HERCULES_BLADE");
        WeaponSystem.WeaponRequirement nierRequirement =
                WeaponSystem.getRequirementForWeapon("NIER_BLADE");
        WeaponSystem.WeaponRequirement protogenoiRequirement =
                WeaponSystem.getRequirementForWeapon("PROTOGENOI_BLADE");
        WeaponSystem.WeaponRequirement tartarusRequirement =
                WeaponSystem.getRequirementForWeapon("TARTARUS_BLADE");

        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.AEON_BLADE.location())
                .pattern("FMM")
                .pattern("SBS")
                .pattern("MMF")
                .define('F', SBItems.proudsoul_sphere)
                .define('S', ModItems.PROTOGENOI.get())
                .define('M', ModItems.FATESTAR.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.DELIGUN_BLADE.location())
                                .proudSoul(aeonRequirement.getProudSoul())
                                .killCount(aeonRequirement.getKillCount())
                                .refineCount(aeonRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.ANANKE_BLADE.location())
                .pattern("PMR")
                .pattern("CBC")
                .pattern("WSP")
                .define('M', Items.OBSIDIAN)
                .define('W', Items.GOLDEN_SWORD)
                .define('S', Items.GOLD_BLOCK)
                .define('R', Items.DIAMOND_SWORD)
                .define('P', ModItems.PROTOGENOI.get())
                .define('C', ModItems.FATESTAR.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.NYX_BLADE.location())
                                .proudSoul(anankeRequirement.getProudSoul())
                                .killCount(anankeRequirement.getKillCount())
                                .refineCount(anankeRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.ANTAUGE_BLADE.location())
                .pattern("FPM")
                .pattern("RBW")
                .pattern("SCK")
                .define('M', Items.NETHER_STAR)
                .define('S', Items.EXPERIENCE_BOTTLE)
                .define('W', Items.IRON_INGOT)
                .define('R', Items.GLOWSTONE)
                .define('P', ModItems.CHAOS.get())
                .define('F', SBItems.proudsoul_sphere)
                .define('K', SBItems.proudsoul_ingot)
                .define('C', ModItems.HERCULES.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(SlashBladeBuiltInRegistry.MURAMASA.location())
                                .proudSoul(antaugeRequirement.getProudSoul())
                                .killCount(antaugeRequirement.getKillCount())
                                .refineCount(antaugeRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

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
                                .proudSoul(aritemisRequirement.getProudSoul())
                                .killCount(aritemisRequirement.getKillCount())
                                .refineCount(aritemisRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.CHAOS_BLADE.location())
                .pattern(" SM")
                .pattern("RBP")
                .pattern("WSC")
                .define('M', Items.NETHER_STAR)
                .define('S', ModItems.CHAOS.get())
                .define('W', SBItems.proudsoul_tiny)
                .define('R', Items.LAPIS_BLOCK)
                .define('P', ModItems.PROTOGENOI.get())
                .define('C', Items.GOLD_BLOCK)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.CHRONOS_BLADE.location())
                                .proudSoul(chaosRequirement.getProudSoul())
                                .killCount(chaosRequirement.getKillCount())
                                .refineCount(chaosRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.CHRONOS_BLADE.location())
                .pattern("WP ")
                .pattern("SBM")
                .pattern(" PR")
                .define('S', Items.DIAMOND)
                .define('W', SBItems.proudsoul_sphere)
                .define('R', SBItems.proudsoul_ingot)
                .define('P', ModItems.CHRONOS.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.NIER_BLADE.location())
                                .proudSoul(chronosRequirement.getProudSoul())
                                .killCount(chronosRequirement.getKillCount())
                                .refineCount(chronosRequirement.getRefineCount())
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
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

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
                                .proudSoul(chronosnRequirement.getProudSoul())
                                .killCount(chronosnRequirement.getKillCount())
                                .refineCount(chronosnRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.EREBUS_BLADE.location())
                .pattern("WS ")
                .pattern("PBM")
                .pattern(" SR")
                .define('S', ModItems.EREBUS.get())
                .define('W', SBItems.proudsoul_sphere)
                .define('R', SBItems.proudsoul_ingot)
                .define('P', Items.OBSIDIAN)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.CHRONOS_BLADE.location())
                                .proudSoul(erebusRequirement.getProudSoul())
                                .killCount(erebusRequirement.getKillCount())
                                .refineCount(erebusRequirement.getRefineCount())
                                .build()
                ))
                .define('M', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(SlashBladeBuiltInRegistry.SABIGATANA.location())
                                .proudSoul(0)
                                .killCount(0)
                                .refineCount(0)
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.HERCULES_BLADE.location())
                .pattern(" SW")
                .pattern("SBS")
                .pattern("MS ")
                .define('M', Items.LAVA_BUCKET)
                .define('S', ModItems.HERCULES.get())
                .define('W', SBItems.proudsoul)
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .proudSoul(herculesRequirement.getProudSoul())
                                .killCount(herculesRequirement.getKillCount())
                                .refineCount(herculesRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

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
                                .proudSoul(nierRequirement.getProudSoul())
                                .killCount(nierRequirement.getKillCount())
                                .refineCount(nierRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

        SlashBladeShapedRecipeBuilder.shaped(NrBladeBuiltInRegistry.PROTOGENOI_BLADE.location())
                .pattern("SPS")
                .pattern("WBW")
                .pattern("SPS")
                .define('S', Items.ENCHANTING_TABLE)
                .define('W', SBItems.proudsoul_sphere)
                .define('P', ModItems.PROTOGENOI.get())
                .define('B', SlashBladeIngredient.of(
                        RequestDefinition.Builder.newInstance()
                                .name(NrBladeBuiltInRegistry.CHRONOSN_BLADE.location())
                                .proudSoul(protogenoiRequirement.getProudSoul())
                                .killCount(protogenoiRequirement.getKillCount())
                                .refineCount(protogenoiRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);

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
                                .proudSoul(tartarusRequirement.getProudSoul())
                                .killCount(tartarusRequirement.getKillCount())
                                .refineCount(tartarusRequirement.getRefineCount())
                                .build()
                ))
                .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade)).save(consumer);
    }
}
