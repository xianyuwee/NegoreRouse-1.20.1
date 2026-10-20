package net.xianyu.prinegorerouse.data;

import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * 修正 {@link SlashBladeShapedRecipe} 的产物计算。
 *
 * <p>父类 getResultItem 的逻辑是：先用产物 RL 去 ITEMS 注册表查物理物品，
 * 若查到的物品 key 恰好等于该 RL，就直接返回它的 {@code getDefaultInstance()}。
 * 本项目每把 NR 刀都注册了与刀定义同 id 的独立物理物品（见把 SlashBladeDefinition
 * 改用双 ResourceLocation 构造后），于是命中这条捷径，返回的是缺少“刀名”数据组件的
 * 裸物品——即无铭/无名。这会让 JEI 配方产物与合成产物都显示成无铭。
 *
 * <p>这里强制走 {@link SlashBladeDefinition#getBlade()}，返回带完整刀名数据组件的成品刀，
 * 与创造栏、/give 拿到的刀保持一致。
 */
public class NRBladeShapedRecipe extends SlashBladeShapedRecipe {
    public NRBladeShapedRecipe(ShapedRecipe base, ResourceLocation outputBlade) {
        super(base, outputBlade);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        ResourceLocation blade = getOutputBlade();
        SlashBladeDefinition def = registryAccess
                .registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
                .get(ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, blade));
        if (def != null) {
            ItemStack named = def.getBlade();
            // 防御：若该刀定义的物理物品无法解析（getBlade 返回空栈），回退父类逻辑，
            // 避免 JEI / 合成拿到空产物。
            if (!named.isEmpty()) {
                return named;
            }
        }
        return super.getResultItem(registryAccess);
    }
}
