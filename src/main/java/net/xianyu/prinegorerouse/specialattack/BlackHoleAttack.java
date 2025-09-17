package net.xianyu.prinegorerouse.specialattack;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityCountableSwords;
import net.xianyu.prinegorerouse.entity.EntityEnchantedSword;
import net.xianyu.prinegorerouse.entity.EntityShinyDrive;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class BlackHoleAttack {
    private int Dcount;
    private int Scount;

    public int getScount() {
        return this.Scount;
    }

    public void setScount(int scount) {
        this.Scount = scount;
    }

    public int getDcount() {
        return this.Dcount;
    }

    public void setDcount(int Dcount) {
        this.Dcount = Dcount;
    }

    public void doBlackHoleSlash(LivingEntity playerIn, float roll, int lifetime, double damage, KnockBacks knockBacks, float speed) {
        Level world = playerIn.level();
        if (world.isClientSide())
            return;
        Vec3 lookVec = playerIn.getLookAngle();
        Vec3 eyePos = playerIn.getEyePosition(1.0F);
        Vec3 targetPos = null;
        LivingEntity nearestTarget = findNearestHostileTarget(playerIn);
        if (nearestTarget != null) {
            targetPos = nearestTarget.position();
        } else {
            targetPos = eyePos.add(lookVec.scale(20.0D));
        }
        generateShinyDrives(world, targetPos, playerIn, 5.0D, this.Dcount, roll, lifetime, damage, knockBacks, speed);
        generateSwords(world, targetPos, playerIn, this.Scount, lifetime, damage, speed);
        launchPhantomBlades(world, targetPos, playerIn, this.Scount);
    }

    private static void generateShinyDrives(Level world, Vec3 center, LivingEntity owner, double radius, int count, float roll, int lifetime, double damage, KnockBacks knockBacks, float speed) {
        for (int i = 0; i < count; i++) {
            EntityShinyDrive drive = new EntityShinyDrive(NrEntitiesRegistry.ShinyDrive, world);
            double angle = 6.283185307179586D * i / count;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;
            drive.setPos(x, y, z);
            drive.setOwner(owner);
            Vec3 toCenter = center.subtract(drive.position());
            double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);
            float yaw = (float) Math.toDegrees(Math.atan2(toCenter.z, toCenter.x)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(toCenter.y, horizontalDistance));
            drive.setYRot(yaw);
            drive.setXRot(pitch);
            drive.setRoll(-90.0F);
            drive.setLifetime(lifetime);
            drive.setDamage(damage);
            drive.setKnockBack(knockBacks);
            drive.setSpeed(3.0F);
            drive.setDelayTick(20);
            drive.setDelaySpeed(0.01F);
            drive.setColor(16766720);
            drive.shoot(toCenter.x, toCenter.y, toCenter.z, speed, 0.0F);
            world.addFreshEntity(drive);
        }
    }

    private void generateSwords(Level world, Vec3 center, LivingEntity owner, int count, int lifetime, double damage, float speed) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            double r = 5.0D + random.nextDouble() * 5.0D;
            double angle = random.nextDouble() * 2.0D * Math.PI;
            double x = center.x + r * Math.cos(angle);
            double z = center.z + r * Math.sin(angle);
            double y = center.y + 5.0D;
            EntityEnchantedSword sword = new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, world);
            sword.setPos(x, y, z);
            Vec3 toCenter = sword.position().subtract(center);
            double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);
            float yaw = (float) Math.toDegrees(Math.atan2(toCenter.z, toCenter.x)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(toCenter.y, horizontalDistance));
            sword.setYAW(Float.valueOf(yaw));
            sword.setPITCH(Float.valueOf(pitch));
            sword.setOwner(owner);
            sword.setUseCustomDirection(true);
            sword.setAutoTargeting(true);
            sword.setDeltaMovement(toCenter);
            sword.setLifeTime(lifetime);
            sword.setDamage(damage);
            sword.setDelayTicks(i);
            sword.setSpeed(speed);
            sword.setCenterPosition(center);
            sword.setChange(false);
            sword.setColor(245245220);
            world.addFreshEntity(sword);
            EntityCountableSwords sword2 = new EntityCountableSwords(NrEntitiesRegistry.Countable_Sword, world);
            sword2.setPos(x, y, z);
            Vec3 toCenter2 = sword2.position().subtract(center);
            double horizontalDistance2 = Math.sqrt(toCenter2.x * toCenter2.x + toCenter2.z * toCenter2.z);
            float yaw2 = (float) Math.toDegrees(Math.atan2(toCenter2.z, toCenter2.x)) - 90.0F;
            float pitch2 = (float) -Math.toDegrees(Math.atan2(toCenter2.y, horizontalDistance2));
            sword2.setOwner(owner);
            sword2.setYAW(Float.valueOf(-yaw2));
            sword2.setPITCH(Float.valueOf(-pitch2));
            sword2.setDeltaMovement(toCenter2);
            sword2.setUseCustomDirection(true);
            sword2.setAutoTargeting(true);
            sword2.setLifeTime(lifetime);
            sword2.setDamage(damage);
            sword2.setSpeed(speed);
            sword2.setDelayTicks(i);
            sword2.setCenterPosition(center);
            sword2.setChange(false);
            sword2.setColor(245245220);
            world.addFreshEntity(sword2);
        }
    }

    private void launchPhantomBlades(Level world, Vec3 target, LivingEntity owner, int count) {
        if (world instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) world;
        } else {
            return;
        }
    }

    private static LivingEntity findNearestHostileTarget(LivingEntity player) {
        AABB area = player.getBoundingBox().inflate(32.0D);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e ->
                (e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive()));
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return targets.isEmpty() ? null : targets.get(0);
    }
}