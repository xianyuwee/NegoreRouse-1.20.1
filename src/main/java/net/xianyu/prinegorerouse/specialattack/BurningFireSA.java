package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityDrive_5ye;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.VectorHelper;

public class BurningFireSA{
    public BurningFireSA() {
    }

    public static void doSlash(LivingEntity playerIn, Vec3 centerOffset, boolean critical, double damage, float speed) {
        doSlash(playerIn, centerOffset, critical, damage, KnockBacks.cancel, speed);
    }

    public static void doSlash(LivingEntity playerIn, Vec3 centerOffset, boolean critical, double damage, KnockBacks knockBacks, float speed) {
        if (playerIn.level().isClientSide()) return;

        Player player = (Player) playerIn;
        int level = player.experienceLevel;
        int count = Math.max(level/5, 1);
        if (count > 5){
            count = 5;
        }
        Vec3 lookAngle = playerIn.getLookAngle();
        Vec3 pos = playerIn.position().add(0.0D, (double) playerIn.getEyeHeight() * 0.75D, 0.0D);
        pos = pos.add(VectorHelper.getVectorForRotation(-90.0F, playerIn.getViewYRot(0)).scale(centerOffset.y))
                .add(VectorHelper.getVectorForRotation(0, playerIn.getViewYRot(0) + 90).scale(centerOffset.z))
                .add(lookAngle.scale(centerOffset.z));

            EntityDrive_5ye driveEx = new EntityDrive_5ye(NrEntitiesRegistry.Drive5_ye, playerIn.level());
            driveEx.setColor(0xFF0000);
            playerIn.level().addFreshEntity(driveEx);
            driveEx.setDamage(damage);
            driveEx.setSpeed(speed);
            driveEx.setPos(pos.x, pos.y, pos.z);
            driveEx.shoot(lookAngle.x, lookAngle.y, lookAngle.z, driveEx.getSpeed(), 0);

            driveEx.setOwner(playerIn);
            driveEx.setRotationRoll(90.0F);
            driveEx.setIsCritical(critical);
            driveEx.setNoClip(true);
            driveEx.setKnockBack(knockBacks);
            driveEx.setLifetime(100);
            if (playerIn != null) {
                playerIn.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                        .ifPresent(rank -> driveEx.setRank(rank.getRankLevel(playerIn.level().getGameTime())));
            }
            if (driveEx.getHitEntity() != null) {
                driveEx.getHitEntity().setSecondsOnFire(3);
            }
            player.playSound(SoundEvents.FIRE_EXTINGUISH, 0.2F, 1.0F);
        }
    }
