package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntityBlisteringSwords;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityCountableSwords;
import net.xianyu.prinegorerouse.entity.EntityEnchantedSword;
import net.xianyu.prinegorerouse.entity.EntityNRBlisteringSword;
import net.xianyu.prinegorerouse.entity.EntityShinyDrive;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.BlackHoleUtil;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class BlackHoleAttack {
    private int Dcount;
    private int Scount;


    public int getScount() {
        return Scount;
    }

    public void setScount(int scount) {
        Scount = scount;
    }

    public int getDcount() {
        return Dcount;
    }

    public void setDcount(int Dcount) {
        this.Dcount = Dcount;
    }
    public void doBlackHoleSlash(LivingEntity playerIn, float roll, int lifetime
            , double damage, KnockBacks knockBacks, float speed) {

        Level world = playerIn.level();

        if (world.isClientSide()) return;

        Vec3 lookVec = playerIn.getLookAngle();
        Vec3 eyePos = playerIn.getEyePosition(1.0F);
        Vec3 targetPos = null;
        LivingEntity nearestTarget = findNearestHostileTarget(playerIn);


        if (nearestTarget != null) {
            targetPos = nearestTarget.getEyePosition();
        } else {
            targetPos = eyePos.add(lookVec.scale(20));
        }

        // Step 1: 生成 ShinyDrive 圆形分布
        generateShinyDrives(world, targetPos, playerIn, 5.0, this.Dcount, roll, lifetime, damage, knockBacks, speed);

        // Step 2: 生成 CountableSwords & EnchantedSword 环形分布
        generateSwords(world, targetPos, playerIn, this.Scount, lifetime, damage,  speed);

        // Step 3: 发射幻影剑
        launchPhantomBlades(world, targetPos, playerIn, this.Scount);
    }

    private static void generateShinyDrives(Level world, Vec3 center, LivingEntity owner, double radius, int count ,
                                            float roll, int lifetime, double damage, KnockBacks knockBacks, float speed) {
        for (int i = 0; i < count; i++) {
            EntityShinyDrive drive = new EntityShinyDrive(NrEntitiesRegistry.ShinyDrive, world);
            double angle = 2 * Math.PI * i / count;
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
            drive.setDelayTick(20); // 设置 delayTick 为20
            drive.setDelaySpeed(0.01f); // speed=0
            drive.setColor(0xFFD700);
            drive.shoot(toCenter.x, toCenter.y, toCenter.z, speed, 0.0F);
            world.addFreshEntity(drive);
        }
    }

    private void generateSwords(Level world, Vec3 center, LivingEntity owner, int count ,
                                int lifetime, double damage, float speed) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            double r = 5 + random.nextDouble() * 5;
            double angle = random.nextDouble() * 2 * Math.PI;
            double x = center.x + r * Math.cos(angle);
            double z = center.z + r * Math.sin(angle);
            double y = center.y + 5;

            EntityEnchantedSword sword =
                    new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, world);
            sword.setPos(x, y, z);
            Vec3 toCenter = sword.position().subtract(center);
            double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);
            float yaw = (float) Math.toDegrees(Math.atan2(toCenter.z, toCenter.x)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(toCenter.y, horizontalDistance));
            sword.setYAW(yaw);
            sword.setPITCH(pitch);
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

            EntityCountableSwords sword2 =
                    new EntityCountableSwords(NrEntitiesRegistry.Countable_Sword, world);
            sword2.setPos(x, y, z);
            Vec3 toCenter2 = sword2.position().subtract(center);
            double horizontalDistance2 = Math.sqrt(toCenter2.x * toCenter2.x + toCenter2.z * toCenter2.z);
            float yaw2 = (float) Math.toDegrees(Math.atan2(toCenter2.z, toCenter2.x)) - 90.0F;
            float pitch2 = (float) -Math.toDegrees(Math.atan2(toCenter2.y, horizontalDistance2));
            sword2.setOwner(owner);
            sword2.setYAW(-yaw2);
            sword2.setPITCH(-pitch2);
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
        if (!(world instanceof ServerLevel serverLevel)) return;
    }

    private static LivingEntity findNearestHostileTarget(LivingEntity player) {
        AABB area = player.getBoundingBox().inflate(32);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e ->
            e instanceof Enemy && e.isAlive()
        );
        targets.sort(Comparator.comparingDouble(e -> e.distanceTo(player)));
        return targets.isEmpty() ? null : targets.get(0);
    }
}
