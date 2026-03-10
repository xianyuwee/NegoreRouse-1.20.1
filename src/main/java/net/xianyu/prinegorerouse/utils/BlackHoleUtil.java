package net.xianyu.prinegorerouse.utils;

import net.minecraft.world.entity.LivingEntity;

public class BlackHoleUtil {
    public static final String BLACK_HOLE_TAG = "blackhole";

    public static int getBlackHoleCount(LivingEntity entity) {
        return entity.getPersistentData().getInt(BLACK_HOLE_TAG);
    }

    public static void addBlackHoleCount(LivingEntity entity, int amount) {
        int count = getBlackHoleCount(entity);
        entity.getPersistentData().putInt(BLACK_HOLE_TAG, count + amount);
    }

    public static void resetBlackHoleCount(LivingEntity entity) {
        entity.getPersistentData().putInt(BLACK_HOLE_TAG, 0);
    }
}
