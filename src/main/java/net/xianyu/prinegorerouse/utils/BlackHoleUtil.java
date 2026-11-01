package net.xianyu.prinegorerouse.utils;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.xianyu.prinegorerouse.compat.MaidCompat;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 黑洞（Eternity 体系）公共工具：层数读写、统一索敌、可视化观察名单。
 * <p>索敌优先级：玩家重锋锁定单位 → 准星方向最优者（有视线）→ 最近敌对生物。</p>
 */
public class BlackHoleUtil {
    public static final String BLACK_HOLE_TAG = "blackhole";
    /** 层数上限（"层满"阈值，可视化与叠层共用）。 */
    public static final int MAX_STACKS = 10;

    /**
     * 做成服务端可视化观察名单：UUID→实体弱引用。叠层时登记；{@code Eternity} 的 tick 定期扫描广播粒子，
     * 并对（死亡/离世/层数归零/被回收）条目做清理。使用弱引用避免强引用导致世界实例无法被 GC。
     */
    public static final Map<UUID, WeakReference<Entity>> WATCHED = new ConcurrentHashMap<>();

    private BlackHoleUtil() {
    }

    // 优化：单次获取Tag，减少重复IO操作
    private static CompoundTag getPersistentTag(Entity entity) {
        return entity.getPersistentData();
    }

    public static int getBlackHoleCount(Entity entity) {
        CompoundTag tag = getPersistentTag(entity);
        return tag.getInt(BLACK_HOLE_TAG);
    }

    /** 叠加层数（封顶 {@link #MAX_STACKS}），并把实体弱引用登记进可视化观察名单。 */
    public static void addBlackHoleCount(Entity entity, int amount) {
        CompoundTag tag = getPersistentTag(entity);
        int count = tag.getInt(BLACK_HOLE_TAG);
        tag.putInt(BLACK_HOLE_TAG, Math.min(count + amount, MAX_STACKS));
        WATCHED.put(entity.getUUID(), new WeakReference<>(entity));
    }

    public static void resetBlackHoleCount(Entity entity) {
        CompoundTag tag = getPersistentTag(entity);
        tag.putInt(BLACK_HOLE_TAG, 0);
        WATCHED.remove(entity.getUUID());
    }

    public static void clear() {
        WATCHED.clear();
    }

    /** 把多段生物（末影龙部件等）归并到本体。 */
    public static Entity mergeToMain(Entity e) {
        if (e instanceof PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent != null) {
                return parent;
            }
        }
        return e;
    }

    /**
     * 统一索敌（服务端）：
     * <ol>
     *   <li><b>重锋锁定单位</b>：刀状态 {@link ISlashBladeState#getTargetEntity(Level)}（可为任意生物，含玩家）；</li>
     *   <li><b>准星方向最优</b>：32 格内 {@code Enemy}，要求与视线夹角 dot&gt;0.25 且有直线视线，
     *       按 {@code dot×2 − 距离/32} 打分取最高；</li>
     *   <li>兜底：32 格内<b>最近</b>敌对生物（保持旧行为，不至于空放）。</li>
     * </ol>
     *
     * @param state 释放者手持刀的状态（可空，空则跳过锁定优先）
     * @return 目标生物；无任何候选返回 {@code null}
     */
    public static LivingEntity selectTarget(LivingEntity attacker, ISlashBladeState state) {
        Level level = attacker.level();
        if (level.isClientSide()) {
            return null;
        }

        // 1) 重锋锁定单位（最优先，含视线外——锁定即意图）；女仆/宠物保护：排除攻击者自己的驯服生物
        if (state != null) {
            Entity locked = mergeToMainNullSafe(state.getTargetEntity(level));
            if (locked instanceof LivingEntity le && le != attacker && le.isAlive() && !le.isSpectator()
                    && !MaidCompat.isOwnedBy(le, attacker)) {
                return le;
            }
        }

        // 2) 32 格候选池：准星方向最优（需有视线） → 3) 兜底最近
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getLookAngle();
        AABB box = attacker.getBoundingBox().inflate(32);
        List<LivingEntity> cands = level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e != attacker && e.isAlive() && e instanceof Enemy && !e.isSpectator());

        LivingEntity bestAim = null;
        double bestScore = -Double.MAX_VALUE;
        LivingEntity nearest = null;
        double nearestD2 = Double.MAX_VALUE;
        for (LivingEntity e : cands) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double d = to.length();
            if (d < 1.0E-3) {
                continue;
            }
            double dot = to.dot(look) / d;
            if (dot > 0.25 && attacker.hasLineOfSight(e)) {
                double score = dot * 2.0 - d / 32.0;
                if (score > bestScore) {
                    bestScore = score;
                    bestAim = e;
                }
            }
            double d2 = eye.distanceToSqr(e.position());
            if (d2 < nearestD2) {
                nearestD2 = d2;
                nearest = e;
            }
        }
        return bestAim != null ? bestAim : nearest;
    }

    /** 便捷重载：释放者是玩家但无刀状态上下文时（不启用锁定优先）。 */
    public static LivingEntity selectTarget(LivingEntity attacker) {
        ISlashBladeState state = null;
        if (attacker instanceof Player p) {
            state = p.getMainHandItem().getCapability(
                    mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE).resolve().orElse(null);
        }
        return selectTarget(attacker, state);
    }

    private static Entity mergeToMainNullSafe(Entity e) {
        return e == null ? null : mergeToMain(e);
    }
}
