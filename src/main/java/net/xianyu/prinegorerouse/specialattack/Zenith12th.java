package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.entity.EntityZenithSword;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

import java.util.List;

/**
 * 穹顶十二式（Zenith12th）生成逻辑。
 *
 * <p>释放时：
 * <ul>
 *   <li>若玩家已锁定实体（拔刀剑重锋锁定 {@code state.getTargetEntity}）→ 优先以该实体为锥顶；</li>
 *   <li>否则选取玩家视野内离玩家最近的敌对实体为锥顶；</li>
 *   <li>完全没有目标时，以玩家视线前方一点作为虚拟锥顶（剑仍会发射，但无锁定追踪）。</li>
 * </ul>
 * 幻影剑与锥顶构成一个<b>高 {@value #CONE_HEIGHT}、底半径 {@value #CONE_RADIUS} 的倒圆锥</b>：
 * 锥顶在实体位置点，底面圆周位于锥顶正上方 {@value #CONE_HEIGHT} 处；{@value #SWORD_COUNT} 把剑均匀分布在底面圆周上，
 * 初始朝向与发射方向 = 生成位置 − 实体(锥顶) 的单位向量（即由实体指向生成位置、向外炸开），随后由追踪拉回目标形成弧线。延迟 {@value #DELAY_TICKS} tick（1s）后发射，
 * 由 {@link EntityZenithSword} 完成“锁定初始目标不换手 / 目标死亡撞方块消失 / 穿透其它敌对最多三次”的飞行规则。
 */
public class Zenith12th {
    /** 参与生成的幻影剑数量（“十二式”）。 */
    private static final int SWORD_COUNT = 12;
    /** 倒圆锥高：底面圆周在锥顶正上方 5 格。 */
    private static final double CONE_HEIGHT = 5.0;
    /** 倒圆锥底面半径。 */
    private static final double CONE_RADIUS = 3.0;
    /** 发射延迟：1 秒 = 20 tick。 */
    private static final int DELAY_TICKS = 20;
    /** 无锁定目标时，索敌的视野半径。 */
    private static final double SEARCH_RANGE = 32.0;
    /** 剑的颜色（与旧实现一致：金色 0xFFB000 附近）。 */
    private static final int SWORD_COLOR = 16766720;

    public Zenith12th() {
    }

    public static void doSlash(LivingEntity playerIn, boolean critical, float speed) {
        Level world = playerIn.level();
        if (world.isClientSide()) return;

        final int colorCode = playerIn.getMainHandItem()
                .getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.getColorCode())
                .orElse(-13421569);

        playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            // 1) 目标选择：锁定实体优先，其次视野内最近敌对实体
            Entity target = state.getTargetEntity(world);
            if (target == null || !target.isAlive() || MaidCompat.isOwnedBy(target, playerIn)) {
                target = nearestHostileInView(playerIn, world);
            }

            // 2) 锥顶：有目标取实体身体中心；无目标取视线前方一点（虚拟锥顶，不追踪）
            Vec3 apex;
            if (target != null) {
                apex = target.position().add(0, target.getBbHeight() * 0.5, 0);
            } else {
                apex = playerIn.getEyePosition().add(playerIn.getLookAngle().scale(8.0));
            }

            // 3) 伤害随击杀数放大（沿用旧档位）
            double damage;
            if (state.getKillCount() >= 200) {
                damage = state.getBaseAttackModifier() * 6;
            } else if (state.getKillCount() >= 100) {
                damage = state.getBaseAttackModifier() * 4;
            } else {
                damage = state.getBaseAttackModifier() * 3;
            }

            // 4) 在底面圆周上均匀生成，初始朝向/发射方向由实体(锥顶)指向生成位置（向外）
            for (int i = 0; i < SWORD_COUNT; i++) {
                double angle = Math.PI * 2 * i / SWORD_COUNT;
                Vec3 swordPos = apex.add(CONE_RADIUS * Math.cos(angle), CONE_HEIGHT, CONE_RADIUS * Math.sin(angle));

                EntityZenithSword sword = new EntityZenithSword(NrEntitiesRegistry.Enchanted_Sword, world);
                sword.setOwner(playerIn);
                sword.setDamage(damage);
                sword.setSpeed(speed);
                sword.setIsCritical(critical);
                sword.setColor(SWORD_COLOR);
                // 追踪初始锁定实体，不更换目标（无目标则不追踪）
                sword.setLockTarget(target);
                // 生成于顶点圆周、延迟 1s、开启穿墙（目标存活时无视方块）
                sword.setPos(swordPos.x, swordPos.y, swordPos.z);
                sword.setDelayTicks(DELAY_TICKS);
                sword.setNoClip(true);
                sword.setChange(false);
                // 初始方向向量 = 生成位置 − 实体(锥顶)，即由实体指向生成位置（向外）；换算成重锋 DIY yaw/pitch
                applyAimDirection(sword, swordPos.subtract(apex).normalize());

                world.addFreshEntity(sword);
            }

            // 5) 统一的释放表现（每个剑都各播一次太吵，这里整体播一次龙翼+雷声）
            world.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, playerIn.getSoundSource(), 0.6F, 1.4F);
        });
    }

    /**
     * 把给定的方向向量（本技能传入的是“由实体指向生成位置”的向量）写入剑的 DIY yaw/pitch（决定发射物理方向），并额外写入出生朝向。
     *
     * <p>父类 {@code initializeEntity}/{@code launchProjectile}（change=false 分支）按如下视线约定重建方向：
     * {@code x=-sin(yaw)cos(pitch), y=-sin(pitch), z=cos(yaw)cos(pitch)}。
     * 反解得 {@code yaw=atan2(-x, z)}、{@code pitch=asin(-y)}（度）。
     * 若 yaw 与 pitch 同时为 0 会被父类当作“未指定”而走玩家朝向，这里给 yaw 加极小偏移避免歧义。
     *
     * <p>出生朝向：Enchanted_Sword 的 updateInterval=20 且父类 setYRot/setXRot 在 noClip 下被静默拦截，
     * 导致出生包只带默认 +Z，延迟 1s 内（首个朝向同步之前）剑会“朝向反了”。故在 addFreshEntity 前
     * 临时关 noClip，按飞行追踪同款“速度朝向”约定 atan2(v.x,v.z) 显式写入出生旋转（与 {@code EntityFateEye} 一致）。
     */
    private static void applyAimDirection(EntityZenithSword sword, Vec3 dir) {
        float yawDeg = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float pitchDeg = (float) (Math.asin(Mth.clamp(-dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);
        if (yawDeg == 0F && pitchDeg == 0F) {
            yawDeg = 0.0001F;
        }
        sword.setYAW(yawDeg);
        sword.setPITCH(pitchDeg);

        // 出生朝向显式写入（速度朝向约定），规避 noClip 旋转锁 + updateInterval 迟同步导致的“方向反了”
        double hh = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float faceYaw = (float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
        float facePitch = (float) (Mth.atan2(dir.y, hh) * Mth.RAD_TO_DEG);
        sword.setNoClip(false);
        sword.setYRot(faceYaw);
        sword.setXRot(facePitch);
        sword.setNoClip(true);
    }

    /**
     * 选取玩家视野内、离玩家最近的敌对实体（存活、非自身、非归属女仆/宠物、在视线锥内且有直线可见性）。
     */
    private static Entity nearestHostileInView(LivingEntity player, Level world) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = player.getBoundingBox().inflate(SEARCH_RANGE);
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e instanceof Enemy && e.isAlive() && e != player && !MaidCompat.isOwnedBy(e, player));

        LivingEntity best = null;
        double minDist = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double len = to.length();
            if (len > SEARCH_RANGE || len < 1.0E-4) continue;
            // 视野锥判定：与视线夹角约 ≤ 45°
            if (to.normalize().dot(look) < 0.7) continue;
            // 直线可见性
            if (!player.hasLineOfSight(e)) continue;
            double d = eye.distanceToSqr(e.position());
            if (d < minDist) {
                minDist = d;
                best = e;
            }
        }
        return best;
    }
}
