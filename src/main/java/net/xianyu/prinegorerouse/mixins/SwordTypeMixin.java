package net.xianyu.prinegorerouse.mixins;

import static net.xianyu.prinegorerouse.prinegorerouse.LOGGER;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xianyu.prinegorerouse.utils.SlashBladeUtils;
import net.xianyu.prinegorerouse.utils.SlashEffectUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.stream.Stream;


@Mixin(ItemSlashBlade.class)
public abstract class SwordTypeMixin {
    @Shadow protected abstract String stackDefaultDescriptionId(ItemStack stack);

    private static boolean isSpecialBlade(ItemStack stack){
        return Stream.of(
                "prinegorerouse:oracle",
                "prinegorerouse:empty",
                "prinegorerouse:clear",
                "prinegorerouse:eternity"
        ).anyMatch(effectId ->
                SlashEffectUtils.hasSpecialEffect(stack, effectId) ||
                        SlashBladeUtils.hasSpecialEffect(stack, effectId)
        );
    }

    @Inject(method = "appendSwordType", cancellable = true, at = @At("HEAD"), remap = false)
    private void appendSwordType(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn, CallbackInfo callbackInfo) {
        var swordType = SwordType.from(stack);

        if(!isSpecialBlade(stack)) return;
        try {
                tooltip.add(
                        Component.translatable("prinegorerouse.sword_type.godlike").withStyle(ChatFormatting.GOLD)
                );
                callbackInfo.cancel();
            } catch (Exception e) {
            LOGGER.error("Tooltip injection failed: " + e.getMessage());
        }
    }
}
