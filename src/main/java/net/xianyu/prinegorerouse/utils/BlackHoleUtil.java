package net.xianyu.prinegorerouse.utils;

import net.minecraft.world.entity.LivingEntity;

public class BlackHoleUtil {
  public static final String BLACK_HOLE_TAG = "blackhole";
  
  public static int getBlackHoleCount(LivingEntity entity) {
    return entity.getPersistentData().getInt("blackhole");
  }
  
  public static void addBlackHoleCount(LivingEntity entity, int amount) {
    int count = getBlackHoleCount(entity);
    entity.getPersistentData().putInt("blackhole", count + amount);
  }
  
  public static void resetBlackHoleCount(LivingEntity entity) {
    entity.getPersistentData().putInt("blackhole", 0);
  }
}