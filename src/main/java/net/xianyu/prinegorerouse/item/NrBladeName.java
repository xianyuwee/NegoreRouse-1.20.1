package net.xianyu.prinegorerouse.item;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.logging.Level;


@OnlyIn(Dist.CLIENT)
public class NrBladeName extends ItemSlashBlade {
    public NrBladeName(Tier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder);
    }

    public void appendNrSwordType(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flag) {
        var swordType = SwordType.from(stack);
        if (swordType.contains(SwordType.BEWITCHED)) {
            if (stack.getHoverName().toString().contains("prinegorerouse")) {
                tooltip.remove(net.minecraft.network.chat.Component.translatable("slashblade.sword_type.bewitched").withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(net.minecraft.network.chat.Component.translatable("prinegorerouse.sword_type.godlike").withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
