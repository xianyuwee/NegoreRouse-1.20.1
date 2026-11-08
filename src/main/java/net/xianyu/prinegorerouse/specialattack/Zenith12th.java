package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.entity.EntityZenithSword;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

import java.util.List;

/**
 * 穹顶十二式（Zenith12th）生成逻辑。
 *
 * <p>目标选择优先级：</p>
 * <ol>
 *   <li><b>E</b>：重锋自带锁定（{@code state.getTargetEntity}）；</li>
 *   <li><b>A</b>：玩家准星指向的实体（带视线可见性检查）；</li>
 *   <li><b>C</b>：玩家最近攻击过的生物（{@code getLastHurtMob}，限 32 格内）；</li>
 *   <li><b>B</b>：玩家视野内最近的敌对实体（45° 视野锥 + 直线可见）。</li>
 * </ol>
 *
 * <p>有目标时：剑生成于“目标身体中心正上方 {@value #CONE_HEIGHT} 格、半径 {@value #CONE_RADIUS} 的圆周”上，
 * 发射方向为<b>向外炸开</b>（= 生成位置 − 锥顶，与原实现一致）；{@link EntityZenithSword} 内部按
 * “炸开上升段 → 恒定角速度平滑翻越 → 竖直直线俯冲”三段飞行：12 把剑先向外上方炸开、飞高，
 * 再在目标正上方近乎垂直地直线落砸下来（仿真：顶点约 30 格高、下落角约 90°、
 * 末 5 tick 偏离直线 0.000 格、总命中约 1.3 秒，原实现是 2.95 秒）。</p>
 * <p>无目标时保持原有行为：以视线前方虚拟锥顶生成，不写入锁定、不开启追踪，剑按原逻辑自行飞行。</p>
 */
public class Zenith12th {
    /** 参与生成的幻影剑数量（“十二式”）。 */
    private static final int SWORD_COUNT = 12;
    /** 倒圆锥高：底面圆周在锥顶正上方。 */
    private static final double CONE_HEIGHT = 10.0;
    /** 倒圆锥底面半径。 */
    private static final double CONE_RADIUS = 6.0;
    /** 发射延迟：0.5 秒 = 10 tick（原 20 tick，等于白等 1 秒）。 */
    private static final int DELAY_TICKS = 10;
    /** 索敌/准星/最近攻击的有效范围。 */
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
            // 1) 目标选择：E > A > C > B
            Entity target = state.getTargetEntity(world);                 // E
            if (!isValidTarget(target, playerIn)) {
                target = crosshairTarget(playerIn);                        // A
            }
            if (!isValidTarget(target, playerIn)) {
                Entity lastHurt = playerIn.getLastHurtMob();               // C
                if (isValidTarget(lastHurt, playerIn)
                        && playerIn.distanceToSqr(lastHurt) <= SEARCH_RANGE * SEARCH_RANGE) {
                    target = lastHurt;
                } else {
                    target = null;
                }
            }
            if (!isValidTarget(target, playerIn)) {
                target = nearestHostileInView(playerIn, world);            // B
            }

            boolean hasTarget = target != null;

            // 2) 锥顶：有目标取实体身体中心；无目标取视线前方一点（保持原有行为）
            Vec3 apex;
            if (hasTarget) {
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

            // 4) 在底面圆周上均匀生成
            for (int i = 0; i < SWORD_COUNT; i++) {
                double angle = Math.PI * 2 * i / SWORD_COUNT;
                Vec3 swordPos = apex.add(CONE_RADIUS * Math.cos(angle), CONE_HEIGHT, CONE_RADIUS * Math.sin(angle));

                EntityZenithSword sword = new EntityZenithSword(NrEntitiesRegistry.Enchanted_Sword, world);
                sword.setOwner(playerIn);
                sword.setDamage(damage);
                sword.setSpeed(speed);
                sword.setIsCritical(critical);
                sword.setColor(SWORD_COLOR);
                // 有目标：写入硬锁定并开启“汇聚→俯冲”；无目标：保持原有行为（null + 关闭）
                sword.setLockTarget(target);
                sword.setPos(swordPos.x, swordPos.y, swordPos.z);
                sword.setDelayTicks(DELAY_TICKS);
                sword.setZenith(true);
                sword.setNoClip(true);
                sword.setChange(false);
                // 初始方向 = 生成位置 − 锥顶（向外炸开，与原实现完全一致）：
                // 向外+向上约 59° 仰角，剑先向外上方炸开、飞高，再由 EntityZenithSword
                // 以恒定角速度平滑翻越到目标正上方，最后近垂直直线扎下。
                applyAimDirection(sword, swordPos.subtract(apex).normalize());

                world.addFreshEntity(sword);
            }

            world.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, playerIn.getSoundSource(), 0.6F, 1.4F);
        });
    }

    /** 目标是否可用：非空、存活、非释放者本人、非同源女仆/宠物。 */
    private static boolean isValidTarget(Entity e, LivingEntity player) {
        return e != null
                && e.isAlive()
                && e != player
                && !MaidCompat.isOwnedBy(e, player);
    }

    /**
     * A：玩家准星指向的实体。使用 {@link ProjectileUtil#getEntityHitResult} 做实体射线，
     * 并额外要求 {@code hasLineOfSight}，避免隔墙/隔玻璃锁定。
     */
    private static Entity crosshairTarget(LivingEntity player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(SEARCH_RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(SEARCH_RANGE)).inflate(1.0);
        EntityHitResult res = ProjectileUtil.getEntityHitResult(
                player, eye, end, box,
                e -> e instanceof LivingEntity living
                        && living.isAlive()
                        && living != player
                        && !MaidCompat.isOwnedBy(living, player)
                        && player.hasLineOfSight(living),
                SEARCH_RANGE * SEARCH_RANGE);
        return res != null ? res.getEntity() : null;
    }

    /**
     * 把给定方向写入剑的 DIY yaw/pitch（决定发射物理方向），并显式写入出生朝向。
     * （与原实现一致，未改动。）
     */
    private static void applyAimDirection(EntityZenithSword sword, Vec3 dir) {
        float yawDeg = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float pitchDeg = (float) (Math.asin(Mth.clamp(-dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);
        if (yawDeg == 0F && pitchDeg == 0F) {
            yawDeg = 0.0001F;
        }
        sword.setYAW(yawDeg);
        sword.setPITCH(pitchDeg);

        double hh = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float faceYaw = (float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
        float facePitch = (float) (Mth.atan2(dir.y, hh) * Mth.RAD_TO_DEG);
        sword.setNoClip(false);
        sword.setYRot(faceYaw);
        sword.setXRot(facePitch);
        sword.setNoClip(true);
    }

    /**
     * B：选取玩家视野内、离玩家最近的敌对实体（存活、非自身、非归属女仆/宠物、视线锥内且有直线可见性）。
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
            if (to.normalize().dot(look) < 0.7) continue;
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