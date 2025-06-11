package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;

import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

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
    protected float trackingRange = 8.0f;
    protected LivingEntity dynamicTarget;
    protected boolean useSmartTracking = true; // 是否启用智能追踪

    // 新增参数：追踪精度控制
    protected double directTrackingPrecision = 0.98; // 0.95-1.0之间，越高越精确
    protected double maxPredictionDistance = 10.0; // 最大预测距离

    //附魔影响
    private int smiteLevel;
    private int sharpnessLevel;
    private int baneLevel;

    // 新增：当前命中的主实体缓存
    private Entity currentMainTarget = null;

    // 新增：目标搜索冷却机制
    private static final int SEARCH_COOLDOWN = 5; // 每5tick搜索一次
    private int searchCooldown = 0;
    private boolean isDiscarded = false; // 防止重复处理

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

    // 新增：通用多部分实体处理
    private Entity getMainEntity(Entity entity) {
        // 处理末影龙类多部分实体
        if (entity instanceof EnderDragonPart part) {
            return part.getParent();
        }

        // 处理其他可能的多部分实体
        if (entity != null) {
            // 检查实体是否有"parent"字段
            try {
                Field parentField = entity.getClass().getDeclaredField("parent");
                parentField.setAccessible(true);
                Object parent = ((java.lang.reflect.Field) parentField).get(entity);
                if (parent instanceof Entity) {
                    return (Entity) parent;
                }
            } catch (NoSuchFieldException e) {
                // 没有parent字段是正常的
            } catch (Exception e) {
                // 忽略其他异常
            }

            // 检查实体是否有"getParent"方法
            try {
                Method getParent = entity.getClass().getMethod("getParent");
                Object parent = getParent.invoke(entity);
                if (parent instanceof Entity) {
                    return (Entity) parent;
                }
            } catch (NoSuchMethodException e) {
                // 没有getParent方法是正常的
            } catch (Exception e) {
                // 忽略其他异常
            }
        }

        return entity;
    }

    public Optional<Entity> getLockTarget() {
        int targetId = this.entityData.get(LOCK_TARGET_ID);
        if (targetId == -1) {
            return Optional.empty();
        }
        Entity target = this.level().getEntity(targetId);

        // 如果是多部分实体，获取其主实体
        if (target != null) {
            Entity mainEntity = getMainEntity(target);
            return Optional.ofNullable(mainEntity);
        }
        return Optional.ofNullable(target);
    }

    public void enableSmartTracking(boolean enable) {
        this.useSmartTracking = enable;
    }

    @Override
    public void tick() {

        if (isDiscarded) return; // 防止重复处理

        // 重置当前目标缓存
        currentMainTarget = null;

        if (!this.hasInitialized) {
            initializeEntity();
            this.prevPos = this.position();
        }

        if (!this.level().isClientSide && --this.lifeTime <= 0) {
            this.discard();
            return;
        }

        if (!this.itFired()) {
            if (this.getOwner() == null || !this.getOwner().isAlive()) {
                this.discard();
                return;
            }
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
        // 新增：提取主手刀附魔
        if (!this.level().isClientSide) {
            extractEnchantments();
        }
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
        // 1. 优先处理锁定目标
        Optional<Entity> lockedTarget = getLockTarget().filter(Entity::isAlive);
        if (lockedTarget.isPresent()) {
            Entity target = lockedTarget.get();
            directSteerToTarget(target);
            if (checkDirectHit(target)) {
                return; // 命中后直接返回
            }
        }

        // 2. 处理动态目标
        if (dynamicTarget != null) {
            if (!dynamicTarget.isAlive()) {
                dynamicTarget = null;
            } else {
                directSteerToTarget(dynamicTarget);
                if (checkDirectHit(dynamicTarget)) {
                    return; // 命中后直接返回
                }
            }
        }

        // 3. 目标搜索（带冷却机制）
        if (dynamicTarget == null && searchCooldown <= 0) {
            searchNearbyTargets();
            searchCooldown = SEARCH_COOLDOWN;
        } else {
            searchCooldown--;
        }
    }

    protected void handleLegacyTracking() {
        Optional<Entity> targetOpt = getLockTarget();

        if (targetOpt.isPresent()) {
            Entity target = targetOpt.get();
            directSteerToTarget(target); // 使用直接追踪
            checkDirectHit(target);
        }
    }

    protected Vec3 calculateTargetPosition(Entity target) {
        // 默认实现：目标中心点
        return target.position().add(0, target.getBbHeight() * 0.5, 0);
    }

    protected void searchNearbyTargets() {
        AABB area = new AABB(
                getX() - trackingRange, getY() - trackingRange, getZ() - trackingRange,
                getX() + trackingRange, getY() + trackingRange, getZ() + trackingRange
        );

        // 使用更高效的目标选择算法
        LivingEntity bestTarget = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity entity : level().getEntities(this, area)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!(entity instanceof Enemy)) continue;
            if (!living.isAlive()) continue;
            if (isEntityPart(living)) continue;
            if (getOwner() instanceof LivingEntity owner &&
                    !TargetSelector.lockon.test(owner, living)) continue;

            double distSq = distanceToSqr(living);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                bestTarget = living;
            }
        }

        dynamicTarget = bestTarget;
    }

    // 新增：判断是否为部分实体
    private boolean isEntityPart(Entity entity) {
        // 末影龙部分
        if (entity instanceof EnderDragonPart) {
            return true;
        }
        // 其他可能的部分实体
        String className = entity.getClass().getName().toLowerCase();
        return className.contains("part") ||
                className.contains("segment") ||
                className.contains("section");
    }

    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();

        // 获取主实体
        Entity mainEntity = getMainEntity(target);

        // 如果已经处理过这个主实体，跳过
        if (currentMainTarget != null && currentMainTarget.equals(mainEntity)) {
            return;
        }

        // 记录当前处理的主实体
        currentMainTarget = mainEntity;

        // 应用伤害和效果
        applyHitEffects(mainEntity);

        // 标记已处理，然后丢弃飞剑
        this.discard();
    }


    // 新增：应用命中效果
    private void applyHitEffects(Entity mainEntity) {
        if (mainEntity instanceof LivingEntity livingTarget) {
            float baseDamage = (float) this.getDamage();
            float finalDamage = calculateEnchantedDamage(baseDamage, livingTarget);

            // 设置伤害值
            this.setDamage(finalDamage);

            // 应用击退和眩晕效果
            if (livingTarget instanceof LivingEntity) {
                KnockBacks.cancel.action.accept(livingTarget);
                StunManager.setStun(livingTarget);
            }

            // 调用父类处理（确保声音、粒子等效果）
            super.onHitEntity(new EntityHitResult(mainEntity));
        }
    }

    // 新增方法：计算附魔增伤
    private float calculateEnchantedDamage(float baseDamage, Entity target1) {
        float damage = baseDamage;

        if (target1 instanceof LivingEntity target) {
            // 获取目标生物类型
            MobType mobType = target.getMobType();

            // 应用锋利附魔（对所有生物有效）
            damage += this.sharpnessLevel * 1.25F; // 每级增加1.25点伤害（2.5颗心）

            // 应用特定生物类型增伤（互斥，只取最高值）
            float typeBonus = 0;
            if (mobType == MobType.UNDEAD && this.smiteLevel > 0) {
                typeBonus = this.smiteLevel * 2.5F; // 每级增加2.5点伤害（5颗心）
            }
            else if (mobType == MobType.ARTHROPOD && this.baneLevel > 0) {
                typeBonus = this.baneLevel * 2.5F; // 每级增加2.5点伤害（5颗心）
            }
            // 应用类型增伤（与锋利叠加）
            damage += typeBonus;
        }
        return damage;
    }

    // 方块命中处理（完全继承父类逻辑）
    protected void onHitBlock(BlockHitResult result) {
        // 调用父类处理（播放音效、清除状态、销毁飞剑）
        super.onHitBlock(result);
    }

    // 直接命中检测（更精确的碰撞检测）
    protected boolean checkDirectHit(Entity target) {
        if (isDiscarded) return true; // 已丢弃则不再检测
        if (isEntityPart(target)) return false;

        Entity mainEntity = getMainEntity(target);
        if (currentMainTarget != null && currentMainTarget.equals(mainEntity)) {
            return false; // 已处理过该主实体
        }

        // 使用更精确的碰撞检测
        AABB targetBB = mainEntity.getBoundingBox().inflate(0.3);
        Vec3 currentPos = position();
        Vec3 nextPos = currentPos.add(getDeltaMovement());

        // 1. 检查当前帧碰撞
        if (targetBB.intersects(this.getBoundingBox())) {
            applyHitEffects(mainEntity);
            discardSafely();
            return true;
        }

        // 2. 检查运动轨迹碰撞
        Optional<Vec3> hitPos = targetBB.clip(currentPos, nextPos);
        if (hitPos.isPresent()) {
            applyHitEffects(mainEntity);
            discardSafely();
            return true;
        }

        return false;
    }

    // 安全丢弃方法（防止重复处理）
    private void discardSafely() {
        isDiscarded = true;
        this.discard();
    }

    public static void spawnSwords(LivingEntity owner, Level world, Vec3 centerPos, SpawnMode mode, int count,
                                   boolean change , float diyYaw, float diyPitch, float zj , float k,
                                   double damage, int colorCode, boolean clip ,int delay) {

        // 在生成飞剑时检查所有者是否有效
        if (owner == null || !owner.isAlive()) {
            return;
        }

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
                // 修改为在5×5×5范围内随机生成
                double halfSize = 2.5; // 5/2=2.5
                double x = centerPos.x + (world.random.nextDouble() - 0.5) * 5.0; // [-2.5, 2.5] 范围
                double y = centerPos.y + world.random.nextDouble() * 5.0; // [0, 5] 范围
                double z = centerPos.z + (world.random.nextDouble() - 0.5) * 5.0; // [-2.5, 2.5] 范围
                sword.setPos(x, y, z);
            } else if(mode == SpawnMode.CIRCLE){
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

    private void extractEnchantments() {
        LivingEntity owner = (LivingEntity) this.getOwner();
        if (owner == null) return;

        ItemStack mainHand = owner.getMainHandItem();
        if (mainHand.isEmpty()) return;

        // 获取所有附魔
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(mainHand);

        // 提取特定附魔等级
        this.sharpnessLevel = enchants.getOrDefault(Enchantments.SHARPNESS, 0);
        this.smiteLevel = enchants.getOrDefault(Enchantments.SMITE, 0);
        this.baneLevel = enchants.getOrDefault(Enchantments.BANE_OF_ARTHROPODS, 0);
    }
    //CIRCLE: 环形 RANDOM:随机 CONE:圆锥
    public enum SpawnMode { CIRCLE, RANDOM }
}
