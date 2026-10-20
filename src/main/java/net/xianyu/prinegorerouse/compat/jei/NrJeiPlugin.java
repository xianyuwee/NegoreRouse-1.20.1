package net.xianyu.prinegorerouse.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.List;

/**
 * JEI 集成（方案 A：物品 ⓘ 文字页）。为本模组<b>每一把拔刀剑</b>在 JEI 物品页挂一个 ⓘ 介绍页，
 * 用于书写该刀专属的 SE / SA 说明。
 *
 * <p>文案来源为 lang 键 {@code jei.prinegorerouse.blade.<物品path>}（如 {@code chaos_blade}），
 * 直接在 {@code zh_cn.json} / {@code en_us.json} 里编辑即可生效，<b>无需重新编译</b>；
 * 一段文字可写多行，用 {@code \n} 分隔，JEI 会自动换行分页。</p>
 *
 * <p>登记的物品栈用 {@link SlashBladeDefinition#getBlade()} 生成的<b>成型刀</b>（带完整刀名数据组件），
 * 而非 {@code new ItemStack(item)}——后者 bladeState 为空，slashblade 的 {@code getName} 会回退显示"无名"，
 * 导致 ⓘ 页顶部图标名字错误。同时只有当某把刀对应的 lang 键<b>确实已填写</b>（{@link I18n#exists(String)}）
 * 时才挂 ⓘ 页，未填写的刀自动跳过，避免 ⓘ 里显示成原始键名。</p>
 *
 * <p>本类带 {@link JeiPlugin} 注解，由 JEI 的 {@code ForgePluginFinder} 在 JEI 存在时反射实例化，
 * 因此普通（未安装 JEI）环境不会触发对 JEI 的硬依赖；请勿在本模组主类里主动引用它。</p>
 */
@JeiPlugin
public class NrJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(prinegorerouse.MOD_ID, "blade_info");

    /** 每把刀的 ⓘ 介绍 lang 键前缀：jei.prinegorerouse.blade.&lt;物品path&gt;。 */
    private static final String INFO_KEY_PREFIX = "jei." + prinegorerouse.MOD_ID + ".blade.";

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 刀定义是数据包注册表，需客户端世界就绪才能取到同步过来的条目；
        // 主菜单下 level 为空时直接跳过，JEI 会在进世界收到配方同步后重建并再次登记。
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        HolderLookup.Provider registries = mc.level.registryAccess();
        List<SlashBladeDefinition> defs = SlashBlade.getSlashBladeDefinitionRegistry(registries)
                .listElements().map(h -> h.value()).toList();
        for (SlashBladeDefinition def : defs) {
            ResourceLocation itemName = def.getItemName();
            // 只处理本模组的刀（物理物品命名空间匹配）
            if (itemName == null || !prinegorerouse.MOD_ID.equals(itemName.getNamespace())) {
                continue;
            }
            String key = INFO_KEY_PREFIX + itemName.getPath();
            // 未提供翻译则跳过，避免 ⓘ 页显示成原始键名
            if (!I18n.exists(key)) {
                continue;
            }
            ItemStack named = def.getBlade();
            if (named.isEmpty()) {
                continue;
            }
            registration.addItemStackInfo(named, Component.translatable(key));
        }
    }
}
