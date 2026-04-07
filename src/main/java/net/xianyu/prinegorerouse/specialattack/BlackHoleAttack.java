package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.entity.EntityCountableSwords;
import net.xianyu.prinegorerouse.entity.EntityEnchantedSword;
import net.xianyu.prinegorerouse.entity.EntityShinyDrive;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.BlackHoleUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class BlackHoleAttack {
    private static final Random RANDOM = new Random();

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
            , KnockBacks knockBacks, float speed) {

        Level world = playerIn.level();
        if (world.isClientSide()) return;

        // ==========================================
        // 【新增：伤害计算逻辑】
        // ==========================================
        float baseAttack = (float) playerIn.getAttributeValue(Attributes.ATTACK_DAMAGE);
        int killCount = getBladeKillCount(playerIn);
        float multiplier = getDamageMultiplier(killCount);
        double finalDamage = baseAttack * multiplier;

        Vec3 lookVec = playerIn.getLookAngle();
        Vec3 eyePos = playerIn.getEyePosition(1.0F);
        Vec3 targetPos;
        LivingEntity nearestTarget = findNearestHostileTarget(playerIn);

        if (nearestTarget != null) {
            targetPos = nearestTarget.getEyePosition();
            BlackHoleUtil.addBlackHoleCount(nearestTarget, 1);
        } else {
            targetPos = eyePos.add(lookVec.scale(20));
        }

        int safeDcount = Math.min(this.Dcount, 20);
        int safeScount = Math.min(this.Scount, 30);

        // 传入计算好的 finalDamage
        generateShinyDrives(world, targetPos, playerIn, 5.0, safeDcount, roll, lifetime, finalDamage, knockBacks, speed);
        generateSwords(world, targetPos, playerIn, safeScount, lifetime, finalDamage, speed);
    }

    // ==========================================
    // 【新增：辅助方法】获取拔刀剑击杀数
    // ==========================================
    private int getBladeKillCount(LivingEntity player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof ItemSlashBlade) {
            return stack.getCapability(ItemSlashBlade.BLADESTATE)
                    .map(ISlashBladeState::getKillCount)
                    .orElse(0);
        }
        // 如果副手是刀也可以检查副手，这里默认只检查主手
        return 0;
    }

    // ==========================================
    // 【新增：辅助方法】根据击杀数获取倍率
    // ==========================================
    private float getDamageMultiplier(int killCount) {
        if (killCount >= 1000) {
            return 0.3F;
        } else if (killCount >= 100) {
            return 0.15F;
        } else {
            return 0.1F;
        }
    }

    // ... (下面的 generateShinyDrives, generateSwords, findNearestHostileTarget 方法保持不变，
    //      只需确保它们接收 finalDamage 参数即可，代码逻辑无需改动)

    private static void generateShinyDrives(Level world, Vec3 center, LivingEntity owner, double radius, int count,
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
            drive.setDamage(damage); // 使用传入的动态伤害
            drive.setKnockBack(knockBacks);
            drive.setSpeed(3.0F);
            drive.setDelayTick(20);
            drive.setDelaySpeed(0.01f);
            drive.setColor(0xFFD700);
            drive.shoot(toCenter.x, toCenter.y, toCenter.z, speed, 0.0F);
            world.addFreshEntity(drive);
        }
    }

    private void generateSwords(Level world, Vec3 center, LivingEntity owner, int count,
                                int lifetime, double damage, float speed) {
        for (int i = 0; i < count; i++) {
            double r = 5 + RANDOM.nextDouble() * 5;
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double x = center.x + r * Math.cos(angle);
            double z = center.z + r * Math.sin(angle);
            double y = center.y + 5;
            Vec3 spawnPos = new Vec3(x, y, z);

            Vec3 toCenter = center.subtract(spawnPos);
            double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);
            float yaw = (float) Math.toDegrees(Math.atan2(toCenter.z, toCenter.x)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(toCenter.y, horizontalDistance));

            // 生成EnchantedSword
            EntityEnchantedSword sword = new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, world);
            sword.setPos(spawnPos);
            sword.setYAW(yaw);
            sword.setPITCH(pitch);
            sword.setOwner(owner);
            sword.setUseCustomDirection(true);
            sword.setAutoTargeting(true);
            sword.setDeltaMovement(toCenter.normalize().scale(speed));
            sword.setLifeTime(lifetime);
            sword.setDamage(damage); // 使用传入的动态伤害
            sword.setDelayTicks(i);
            sword.setSpeed(speed);
            sword.setCenterPosition(center);
            sword.setChange(false);
            sword.setColor(245245220);
            sword.setNoClip(true);
            world.addFreshEntity(sword);

            // 生成CountableSwords
            EntityCountableSwords sword2 = new EntityCountableSwords(NrEntitiesRegistry.Countable_Sword, world);
            sword2.setPos(spawnPos);
            sword2.setOwner(owner);
            sword2.setYAW(-yaw);
            sword2.setPITCH(-pitch);
            sword2.setDeltaMovement(toCenter.normalize().scale(speed));
            sword2.setUseCustomDirection(true);
            sword2.setAutoTargeting(true);
            sword2.setLifeTime(lifetime);
            sword2.setDamage(damage); // 使用传入的动态伤害
            sword2.setSpeed(speed);
            sword2.setDelayTicks(i);
            sword2.setCenterPosition(center);
            sword2.setChange(false);
            sword2.setColor(245245220);
            sword2.setNoClip(true);
            world.addFreshEntity(sword2);
        }
    }

    private static LivingEntity findNearestHostileTarget(LivingEntity player) {
        AABB area = player.getBoundingBox().inflate(32);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e ->
                e instanceof Enemy && e.isAlive()
        );
        if (targets.isEmpty()) return null;
        return targets.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }
}