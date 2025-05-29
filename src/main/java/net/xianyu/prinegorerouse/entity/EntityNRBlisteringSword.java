package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;

import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Stream;

public class EntityNRBlisteringSword extends EntityAbstractSummonedSword {
    //数据同步参数（移除TRACKING）
    public static final EntityDataAccessor<Boolean> IT_FIRED = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> TARGET = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Vector3f> CENTER_POS = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Float> DIRECTION_YAW = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DIRECTION_PITCH = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DIY_YAW = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DIY_PITCH = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> LOCK_TARGET = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> LOCK_TARGET_ID = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DELAY_TICK = SynchedEntityData.defineId(EntityNRBlisteringSword.class,EntityDataSerializers.INT);

    public static final EntityDataAccessor<Boolean> IT_CHANGED = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);

    //运动控制参数
    private final float rotationAngle = 0.0f;
    private int delayTicks = 20;
    public SpawnMode spawnMode = SpawnMode.CIRCLE;
    private Vec3 prevPos = Vec3.ZERO;
    private float prevRotationAngle = 0.0f;
    public int lifeTime = 200;
    private boolean hasInitialized = false;

    //RANDOM模式专用
    private float initialYaw;
    private float initialPitch;

    //自定义参数
    private float diyYaw;
    private float diyPitch;
    private Entity lockTarget; // 当前锁定目标

    // 新增追踪参数配置
    protected float trackingRange = 30.0f;
    protected LivingEntity dynamicTarget;
    protected boolean useSmartTracking = true; // 是否启用智能追踪

    // 新增参数：追踪精度控制
    protected double directTrackingPrecision = 0.98; // 0.95-1.0之间，越高越精确
    protected double maxPredictionDistance = 10.0; // 最大预测距离

    //附魔影响
    private int smiteLevel;
    private int sharpnessLevel;
    private int baneLevel;

    public EntityNRBlisteringSword(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
        this.setPierce((byte) 5);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IT_FIRED, false);
        this.entityData.define(TARGET, Optional.empty());
        this.entityData.define(SPEED, 2.0F);
        this.entityData.define(CENTER_POS, Vec3.ZERO.toVector3f());
        this.entityData.define(DIRECTION_YAW, 0.0f);
        this.entityData.define(DIRECTION_PITCH, 0.0f);
        this.entityData.define(IT_CHANGED, true);
        this.entityData.define(DIY_YAW, 0F);
        this.entityData.define(DIY_PITCH, 0F);
        this.entityData.define(LOCK_TARGET, Optional.empty());
        this.entityData.define(LOCK_TARGET_ID, -1); // -1 表示无目标
        this.entityData.define(DELAY_TICK, delayTicks);
    }

    public static EntityNRBlisteringSword createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, worldIn);
    }

    public void doFire() {
        this.getEntityData().set(IT_FIRED, true);
    }

    public boolean itFired() {
        return this.entityData.get(IT_FIRED);
    }

    public void setChange(boolean change) {
        this.getEntityData().set(IT_CHANGED, change);
    }

    public void doChange() {
        this.getEntityData().set(IT_CHANGED, true);
    }

    public boolean itChanged() {
        return this.entityData.get(IT_CHANGED);
    }

    public void setSpeed(float speed) {
        this.getEntityData().set(SPEED, speed);
    }

    public float getSpeed() {
        return this.entityData.get(SPEED);
    }

    public int getDelayTicks() {
        return this.entityData.get(DELAY_TICK);
    }

    public void setDelayTicks(int delayTick) {
        this.getEntityData().set(DELAY_TICK, delayTick);
    }

    public void setCenterPosition(Vec3 pos) {
        this.entityData.set(CENTER_POS, pos.toVector3f());
    }

    //新增自定义
    //绕y轴旋转
    public float getDiyYaw() {
        return this.entityData.get(DIY_YAW);
    }

    public void setYAW(Float diyYaw) {
        this.entityData.set(DIY_YAW, diyYaw);
    }
    //绕x轴旋转
    public float getDiyPitch() {
        return this.entityData.get(DIY_PITCH);
    }

    public void setPITCH(Float diyPitch) {
        this.entityData.set(DIY_PITCH, diyPitch);
    }

    public Optional<Entity> getLockTarget() {
        int targetId = this.entityData.get(LOCK_TARGET_ID);
        if (targetId == -1) {
            return Optional.empty();
        }
        Entity target = this.level().getEntity(targetId);
        return Optional.ofNullable(target);
    }

    public void enableSmartTracking(boolean enable) {
        this.useSmartTracking = enable;
    }

    @Override
    public void tick() {
        if (!this.hasInitialized) {
            initializeEntity();
            this.prevPos = this.position();
        }

        if (!this.level().isClientSide && --this.lifeTime <= 0) {
            this.discard();
            return;
        }

        if (!this.itFired()) {
            handleDelayPhase();
        } else {
            handleFlyingPhase();
            super.tick();
        }

    }

    private void initializeEntity() {
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.delayTicks = this.getDelayTicks();
        if (this.getOwner() != null) {
            LivingEntity owner = (LivingEntity) this.getOwner();
            if (getDiyYaw() == 0 && getDiyPitch() ==0) {
                // 始终记录初始方向
                if (spawnMode == SpawnMode.RANDOM) {
                    this.initialYaw = owner.getYHeadRot();
                    this.initialPitch = owner.getXRot();
                } else {
                    this.initialYaw = -owner.getYHeadRot();
                    this.initialPitch = -owner.getXRot();
                }
                this.setDeltaMovement(this.getLookAngle());
                if (!itChanged()) {
                    this.setYRot(initialYaw);
                    this.setXRot(initialPitch);
                }
                this.entityData.set(DIRECTION_YAW, initialYaw);
                this.entityData.set(DIRECTION_PITCH, initialPitch);
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
            } else {
                this.initialYaw = getDiyYaw();
                this.initialPitch = getDiyPitch();
                this.setDeltaMovement(this.getLookAngle());
                if (!itChanged()) {
                    this.setYRot(initialYaw);
                    this.setXRot(initialPitch);
                }
                this.entityData.set(DIRECTION_YAW, initialYaw);
                this.entityData.set(DIRECTION_PITCH, initialPitch);
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
            }
        }
        this.hasInitialized = true;
    }

    private void handleDelayPhase() {
        if (this.level().isClientSide) {
            this.prevPos = this.position();
            this.prevRotationAngle = this.rotationAngle;
        }

        if (!this.level().isClientSide) {
            if (this.delayTicks > 0) {
                this.delayTicks--;
                if (spawnMode == SpawnMode.RANDOM) {
                    if (!itChanged()) {
                        // IT_CHANGED为false时保持初始设定方向
                        this.setYRot(-initialYaw);
                        this.setXRot(-initialPitch);
                        adjustInitialDirection();
                    } else {
                        // IT_CHANGED为true时跟随玩家视角
                        adjustInitialDirection();
                    }
                }
            } else {
                launchProjectile();
                this.entityData.set(IT_FIRED, true);
            }
        }

        if (this.level().isClientSide) {
            adjustInitialDirection();
        }
    }

    private void adjustInitialDirection() {
        if (itChanged()) {
            LivingEntity owner = (LivingEntity) this.getOwner();
            if (owner == null) return;
            this.initialYaw = -owner.getYHeadRot();
            this.initialPitch = -owner.getXRot();
            this.setYRot(initialYaw);
            this.setXRot(initialPitch);
        }
    }

    private void launchProjectile() {
        if (this.getOwner() == null) {
            this.discard();
            return;
        }
        switch (spawnMode) {
            case CIRCLE -> {
                if (itChanged()) {
                    float currentYaw = this.getOwner().getYHeadRot();
                    float currentPitch = this.getOwner().getXRot();

                    float yawRad = currentYaw * Mth.DEG_TO_RAD;
                    float pitchRad = currentPitch * Mth.DEG_TO_RAD;

                    double xzLen = Math.cos(pitchRad);
                    double x = -Math.sin(yawRad) * xzLen;
                    double y = -Math.sin(pitchRad);
                    double z = Math.cos(yawRad) * xzLen;

                    this.shoot(x, y, z, this.getSpeed(), 0);
                } else {
                    float yawRad = -initialYaw * Mth.DEG_TO_RAD;
                    float pitchRad = -initialPitch * Mth.DEG_TO_RAD;
                    double xzLen = Math.cos(pitchRad);
                    double x = -Math.sin(yawRad) * xzLen;
                    double y = -Math.sin(pitchRad);
                    double z = Math.cos(yawRad) * xzLen;
                    this.shoot(x, y, z, this.getSpeed(), 0);
                }
            }
            case RANDOM -> {
                if (itChanged()) {
                    float currentYaw = this.getOwner().getYHeadRot();
                    float currentPitch = this.getOwner().getXRot();

                    float yawRad = currentYaw * Mth.DEG_TO_RAD;
                    float pitchRad = currentPitch * Mth.DEG_TO_RAD;

                    double xzLen = Math.cos(pitchRad);
                    double x = -Math.sin(yawRad) * xzLen;
                    double y = -Math.sin(pitchRad);
                    double z = Math.cos(yawRad) * xzLen;

                    this.shoot(x, y, z, this.getSpeed(), 0);
                } else {
                    float yawRad = initialYaw * Mth.DEG_TO_RAD;
                    float pitchRad = initialPitch * Mth.DEG_TO_RAD;
                    double xzLen = Math.cos(pitchRad);
                    double x = -Math.sin(yawRad) * xzLen;
                    double y = -Math.sin(pitchRad);
                    double z = Math.cos(yawRad) * xzLen;
                    this.shoot(x, y, z, this.getSpeed(), 0);
                }
            }
        }
    }

    //飞行阶段处理
    private void handleFlyingPhase() {
        if (useSmartTracking) {
            handleSmartTracking();
        } else {
            handleLegacyTracking(); // 保留旧版追踪逻辑
        }
        super.tick();
    }

    // 新增智能追踪逻辑
    protected void handleSmartTracking() {
        acquireAndTrackTarget();
    }


    protected void handleLegacyTracking() {
        Optional<Entity> targetOpt = getLockTarget();

        if (targetOpt.isPresent()) {
            Entity target = targetOpt.get();
            directSteerToTarget(target); // 使用直接追踪
            checkDirectHit(target);
        }
    }

    // 目标获取与追踪（核心逻辑）
    protected void acquireAndTrackTarget() {
        // 优先级1：锁定目标
        Optional<Entity> lockedTarget = getLockTarget().filter(Entity::isAlive);
        if (lockedTarget.isPresent()) {
            Entity target = lockedTarget.get();
            directSteerToTarget(target); // 使用直接追踪
            checkDirectHit(target);
            return;
        }


        // 优先级2：射线检测目标
        if (dynamicTarget != null) {
            trackDynamicTargetDirect();
        }

        // 优先级3：动态搜索目标
        if (dynamicTarget == null || !dynamicTarget.isAlive()) {
            searchNearbyTargets();
        }
    }

    protected Vec3 calculateTargetPosition(Entity target) {
        // 默认实现：目标中心点
        return target.position().add(0, target.getBbHeight() * 0.5, 0);
    }

    // 直接动态目标追踪
    private void trackDynamicTargetDirect() {
        directSteerToTarget(dynamicTarget);
        checkDirectHit(dynamicTarget);
    }

    protected void searchNearbyTargets() {
        AABB area = new AABB(
                getX() - trackingRange, getY() - trackingRange, getZ() - trackingRange,
                getX() + trackingRange, getY() + trackingRange, getZ() + trackingRange
        );

        List<LivingEntity> candidates = level().getEntitiesOfClass(LivingEntity.class, area, e ->
                e instanceof Enemy &&
                        e.isAlive() &&
                        TargetSelector.lockon.test((LivingEntity)getOwner(), e)
        );

        if (!candidates.isEmpty()) {
            dynamicTarget = candidates.get(0);
        }
    }

    protected void onTargetReached(Entity target) {
        EntityHitResult hitResult = new EntityHitResult(target);
        this.onHitEntity(hitResult);
        this.discard();
    }

    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (target instanceof LivingEntity) {
            KnockBacks.cancel.action.accept((LivingEntity) target);
            StunManager.setStun((LivingEntity) target);
        }
        super.onHitEntity(result);
    }

    // 方块命中处理（完全继承父类逻辑）
    protected void onHitBlock(BlockHitResult result) {
        // 调用父类处理（播放音效、清除状态、销毁飞剑）
        super.onHitBlock(result);
    }

    // 直接命中检测（更精确的碰撞检测）
    protected void checkDirectHit(Entity target) {
        // 使用精确的AABB碰撞检测
        AABB targetBB = target.getBoundingBox().inflate(0.3);
        AABB swordBB = this.getBoundingBox();

        if (targetBB.intersects(swordBB)) {
            // 立即触发命中效果
            onTargetReached(target);
            return;
        }

        // 额外检查：高速穿透情况
        Vec3 nextPos = position().add(getDeltaMovement());
        Optional<Vec3> hitPos = targetBB.clip(position(), nextPos);
        if (hitPos.isPresent()) {
            onTargetReached(target);
        }
    }

    private Vec3 calculateHitResultPosition() {
        // 合并锁定目标和射线检测结果
        Optional<Entity> foundTarget = Stream.of(
                        Optional.ofNullable(getLockTarget().orElse(null)), // 获取当前锁定目标
                        RayTraceHelper.rayTrace(
                                        this.level(),
                                        this, // 发射者为飞剑自身
                                        this.getEyePosition(1.0F),
                                        this.getLookAngle(),
                                        12.0, // 最大距离
                                        12.0, // 检测半径
                                        e -> {
                                            // 过滤条件：排除发射者、不可攻击目标
                                            if (e == this.getOwner()) return false;
                                            if (e instanceof IShootable) {
                                                return ((IShootable) e).getShooter() != this.getOwner();
                                            }
                                            if (e instanceof LivingEntity) {
                                                return TargetSelector.lockon.test((LivingEntity) this.getOwner(), (LivingEntity) e);
                                            }
                                            return true;
                                        }
                                ).filter(r -> r.getType() == HitResult.Type.ENTITY)
                                .map(r -> ((EntityHitResult) r).getEntity())
                ).filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        // 计算最终目标位置
        return foundTarget.map(e ->
                new Vec3(e.getX(), e.getY() + e.getEyeHeight() * 0.8, e.getZ())
        ).orElseGet(() -> {
            // 无目标时检测方块碰撞点
            Vec3 start = this.getEyePosition(1.0F);
            Vec3 end = start.add(this.getLookAngle().scale(40.0));
            ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
            BlockHitResult blockResult = this.level().clip(context);
            return blockResult.getLocation();
        });
    }

    public static void spawnSwords(LivingEntity owner, Level world, Vec3 centerPos, SpawnMode mode, int count,
                                   boolean change , float diyYaw, float diyPitch, float zj , float k,
                                   double damage, int colorCode, boolean clip ,int delay) {
        for (int i = 0; i < count; i++) {
            EntityNRBlisteringSword sword = new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, world);
            if (diyPitch != 0 && diyYaw != 0) {
                sword.setYAW(diyYaw + k * i + zj);
                sword.setPITCH(diyPitch + k * i + zj);
            }
            sword.setOwner(owner);
            sword.setCenterPosition(centerPos);
            sword.spawnMode = mode;
            sword.lifeTime = 200;
            sword.setChange(change);
            sword.setDamage(damage);
            sword.setColor(colorCode);
            sword.setNoClip(clip);
            sword.setDelayTicks(delay);


            if (mode == SpawnMode.RANDOM) {
                Vec3 initPos = calculateInitialRandomPos(centerPos);
                sword.setPos(initPos);
            } else {
                double radius = 3.0;
                double angle = Math.PI * 2 * i / count;
                Vec3 pos = centerPos.add(
                        radius * Math.cos(angle), 0, radius * Math.sin(angle));
                sword.setPos(pos.x, pos.y, pos.z);
            }
            if (diyPitch == 0 && diyYaw == 0) {
                sword.setYRot(-owner.getYHeadRot());
                sword.setXRot(-owner.getXRot());
            } else {
                sword.setYRot(-(sword.getDiyPitch() + k * i + zj));
                sword.setXRot(-(sword.getDiyYaw() + k * i + zj));
            }
            world.addFreshEntity(sword);
        }
    }

    private static Vec3 calculateInitialRandomPos(Vec3 center) {
        double radius = 1.5;
        double angle = Math.PI * 2 * Math.random();
        return center.add(
                radius * Math.cos(angle), 0, radius * Math.sin(angle));
    }


    // 直接追踪方法（智能/旧版追踪共用）
    protected void directSteerToTarget(Entity target) {
        Vec3 currentPos = position();
        Vec3 targetPos = calculateTargetPosition(target);
        Vec3 currentVelocity = getDeltaMovement();
        double currentSpeed = currentVelocity.length();

        // 1. 计算直接方向向量
        Vec3 directDirection = targetPos.subtract(currentPos).normalize();

        // 2. 高级轨迹预测（考虑目标移动速度）
        Vec3 targetVelocity = target.getDeltaMovement();
        double distance = currentPos.distanceTo(targetPos);

        // 计算预测时间（基于当前速度和目标速度）
        double timeToTarget = Math.min(distance / currentSpeed, 1.0);
        Vec3 predictedPos = targetPos.add(targetVelocity.scale(timeToTarget * 0.8));

        // 3. 混合当前方向和新方向（防止瞬间转向导致的视觉不适）
        Vec3 desiredDirection = predictedPos.subtract(currentPos).normalize();
        Vec3 newDirection = directDirection.scale(1.0 - directTrackingPrecision)
                .add(desiredDirection.scale(directTrackingPrecision))
                .normalize();

        // 4. 直接应用新方向
        Vec3 newVelocity = newDirection.scale(currentSpeed);
        setDeltaMovement(newVelocity);

        // 5. 立即更新实体朝向
        updateRotationFromVelocity(newVelocity);
    }

    // 立即从速度向量更新旋转角度
    private void updateRotationFromVelocity(Vec3 velocity) {
        double horizontalDistance = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float yaw = (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z));
        float pitch = (float) Math.toDegrees(Math.atan2(velocity.y, horizontalDistance));

        // 立即设置旋转角度（无插值）
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }
    public enum SpawnMode { CIRCLE, RANDOM }
}
