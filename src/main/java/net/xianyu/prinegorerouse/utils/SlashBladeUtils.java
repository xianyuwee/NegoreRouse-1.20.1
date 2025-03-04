package net.xianyu.prinegorerouse.utils;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class SlashBladeUtils {
    public static final String RepairCounter = "RepairCounter";
    public static int getcolor(Entity entity) {
        if (entity == null)
            return 0;

        LivingEntity playerIn = (LivingEntity) entity;
        int colorCode = playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.getColorCode()).orElse(0xFF3333FF);
        return colorCode;
    }
    public static boolean hasSpecialEffect(ItemStack stack, String effect) {
        CompoundTag tag = stack.getOrCreateTag(); // 获取或创建NBT标签

        if (tag.contains("bladeState")) { // 检查是否存在ForgeCaps标签
            CompoundTag forgeCaps = tag.getCompound("bladeState");

            if (forgeCaps.contains("SpecialEffects")) { // 检查SpecialEffects标签
                ListTag specialEffects = forgeCaps.getList("SpecialEffects",8); // 8表示String类型
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
    public static boolean hastran(ItemStack stack, String effect) {
        CompoundTag tag = stack.getOrCreateTag(); // 获取或创建NBT标签

        if (tag.contains("bladeState")) { // 检查是否存在ForgeCaps标签
            CompoundTag forgeCaps = tag.getCompound("bladeState");

            if (forgeCaps.contains("translationKey")) { // 检查SpecialEffects标签
                String translationKey = forgeCaps.getString("translationKey");
                if (translationKey.equals(effect)){
                    return true;
                }
            }

        }
        return false; // 没有找到指定的特殊效果
    }
    public static int getNumberNBT(CompoundTag tag , String key){

        if (tag.contains("bladeState")) { // 检查是否存在ForgeCaps标签
            CompoundTag forgeCaps = tag.getCompound("bladeState");

            return forgeCaps.getInt(key);


        }
        return 0;

    }
    public static String getStringNBT(CompoundTag tag , String key){

        if (tag.contains("bladeState")) { // 检查是否存在ForgeCaps标签
            CompoundTag forgeCaps = tag.getCompound("bladeState");

            return forgeCaps.getString(key);


        }
        return "";

    }
    public static void setStringNBT(CompoundTag tag , String key , String value){

        if (tag.contains("bladeState")) { // 检查是否存在ForgeCaps标签
            CompoundTag forgeCaps = tag.getCompound("bladeState");
            tag.putString(key,value);
            forgeCaps.putString(key,value);


        }

    }
}
