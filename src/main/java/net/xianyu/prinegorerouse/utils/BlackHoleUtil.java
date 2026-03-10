package net.xianyu.prinegorerouse.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;

public class BlackHoleUtil {
    public static final String BLACK_HOLE_TAG = "blackhole";

    // 优化：单次获取Tag，减少重复IO操作
    private static CompoundTag getPersistentTag(Entity entity) {
        return entity.getPersistentData();
    }

    public static int getBlackHoleCount(Entity entity) {
        CompoundTag tag = getPersistentTag(entity);
        return tag.getInt(BLACK_HOLE_TAG);
    }

    public static void addBlackHoleCount(Entity entity, int amount) {
        CompoundTag tag = getPersistentTag(entity);
        int count = tag.getInt(BLACK_HOLE_TAG);
        tag.putInt(BLACK_HOLE_TAG, count + amount);
    }

    public static void resetBlackHoleCount(Entity entity) {
        CompoundTag tag = getPersistentTag(entity);
        tag.putInt(BLACK_HOLE_TAG, 0);
    }
}