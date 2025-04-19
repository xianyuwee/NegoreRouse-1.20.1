package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.entity.EntityDriveEx;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.VectorHelper;

import static java.lang.Math.*;

public class DivineCrossSA {
    public DivineCrossSA() {
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, Vec3 centerOffset, boolean critical, boolean clip, double damage, float speed) {
        doSlash(playerIn, roll, lifetime, centerOffset, critical, clip, damage, KnockBacks.cancel, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, Vec3 centerOffset, boolean critical, boolean clip, double damage, KnockBacks knockBacks, float speed) {
        int colorCode = (Integer) playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map((state) -> {
            return state.getColorCode();
        }).orElse(-13421569);
        doSlash(playerIn, roll, lifetime, colorCode, centerOffset, critical, clip, damage, knockBacks, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, int colorCode, Vec3 centerOffset,
                                      boolean critical, boolean clip, double damage, KnockBacks knockback, float speed) {
        if (playerIn.level().isClientSide()) return;

        int count = 16;

        for (int i = 0; i <= count; i += 1 ) {
            EntityDriveEx driveEx = new EntityDriveEx(NrEntitiesRegistry.DriveEx, playerIn.level());
            boolean isRight = i % 2 ==0;

            Vec3 lookAngle = playerIn.getLookAngle();

            double x_ = asin(lookAngle.x);
            double z_ = asin(lookAngle.z);

            Vec3 pos = playerIn.position().add(0.0D, (double) playerIn.getEyeHeight() * 0.75D, 0.0D);

            pos = pos.add(VectorHelper.getVectorForRotation(-90.0F, playerIn.getViewYRot(0)).scale(centerOffset.y))
                    .add(VectorHelper.getVectorForRotation(0, playerIn.getViewYRot(0) + 90).scale(centerOffset.z))
                    .add(lookAngle.scale(centerOffset.z));

            playerIn.level().addFreshEntity(driveEx);
            driveEx.setDamage(damage);
            driveEx.setSpeed(speed);
            driveEx.setDelay(20);

            if (x_ * z_ >= 0) {
                driveEx.setPos(pos.x + sin(x_ + (22.5 * i * PI /180) * (double) (isRight ? 1:-1)),
                        pos.y,
                        pos.z + (sin(z_ + (22.5 * (i-1) * PI /180)) * (double) (isRight ? 1:-1)));
                driveEx.shoot(sin(x_ + (22.5 * i * PI /180) * (double) (isRight ? 1:-1)),
                        0.0D,
                        cos(z_ + (22.5 * (i * PI /180)) * (double) (isRight ? 1:-1)),
                        driveEx.getSpeed(),
                        0);
            } else {
                driveEx.setPos(pos.x - sin(x_ + (22.5 * i * PI /180) * (double) (isRight ? 1:-1)),
                        pos.y,
                        pos.z - (sin(z_ + (22.5 * (i-1) * PI /180)) * (double) (isRight ? 1:-1)));
                driveEx.shoot(sin(x_ - (22.5 * i * PI /180) * (double) (isRight ? 1:-1)),
                        0.0D,
                        sin(z_ - (22.5 * (i * PI /180)) * (double) (isRight ? 1:-1)),
                        driveEx.getSpeed(),
                        0);
            }
            driveEx.setOwner(playerIn);
            driveEx.setRotationRoll(roll);

            driveEx.setDelay(20);

            driveEx.setColor(colorCode);
            driveEx.setIsCritical(critical);
            driveEx.setNoClip(clip);
            driveEx.setKnockBack(knockback);
            driveEx.setLifetime(lifetime);
            if (playerIn != null) {
                playerIn.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                        .ifPresent(rank -> driveEx.setRank(rank.getRankLevel(playerIn.level().getGameTime())));
            }
        }
        if (playerIn.level() instanceof ServerLevel && NRConfig.TIME_CAN_CHANGE.get().equals(true)) {
            ServerLevel serverLevel = (ServerLevel) playerIn.level();
            serverLevel.setDayTime(serverLevel.getDayTime() + 12000);
        } else return;
    }
}
