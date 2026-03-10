package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.xianyu.prinegorerouse.specialattack.BlackHoleAttack;

public class OverTheHorizon {
    public OverTheHorizon() {}

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, double damage, float speed) {
        doSlash(playerIn, roll, lifetime, damage, KnockBacks.cancel, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, double damage, KnockBacks knockBacks, float speed) {
        int colorCode = playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getColorCode)
                .orElse(-13421569);
        doSlash(playerIn, roll, lifetime, colorCode, damage, knockBacks, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, int colorCode,
                               double damage, KnockBacks knockBacks, float speed) {
        // 移除冗余的客户端判断（单层判断即可）
        if (playerIn.level().isClientSide()) return;

        playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent((state) -> {
            Level world = playerIn.level();
            int rank = playerIn.getCapability(CapabilityConcentrationRank.RANK_POINT)
                    .map(r -> r.getRank(world.getGameTime()).level)
                    .orElse(0);

            int DCount = 1;
            int SCount = 1;

            // 简化分支判断，减少CPU开销
            if (rank <= 2) {
                DCount = 3;
                SCount = 9;
            } else if (rank <= 4) {
                DCount = 5;
                SCount = 15;
            } else if (rank <= 6) {
                DCount = 7;
                SCount = 21;
            } else if (rank == 7) {
                DCount = 9;
                SCount = 9;
            }

            BlackHoleAttack attack = new BlackHoleAttack();
            attack.setDcount(DCount);
            attack.setScount(SCount);
            attack.doBlackHoleSlash(playerIn, roll, lifetime, damage, knockBacks, speed);
        });
    }
}