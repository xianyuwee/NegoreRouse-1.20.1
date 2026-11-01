package net.xianyu.prinegorerouse.data;

import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismItems;
import mods.flammpfeil.slashblade.data.builtin.SlashBladeBuiltInRegistry;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.recipe.RequestDefinition;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.data.builtin.NrBladeBuiltInRegistry;
import net.xianyu.prinegorerouse.item.ModItems;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NRBladeRuntimeRecipeRegistry {
    public static boolean hasInjectedOnce = false;

    // 所有动态配方ID（13把刀全覆盖）
    private static final List<ResourceLocation> DYNAMIC_RECIPES = List.of(
            NrBladeBuiltInRegistry.AEON_BLADE.location(),
            NrBladeBuiltInRegistry.ANANKE_BLADE.location(),
            NrBladeBuiltInRegistry.ANTAUGE_BLADE.location(),
            NrBladeBuiltInRegistry.ARITEMIS_BLADE.location(),
            NrBladeBuiltInRegistry.CHAOS_BLADE.location(),
            NrBladeBuiltInRegistry.CHRONOS_BLADE.location(),
            NrBladeBuiltInRegistry.CHRONOSN_BLADE.location(),
            NrBladeBuiltInRegistry.EREBUS_BLADE.location(),
            NrBladeBuiltInRegistry.HERCULES_BLADE.location(),
            NrBladeBuiltInRegistry.NIER_BLADE.location(),
            NrBladeBuiltInRegistry.PROTOGENOI_BLADE.location(),
            NrBladeBuiltInRegistry.TARTARUS_BLADE.location(),
            NrBladeBuiltInRegistry.ARITEMISS_HF_BLADE.location()
    );

    public static void resetInjection() {
        hasInjectedOnce = false;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            prinegorerouse.LOGGER.info("拔刀剑运行时配方系统 加载完成");
        });
    }

    // ====================== 核心：刷新配方（无任何私有访问） ======================
    public static void refreshRecipes(RecipeManager recipeManager) {
        if (recipeManager == null) return;

        prinegorerouse.LOGGER.info("开始重载拔刀剑动态配方");

        // 1. 获取所有原版配方（公共API）
        List<Recipe<?>> allRecipes = new ArrayList<>(recipeManager.getRecipes());

        // 2. 生成全新难度配方
        List<Recipe<?>> newRecipes = generateAllBladeRecipes();

        // 3. 移除旧模组配方：取 DYNAMIC_RECIPES 与“本次实际要生成的配方 id”的并集。
        //    这样即使 DYNAMIC_RECIPES 与 generateAllBladeRecipes 不同步（例如早期遗漏了
        //    aritemiss_blade），也不会残留旧配方导致 replaceRecipes 抛
        //    "Duplicate recipe ignored with ID ..." 而使重载中断、难度不生效。
        Set<ResourceLocation> removeIds = new HashSet<>(DYNAMIC_RECIPES);
        for (Recipe<?> recipe : newRecipes) {
            removeIds.add(recipe.getId());
        }
        allRecipes.removeIf(recipe -> removeIds.contains(recipe.getId()));

        // 4. 加入新配方
        allRecipes.addAll(newRecipes);

        // 5. 官方API替换配方（参数完全匹配）
        recipeManager.replaceRecipes(allRecipes);

        hasInjectedOnce = true;
        prinegorerouse.LOGGER.info("拔刀剑配方重载完成！");
    }

    // ====================== 生成全部13把刀配方 ======================
    private static List<Recipe<?>> generateAllBladeRecipes() {
        List<Recipe<?>> recipes = new ArrayList<>();
        recipes.add(createAeonBladeRecipe());
        recipes.add(createAnankeBladeRecipe());
        recipes.add(createAntaugeBladeRecipe());
        recipes.add(createAritemisBladeRecipe());
        recipes.add(createChaosBladeRecipe());
        recipes.add(createChronosBladeRecipe());
        recipes.add(createChronosnBladeRecipe());
        recipes.add(createErebusBladeRecipe());
        recipes.add(createHerculesBladeRecipe());
        recipes.add(createNierBladeRecipe());
        recipes.add(createProtogenoiBladeRecipe());
        recipes.add(createTartarusBladeRecipe());
        recipes.add(createAritemissBladeRecipe());
        return recipes;
    }

    // ====================== 1. AEON_BLADE ======================
    private static SlashBladeShapedRecipe createAeonBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.AEON_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("AEON_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(SlashBladeItems.PROUDSOUL_SPHERE.get()), Ingredient.of(ModItems.FATESTAR.get()), Ingredient.of(ModItems.FATESTAR.get()),
                Ingredient.of(ModItems.PROTOGENOI.get()), getBladeIngredient(NrBladeBuiltInRegistry.DELIGUN_BLADE.location(), req), Ingredient.of(ModItems.PROTOGENOI.get()),
                Ingredient.of(ModItems.FATESTAR.get()), Ingredient.of(ModItems.FATESTAR.get()), Ingredient.of(SlashBladeItems.PROUDSOUL_SPHERE.get())
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 2. ANANKE_BLADE ======================
    private static SlashBladeShapedRecipe createAnankeBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.ANANKE_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("ANANKE_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.PROTOGENOI.get()), Ingredient.of(Items.OBSIDIAN), Ingredient.of(Items.DIAMOND_SWORD),
                Ingredient.of(ModItems.FATESTAR.get()), getBladeIngredient(NrBladeBuiltInRegistry.NYX_BLADE.location(), req), Ingredient.of(ModItems.FATESTAR.get()),
                Ingredient.of(Items.GOLDEN_SWORD), Ingredient.of(Items.GOLD_BLOCK), Ingredient.of(ModItems.PROTOGENOI.get())
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 3. ANTAUGE_BLADE ======================
    private static SlashBladeShapedRecipe createAntaugeBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.ANTAUGE_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("ANTAUGE_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(SlashBladeItems.PROUDSOUL_SPHERE.get()), Ingredient.of(ModItems.CHAOS.get()), Ingredient.of(Items.NETHER_STAR),
                Ingredient.of(Items.GLOWSTONE), getBladeIngredient(SlashBladeBuiltInRegistry.MURAMASA.location(), req), Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(Items.EXPERIENCE_BOTTLE), Ingredient.of(ModItems.HERCULES.get()), Ingredient.of(SlashBladeItems.PROUDSOUL_INGOT.get())
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 4. ARITEMIS_BLADE ======================
    private static SlashBladeShapedRecipe createAritemisBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.ARITEMIS_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("ARITEMIS_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.EMPTY, Ingredient.of(ModItems.ARITEMIS.get()), Ingredient.of(Items.DIAMOND_BLOCK),
                Ingredient.of(ModItems.ARITEMIS.get()), getBladeIngredient(SlashBladeBuiltInRegistry.MURAMASA.location(), req), Ingredient.of(ModItems.ARITEMIS.get()),
                Ingredient.of(Items.WATER_BUCKET), Ingredient.of(ModItems.ARITEMIS.get()), Ingredient.EMPTY
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 5. CHAOS_BLADE ======================
    private static SlashBladeShapedRecipe createChaosBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.CHAOS_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("CHAOS_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.EMPTY, Ingredient.of(ModItems.CHAOS.get()), Ingredient.of(Items.NETHER_STAR),
                Ingredient.of(Items.LAPIS_BLOCK), getBladeIngredient(NrBladeBuiltInRegistry.CHRONOS_BLADE.location(), req), Ingredient.of(ModItems.PROTOGENOI.get()),
                Ingredient.of(SlashBladeItems.PROUDSOUL_TINY.get()), Ingredient.of(ModItems.CHAOS.get()), Ingredient.of(Items.GOLD_BLOCK)
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 6. CHRONOS_BLADE ======================
    private static SlashBladeShapedRecipe createChronosBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.CHRONOS_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("CHRONOS_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(SlashBladeItems.PROUDSOUL_SPHERE.get()), Ingredient.of(ModItems.CHRONOS.get()), Ingredient.EMPTY,
                Ingredient.of(Items.DIAMOND), getBladeIngredient(NrBladeBuiltInRegistry.NIER_BLADE.location(), req), Ingredient.of(Items.NETHER_STAR),
                Ingredient.EMPTY, Ingredient.of(SlashBladeItems.PROUDSOUL_INGOT.get()), Ingredient.EMPTY
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 7. CHRONOSN_BLADE ======================
    private static SlashBladeShapedRecipe createChronosnBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.CHRONOSN_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("CHRONOSN_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.EMPTY, Ingredient.of(ModItems.CHRONOS.get()), Ingredient.EMPTY,
                Ingredient.of(Items.OBSIDIAN), getBladeIngredient(NrBladeBuiltInRegistry.EREBUS_BLADE.location(), req), Ingredient.of(Items.LAPIS_BLOCK),
                Ingredient.EMPTY, Ingredient.of(ModItems.CHRONOS.get()), Ingredient.EMPTY
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 8. EREBUS_BLADE ======================
    private static SlashBladeShapedRecipe createErebusBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.EREBUS_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("EREBUS_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(SlashBladeItems.PROUDSOUL_SPHERE.get()), Ingredient.of(ModItems.EREBUS.get()), Ingredient.EMPTY,
                Ingredient.of(Items.OBSIDIAN), getBladeIngredient(NrBladeBuiltInRegistry.CHRONOS_BLADE.location(), req), Ingredient.EMPTY,
                Ingredient.EMPTY, Ingredient.of(ModItems.EREBUS.get()), Ingredient.of(SlashBladeItems.PROUDSOUL_INGOT.get())
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 9. HERCULES_BLADE ======================
    private static SlashBladeShapedRecipe createHerculesBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.HERCULES_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("HERCULES_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.EMPTY, Ingredient.of(ModItems.HERCULES.get()), Ingredient.of(SlashBladeItems.PROUDSOUL.get()),
                Ingredient.of(ModItems.HERCULES.get()), getBladeIngredient(null, req), Ingredient.of(ModItems.HERCULES.get()),
                Ingredient.of(Items.LAVA_BUCKET), Ingredient.of(ModItems.HERCULES.get()), Ingredient.EMPTY
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 10. NIER_BLADE ======================
    private static SlashBladeShapedRecipe createNierBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.NIER_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("NIER_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(Items.ENDER_PEARL), Ingredient.of(ModItems.CHRONOS.get()), Ingredient.EMPTY,
                Ingredient.of(ModItems.CHRONOS.get()), getBladeIngredient(SlashBladeBuiltInRegistry.MURAMASA.location(), req), Ingredient.of(ModItems.CHRONOS.get()),
                Ingredient.EMPTY, Ingredient.of(Items.ENDER_EYE), Ingredient.of(Items.EMERALD)
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 11. PROTOGENOI_BLADE ======================
    private static SlashBladeShapedRecipe createProtogenoiBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.PROTOGENOI_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("PROTOGENOI_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(Items.ENCHANTING_TABLE), Ingredient.of(ModItems.PROTOGENOI.get()), Ingredient.of(Items.ENCHANTING_TABLE),
                Ingredient.of(SlashBladeItems.PROUDSOUL_SPHERE.get()), getBladeIngredient(NrBladeBuiltInRegistry.CHRONOSN_BLADE.location(), req), Ingredient.of(SlashBladeItems.PROUDSOUL_SPHERE.get()),
                Ingredient.of(Items.ENCHANTING_TABLE), Ingredient.of(ModItems.PROTOGENOI.get()), Ingredient.of(Items.ENCHANTING_TABLE)
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 12. TARTARUS_BLADE ======================
    private static SlashBladeShapedRecipe createTartarusBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.TARTARUS_BLADE.location();
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("TARTARUS_BLADE");

        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(Items.GHAST_TEAR), Ingredient.of(ModItems.TARTARUS.get()), Ingredient.of(Items.OBSIDIAN),
                Ingredient.of(Items.NETHERRACK), getBladeIngredient(NrBladeBuiltInRegistry.EREBUS_BLADE.location(), req), Ingredient.of(Items.NETHER_WART),
                Ingredient.of(Items.WITHER_SKELETON_SKULL), Ingredient.of(ModItems.TARTARUS.get()), Ingredient.of(Items.BLAZE_ROD)
        );

        ShapedRecipe base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3,3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 13. ARITEMISS_BLADE ======================
    private static SlashBladeShapedRecipe createAritemissBladeRecipe() {
        ResourceLocation id = NrBladeBuiltInRegistry.ARITEMISS_HF_BLADE.location();
        ShapedRecipe base;
        WeaponSystem.WeaponRequirement req = WeaponSystem.getRequirementForWeapon("ARITEMIS_S_BLADE");

        if (ModList.get().isLoaded("mekanism")) {
            NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                    Ingredient.of(MekanismBlocks.ULTIMATE_ENERGY_CUBE), Ingredient.of(Items.NETHER_STAR), Ingredient.of(MekanismBlocks.ULTIMATE_ENERGY_CUBE),
                    Ingredient.of(MekanismBlocks.ULTIMATE_INDUCTION_CELL), getBladeIngredient(NrBladeBuiltInRegistry.ARITEMIS_BLADE.location(), req), Ingredient.of(MekanismBlocks.ULTIMATE_INDUCTION_PROVIDER),
                    Ingredient.of(MekanismBlocks.ULTIMATE_ENERGY_CUBE), Ingredient.of(Items.DIAMOND_SWORD), Ingredient.of(MekanismBlocks.ULTIMATE_ENERGY_CUBE));
            base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3, 3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        } else {
            NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                    Ingredient.of(Items.DEEPSLATE_REDSTONE_ORE), Ingredient.of(Items.DIAMOND_SWORD), Ingredient.of(Items.DEEPSLATE_REDSTONE_ORE),
                    Ingredient.of(Items.DAYLIGHT_DETECTOR), getBladeIngredient(NrBladeBuiltInRegistry.ARITEMIS_BLADE.location(), req), Ingredient.of(Items.DAYLIGHT_DETECTOR),
                    Ingredient.of(Items.DEEPSLATE_LAPIS_ORE), Ingredient.of(Items.NETHER_STAR), Ingredient.of(Items.DEEPSLATE_LAPIS_ORE));
            base = new ShapedRecipe(id, "", CraftingBookCategory.EQUIPMENT, 3, 3, ingredients, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        }

        return new NRBladeShapedRecipe(base, id);
    }

    // ====================== 工具：拔刀剑合成要求（自动读取WeaponSystem） ======================
    private static SlashBladeIngredient getBladeIngredient(ResourceLocation baseBlade, WeaponSystem.WeaponRequirement req) {
        RequestDefinition.Builder builder = RequestDefinition.Builder.newInstance()
                .proudSoul(req.getProudSoul())
                .killCount(req.getKillCount())
                .refineCount(req.getRefineCount());

        if (baseBlade != null) {
            builder.name(baseBlade);
            // 关键：用该母刀“自己的物理物品”走 of(ItemLike, request) 重载来构造成分。
            // 默认 of(request) 会把展示物品写死为 slashblade:slashblade（刀名只作为数据组件），
            // 导致 JEI 合成表里母刀显示的仍是 slashblade:slashblade，无法点击链式查看它的上级配方。
            // 传具体物品后，展示/匹配都用该刀自己的注册物品（如 prinegorerouse:chronos_blade），链式查看恢复。
            Item bladeItem = ForgeRegistries.ITEMS.getValue(baseBlade);
            if (bladeItem != null && bladeItem != Items.AIR) {
                return SlashBladeIngredient.of(bladeItem, builder.build());
            }
        }
        // 无独立注册物品时（如原版共享 slashblade:slashblade 的刀，或 baseBlade 为 null 的通用母刀要求）回退默认行为。
        return SlashBladeIngredient.of(builder.build());
    }
}