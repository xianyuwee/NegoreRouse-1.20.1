package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityCountableSwords;
import net.xianyu.prinegorerouse.entity.EntityEnchantedSword;
import net.xianyu.prinegorerouse.entity.EntityShinyDrive;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class OverTheHorizon {
    public OverTheHorizon() {}

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, double damage, float speed) {
        doSlash(playerIn, roll, lifetime, damage, KnockBacks.cancel, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, double damage, KnockBacks knockBacks, float speed) {
        int colorCode = playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map((state) -> {
            return state.getColorCode();
        }).orElse(-13421569);
        doSlash(playerIn, roll, lifetime, colorCode, damage, knockBacks, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, int colorCode,
                                double damage, KnockBacks knockBacks, float speed) {
        if (playerIn.level().isClientSide()) return;
        if (!playerIn.level().isClientSide()) {
            playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent((state) -> {
                Level world = playerIn.level();
                int rank = playerIn.getCapability(CapabilityConcentrationRank.RANK_POINT)
                        .map(r -> r.getRank(world.getGameTime()).level).orElse(0);

                int DCount = 1;
                int SCount = 1;

                switch (rank) {
                    case 0,1,2 -> {
                        DCount = 3;
                        SCount = 9;
                    }
                    case 3,4 -> {
                        DCount = 5;
                        SCount = 15;
                    }
                    case 5,6 -> {
                        DCount = 7;
                        SCount = 21;
                    }
                    case 7 -> {
                        DCount = 9;
                        SCount = 9;
                    }
                }

                BlackHoleAttack attack = new BlackHoleAttack();
                attack.setDcount(DCount);
                attack.setScount(SCount);
                attack.doBlackHoleSlash(playerIn, roll, lifetime, damage, knockBacks, speed);

            });
        }
    }
}
