package net.xianyu.prinegorerouse.mixins;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.specialeffect.ChaosHandler;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 特效 tooltip 重写：把原本显示“经验等级需求”的位置改为显示“时间素激活需求”，
 * 并按激活状态着色——未激活为灰色（去掉 lang 里的 § 代码），激活后按 lang 文件的字体颜色显示。
 * <p>激活状态<b>按单把刀判定</b>：{@code appendSpecialEffects} 只持有 {@link ISlashBladeState} 而没有 ItemStack，
 * 因此在 {@code appendHoverText}（含具体刀栈）HEAD 处把当前刀栈暂存到静态字段，渲染特效时读取该刀 NBT。
 * 渲染在客户端主线程同步进行，暂存字段安全。</p>
 * <p>本 mixin 只在客户端生效（注册在 mixins.json 的 client 列表），可安全引用 {@link Minecraft}。
 * 对非本模组（命名空间不是 prinegorerouse）的原生特效，保持原版按经验等级的渲染逻辑不变。</p>
 */
@Mixin(ItemSlashBlade.class)
public class SpecialEffectTooltipMixin {

    /** 当前正在生成 tooltip 的刀栈，由 appendHoverText 捕获。 */
    private static ItemStack nr$currentStack = ItemStack.EMPTY;

    @Inject(method = "appendHoverText", at = @At("HEAD"))
    private void nr$captureStack(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        nr$currentStack = stack;
    }

    /** 卡俄斯专属信息：累计吞噬时间素（翻倍显示）与当前阶段。 */
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void nr$onChaosTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        if (!NRSpecialEffect.isChaosBlade(stack)) {
            return;
        }
        long shown = ChaosHandler.devouredAqua(stack) * 2L;
        int stage = ChaosHandler.stage(stack);
        tooltip.add(Component.translatable("screen.prinegorerouse.chaos.aqua", shown).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("screen.prinegorerouse.chaos.stage",
                stage, NRConfig.chaosMaxStage()).withStyle(ChatFormatting.GOLD));
        // 随机 SA 提示：开启时把当前 SA 名以删除线显示，提示其已被随机接管
        if (ChaosHandler.isRandomMode(stack)) {
            stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(st -> {
                ResourceLocation sa = st.getSlashArtsKey();
                if (sa != null) {
                    Component saName = Component
                            .translatable("slash_art." + sa.getNamespace() + "." + sa.getPath())
                            .withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.GRAY);
                    tooltip.add(Component.translatable("screen.prinegorerouse.chaos.random.hint", saName)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            });
        }
    }

    @Inject(method = "appendSpecialEffects", cancellable = true, at = @At("HEAD"), remap = false)
    private void nr$onAppendSpecialEffects(List<Component> tooltip, ISlashBladeState state, CallbackInfo ci) {
        if (state.getSpecialEffects().isEmpty()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ci.cancel();
        ItemStack blade = nr$currentStack;
        for (ResourceLocation se : state.getSpecialEffects()) {
            tooltip.add(nr$renderSeLine(player, blade, se));
        }
    }

    private static Component nr$renderSeLine(Player player, ItemStack blade, ResourceLocation se) {
        // 非本模组特效：完全保留原版渲染（经验等级门槛）
        if (!prinegorerouse.MOD_ID.equals(se.getNamespace())) {
            boolean showingLevel = SpecialEffect.getRequestLevel(se) > 0;
            Component req = Component.literal(showingLevel ? String.valueOf(SpecialEffect.getRequestLevel(se)) : "")
                    .withStyle(SpecialEffect.isEffective(se, player.experienceLevel) ? ChatFormatting.RED : ChatFormatting.DARK_GRAY);
            return Component.translatable("slashblade.tooltip.special_effect", SpecialEffect.getDescription(se), req)
                    .withStyle(ChatFormatting.GRAY);
        }

        // 本模组特效：按单把刀的时间素激活判定 + 成本显示（成本显示在原本经验需求的位置）
        boolean active = NRSpecialEffect.isAquaActive(blade, se);
        long cost = NRSpecialEffect.costOf(se);
        Component name;
        if (active) {
            // 激活：使用 lang 中的原始颜色（含 § 代码）
            name = SpecialEffect.getDescription(se);
        } else {
            // 未激活：去掉 § 代码并强制灰色
            name = Component.literal(stripCodes(SpecialEffect.getDescription(se).getString()))
                    .withStyle(ChatFormatting.GRAY);
        }
        Component req = Component.literal(cost > 0L ? String.valueOf(cost) : "")
                .withStyle(active ? ChatFormatting.RED : ChatFormatting.DARK_GRAY);
        return Component.translatable("slashblade.tooltip.special_effect", name, req).withStyle(ChatFormatting.GRAY);
    }

    private static String stripCodes(String s) {
        return s == null ? "" : s.replaceAll("(?i)§.", "");
    }
}
