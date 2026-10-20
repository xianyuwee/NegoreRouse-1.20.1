package net.xianyu.prinegorerouse.item;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.xianyu.prinegorerouse.data.ChaosDamageSavedData;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 卡俄斯（CHAOS_BLADE）专用物品：继承 {@link ItemFEBlade} 的 FE 能力（FEBladeStorage 与能量条），
 * 但显示层仅在「该刀携带清耀（Clear）特效」时启用——
 * 即卡俄斯吞噬清耀刀后，无论 Clear 是否激活，都为其呈现 FE 条并可充能。
 */
public class ItemFEChaosBlade extends ItemFEBlade {

    public ItemFEChaosBlade(Tier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder);
    }

    /**
     * 把刀栈的面板攻击力（{@code baseAttackModifier}）改写为指定值，并写回 bladeState tag 以持久化 / 同步。
     * 供“正常合成”与“生存转化”两条产出路径复用（各自取 {@link ChaosDamageSavedData} 的对应随机）。
     */
    public static void bakeBaseAttack(ItemStack stack, float value) {
        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setBaseAttackModifier(value);
            // 写回刀栈根 tag，确保放进背包 / 同步到客户端时携带改写后的攻击力
            stack.getOrCreateTag().put("bladeState", state.serializeNBT());
        });
    }

    /**
     * 正常合成卡俄斯时（服务端回调）：把面板攻击力固定为本存档的 {@code [10,100]} 随机值。
     * 生存转化路径不经合成，由 {@code ChaosBladeHandler} 另行烘焙 {@code (1,10)} 随机值。
     */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (!level.isClientSide() && level instanceof ServerLevel sl) {
            bakeBaseAttack(stack, ChaosDamageSavedData.get(sl).crafted());
        }
    }

    /** FE 条是否可见：仅当刀上携带 Clear 特效（被吞噬获得或本体即清耀刀）。 */
    private static boolean hasClearFE(ItemStack stack) {
        return NRSpecialEffect.carriesSE(stack, NrSpecialEffectsRegistry.Clear.getId());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return hasClearFE(stack) && super.isBarVisible(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendForgeEnergyInfo(ItemStack stack, List<Component> tooltip) {
        if (!hasClearFE(stack)) {
            return;
        }
        super.appendForgeEnergyInfo(stack, tooltip);
    }

    /** 卡俄斯特效界面含吞噬，提示语用 chaos 版 lang 键。 */
    @Override
    protected String seKeyLang() {
        return "tip.prinegorerouse.se_key_chaos";
    }
}
