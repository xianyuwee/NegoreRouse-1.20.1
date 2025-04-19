package net.xianyu.prinegorerouse.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class SlashEffectUtils {
    public static boolean hasSpecialEffect(ItemStack stack, String effect) {
        CompoundTag tag = stack.getOrCreateTag(); // 获取或创建NBT标签
        if (tag.contains("bladeState")) { // 检查是否存在ForgeCaps标签
            CompoundTag forgeCaps = tag.getCompound("bladeState");

            if (forgeCaps.contains("SpecialEffects")) { // 检查SpecialEffects标签
                ListTag specialEffects = forgeCaps.getList("SpecialEffects", 8); // 8表示String类型
                for (int i = 0; i < specialEffects.size(); i++) {
                    String currentEffect = specialEffects.getString(i);
                    if (effect.equals(currentEffect)) {
                        return true; // 找到了指定的特殊效果
                    }
                }

            }

        }
        return false; // 没有找到指定的特殊效果
    }
}
