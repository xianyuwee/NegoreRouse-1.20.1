package net.xianyu.prinegorerouse.item;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.prinegorerouse;

public class BladeEnergyHelper {
    private static final String TAG_BLADE_ENERGY = "BladeEnergy";
    private static final int DEFAULT_MAX_ENERGY = 200_000_000;
    private static final ResourceLocation ARITEMIS_S_BLADE_ID = prinegorerouse.prefix("aritemiss_blade");

    public static boolean isAritemisSBlade(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("bladeState")) {
            CompoundTag bladeState = tag.getCompound("bladeState");
            String translationKey = bladeState.getString("translationKey");
            if ("item.prinegorerouse.aritemiss_blade".equals(translationKey)) return true;
        }
        return false;
    }

    public static int getCurrentEnergy(ItemStack stack) {
        if (!isAritemisSBlade(stack)) return 0;
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_BLADE_ENERGY) : 0;
    }

    public static void setCurrentEnergy(ItemStack stack, int energy) {
        if (!isAritemisSBlade(stack)) return;
        int clamped = Math.max(0, Math.min(energy, DEFAULT_MAX_ENERGY));
        stack.getOrCreateTag().putInt(TAG_BLADE_ENERGY, clamped);
    }

    public static int getMaxEnergy(ItemStack stack) {
        return DEFAULT_MAX_ENERGY;
    }

    public static String formatEnergy(int energy) {
        if (energy >= 1_000_000) {
            double val = energy / 1_000_000.0;
            if (val == (int) val) return (int) val + "M";
            return String.format("%.1fM", val);
        } else if (energy >= 1_000) {
            double val = energy / 1_000.0;
            if (val == (int) val) return (int) val + "K";
            return String.format("%.1fK", val);
        } else {
            return String.valueOf(energy);
        }
    }
}