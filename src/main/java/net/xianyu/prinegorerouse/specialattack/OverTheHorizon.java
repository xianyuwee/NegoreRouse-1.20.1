package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class OverTheHorizon {
  public static void doSlash(LivingEntity playerIn, float roll, int lifetime, double damage, float speed) {
    doSlash(playerIn, roll, lifetime, damage, KnockBacks.cancel, speed);
  }
  
  public static void doSlash(LivingEntity playerIn, float roll, int lifetime, double damage, KnockBacks knockBacks, float speed) {
    int colorCode = playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
        .map(state -> state.getColorCode())
        .orElse(-13421569);
    doSlash(playerIn, roll, lifetime, colorCode, damage, knockBacks, speed);
  }
  
  public static void doSlash(LivingEntity playerIn, float roll, int lifetime, int colorCode, double damage, KnockBacks knockBacks, float speed) {
    if (playerIn.level().isClientSide())
      return; 
    
    playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
      Level world = playerIn.level();
      // 修复类型不匹配问题：使用方法引用转换为lambda表达式
      int rank = playerIn.getCapability(CapabilityConcentrationRank.RANK_POINT)
          .map(rankPoint -> (int) rankPoint.getRankLevel(20L)) // 添加类型转换
          .orElse(0);
      int DCount = 1;
      int SCount = 1;
      switch (rank) {
        case 0:
        case 1:
        case 2:
          DCount = 3;
          SCount = 9;
          break;
        case 3:
        case 4:
          DCount = 5;
          SCount = 15;
          break;
        case 5:
        case 6:
          DCount = 7;
          SCount = 21;
          break;
        case 7:
          DCount = 9;
          SCount = 9;
          break;
      } 
      BlackHoleAttack attack = new BlackHoleAttack();
      attack.setDcount(DCount);
      attack.setScount(SCount);
      attack.doBlackHoleSlash(playerIn, roll, lifetime, damage, knockBacks, speed);
    });
  }
}