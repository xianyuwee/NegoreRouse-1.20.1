package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.BlackHoleUtil;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class EntityNRBlisteringSword extends EntityAbstractSummonedSword {
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

    private static final EntityDataAccessor<Boolean> USE_CUSTOM_DIRECTION = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AUTO_TARGETING = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);

    private final float rotationAngle = 0.0f;
    private int delayTicks = 20;
    public SpawnMode spawnMode = SpawnMode.CIRCLE;
    private Vec3 prevPos = Vec3.ZERO;
    private float prevRotationAngle = 0.0f;

    public int lifeTime = 200;
    private boolean hasInitialized = false;

    private float initialYaw;
    private float initialPitch;

    private float diyYaw;
    private float diyPitch;
    private Entity lockTarget;

    protected float trackingRange = 8.0f;
    protected LivingEntity dynamicTarget;
    protected boolean useSmartTracking = true;

    protected double directTrackingPrecision = 0.98;
    protected double maxPredictionDistance = 10.0;

    private int smiteLevel;
    private int sharpnessLevel;
    private int baneLevel;

    private Entity currentMainTarget = null;

    private static final int SEARCH_COOLDOWN = 5;
    private int searchCooldown = 0;
    private boolean isDiscarded = false;

    // ==============================================
    // 新增：delay阶段固定位置缓存
    // ==============================================
    private Vec3 fixedDelayPos = Vec3.ZERO;

    // ==============================================
    // 反射强制修改父类 private inGround 字段
    // ==============================================
    private static final Field IN_GROUND_FIELD;
    static {
        Field tmp = null;
        try {
            tmp = EntityAbstractSummonedSword.class.getDeclaredField("inGround");
            tmp.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        IN_GROUND_FIELD = tmp;
    }

    // ==============================================
    // 核心修复1：旋转锁，拦截父类的强制旋转/反向
    // ==============================================
    private boolean allowRotationUpdate = false;

    @Override
    public void setYRot(float yaw) {
        // 只有我们自己允许修改时，才更新旋转，彻底拦截父类的强制反向
        if (allowRotationUpdate || !isNoClip()) {
            super.setYRot(yaw);
            this.yRotO = yaw; // 同步旧值，防止插值混乱
        }
    }

    @Override
    public void setXRot(float pitch) {
        // 只有我们自己允许修改时，才更新旋转
        if (allowRotationUpdate || !isNoClip()) {
            super.setXRot(pitch);
            this.xRotO = pitch; // 同步旧值，防止插值混乱
        }
    }

    // 我们自己的旋转设置专用方法，解锁旋转锁
    private void setRotationSafe(float yaw, float pitch) {
        allowRotationUpdate = true;
        setYRot(yaw);
        setXRot(pitch);
        allowRotationUpdate = false;
    }

    public EntityNRBlisteringSword(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
        this.setPierce((byte) 5);
        this.setNoGravity(true);
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
        this.entityData.define(LOCK_TARGET_ID, -1);
        this.entityData.define(DELAY_TICK, delayTicks);
        this.entityData.define(USE_CUSTOM_DIRECTION, false);
        this.entityData.define(AUTO_TARGETING, false);
    }

    // ==============================================
    // 核心修复2：完全重写tick逻辑，分离delay和flying阶段
    // ==============================================
    @Override
    public void tick() {
        // 1. 先处理NoClip的inGround强制清除
        if (isNoClip() && IN_GROUND_FIELD != null) {
            try {
                IN_GROUND_FIELD.setBoolean(this, false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (isDiscarded) return;
        currentMainTarget = null;

        // 2. 初始化逻辑（只执行一次）
        if (!hasInitialized) {
            initializeEntity();
            prevPos = position();
            // 初始化时记录固定位置
            fixedDelayPos = position();
        }

        // 3. 生命周期控制
        if (!level().isClientSide && --lifeTime <= 0) {
            discard();
            return;
        }

        // 4. 分离处理：delay阶段和flying阶段完全分开
        if (!itFired()) {
            // ==============================================
            // 核心修复3：delay阶段完全跳过父类tick，只执行我们自己的逻辑
            // ==============================================
            if (getOwner() == null || !getOwner().isAlive()) {
                discard();
                return;
            }
            handleDelayPhase();
        } else {
            // ==============================================
            // flying阶段正常执行父类逻辑 + 我们自己的逻辑
            // ==============================================
            super.tick();
            handleFlyingPhase();
        }
    }

    // ==============================================
    // 核心修复4：NoClip时方块命中完全失效
    // ==============================================
    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (isNoClip()) {
            return;
        }
        super.onHitBlock(result);
    }

    // ==============================================
    // 注册修复：客户端工厂方法
    // ==============================================
    public static EntityNRBlisteringSword createInstance(PlayMessages.SpawnEntity packet, Level world) {
        return new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, world);
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

    public void setUseCustomDirection(boolean use) {
        this.entityData.set(USE_CUSTOM_DIRECTION, use);
    }

    public boolean useCustomDirection() {
        return this.entityData.get(USE_CUSTOM_DIRECTION);
    }

    public void setAutoTargeting(boolean auto) {
        this.entityData.set(AUTO_TARGETING, auto);
    }

    public boolean autoTargetingEnabled() {
        return this.entityData.get(AUTO_TARGETING);
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
        this.entityData.set(DELAY_TICK, delayTick);
    }

    public void setCenterPosition(Vec3 pos) {
        this.entityData.set(CENTER_POS, pos.toVector3f());
    }

    public float getDiyYaw() {
        return this.entityData.get(DIY_YAW);
    }

    public void setYAW(Float diyYaw) {
        this.entityData.set(DIY_YAW, diyYaw);
    }

    public float getDiyPitch() {
        return this.entityData.get(DIY_PITCH);
    }

    public void setPITCH(Float diyPitch) {
        this.entityData.set(DIY_PITCH, diyPitch);
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    private Entity getMainEntity(Entity entity) {
        if (entity instanceof EnderDragonPart part) {
            return part.getParent();
        }
        try {
            Field parentField = entity.getClass().getDeclaredField("parent");
            parentField.setAccessible(true);
            Object parent = parentField.get(entity);
            if (parent instanceof Entity) return (Entity) parent;
        } catch (Exception ignored) {}
        try {
            Method getParent = entity.getClass().getMethod("getParent");
            Object parent = getParent.invoke(entity);
            if (parent instanceof Entity) return (Entity) parent;
        } catch (Exception ignored) {}
        return entity;
    }

    public Optional<Entity> getLockTarget() {
        int targetId = this.entityData.get(LOCK_TARGET_ID);
        if (targetId == -1) return Optional.empty();
        Entity target = this.level().getEntity(targetId);
        if (target != null) {
            Entity main = getMainEntity(target);
            return Optional.ofNullable(main);
        }
        return Optional.empty();
    }

    public void enableSmartTracking(boolean enable) {
        this.useSmartTracking = enable;
    }

    private void initializeEntity() {
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.delayTicks = this.getDelayTicks();
        if (!this.level().isClientSide()) extractEnchantments();

        if (this.getOwner() != null) {
            LivingEntity owner = (LivingEntity) this.getOwner();
            if (getDiyYaw() == 0 && getDiyPitch() == 0) {
                if (spawnMode == SpawnMode.RANDOM) {
                    this.initialYaw = owner.getYHeadRot();
                    this.initialPitch = owner.getXRot();
                } else {
                    this.initialYaw = -owner.getYHeadRot();
                    this.initialPitch = -owner.getXRot();
                }
                this.setDeltaMovement(this.getLookAngle());
                if (!itChanged()) {
                    // 初始化用安全旋转方法
                    setRotationSafe(initialYaw, initialPitch);
                }
                this.entityData.set(DIRECTION_YAW, initialYaw);
                this.entityData.set(DIRECTION_PITCH, initialPitch);
            } else {
                this.initialYaw = getDiyYaw();
                this.initialPitch = getDiyPitch();
                double yawRad = initialYaw * Mth.DEG_TO_RAD;
                double pitchRad = initialPitch * Mth.DEG_TO_RAD;
                double xzLen = Math.cos(pitchRad);
                double x = -Math.sin(yawRad) * xzLen;
                double y = -Math.sin(pitchRad);
                double z = Math.cos(yawRad) * xzLen;
                Vec3 direction = new Vec3(x, y, z).normalize();
                this.setDeltaMovement(direction);
                if (!itChanged()) {
                    // 初始化用安全旋转方法
                    setRotationSafe(-initialYaw, -initialPitch);
                }
                this.entityData.set(DIRECTION_YAW, initialYaw);
                this.entityData.set(DIRECTION_PITCH, initialPitch);
            }
        }
        this.hasInitialized = true;
    }

    // ==============================================
    // 核心修复5：完全重写handleDelayPhase，强制固定位置，只更新旋转
    // ==============================================
    private void handleDelayPhase() {
        // 强制固定在初始位置，绝对不让它动
        this.setPos(fixedDelayPos.x, fixedDelayPos.y, fixedDelayPos.z);
        this.setDeltaMovement(Vec3.ZERO);

        if (this.level().isClientSide()) {
            this.prevPos = this.position();
            this.prevRotationAngle = this.rotationAngle;
        }

        if (!this.level().isClientSide()) {
            if (this.delayTicks > 0) {
                this.delayTicks--;
                if (spawnMode == SpawnMode.RANDOM) {
                    if (!itChanged()) {
                        // change=false：保持初始方向，不动
                        setRotationSafe(-initialYaw, -initialPitch);
                    } else {
                        // change=true：跟随玩家视角
                        adjustInitialDirection();
                    }
                }
            } else {
                // delay结束，发射
                launchProjectile();
                this.entityData.set(IT_FIRED, true);
            }
        }

        if (this.level().isClientSide()) {
            if (itChanged()) {
                adjustInitialDirection();
            }
        }
    }

    private void maintainCustomDirection() {
        // 自定义方向用安全旋转方法
        setRotationSafe(initialYaw, initialPitch);
    }

    private void adjustInitialDirection() {
        if (itChanged()) {
            LivingEntity owner = (LivingEntity) this.getOwner();
            if (owner == null) return;
            this.initialYaw = -owner.getYHeadRot();
            this.initialPitch = -owner.getXRot();
            // 跟随视角用安全旋转方法
            setRotationSafe(initialYaw, initialPitch);
        }
    }

// ==============================================
// 核心修复：完全重写 launchProjectile，确保 change=false 时方向正确
// ==============================================
    private void launchProjectile() {
        if (this.getOwner() == null) {
            this.discard();
            return;
        }

        Vec3 shootDir;
        float finalYaw;
        float finalPitch;

        if (itChanged()) {
            // change=true：使用当前玩家视角（保持之前的正确逻辑）
            LivingEntity owner = (LivingEntity) this.getOwner();
            float yaw = owner.getYHeadRot();
            float pitch = owner.getXRot();

            // 计算方向向量
            float yawRad = yaw * Mth.DEG_TO_RAD;
            float pitchRad = pitch * Mth.DEG_TO_RAD;
            double xzLen = Math.cos(pitchRad);
            double x = -Math.sin(yawRad) * xzLen;
            double y = -Math.sin(pitchRad);
            double z = Math.cos(yawRad) * xzLen;
            shootDir = new Vec3(x, y, z).normalize();

            // 设置旋转（和方向一致）
            finalYaw = -yaw;
            finalPitch = -pitch;
        } else {
            // ==============================================
            // 核心修复：change=false 时，直接用 initialYaw/initialPitch 计算，不再加负号
            // ==============================================
            float yawRad = initialYaw * Mth.DEG_TO_RAD;
            float pitchRad = initialPitch * Mth.DEG_TO_RAD;
            double xzLen = Math.cos(pitchRad);

            // 这里的计算要和 initializeEntity 里的 setDeltaMovement 完全一致
            double x = -Math.sin(yawRad) * xzLen;
            double y = -Math.sin(pitchRad);
            double z = Math.cos(yawRad) * xzLen;
            shootDir = new Vec3(x, y, z).normalize();

            // 旋转也直接用 initialYaw/initialPitch，和初始化时一致
            finalYaw = initialYaw;
            finalPitch = initialPitch;
        }

        // 应用方向和旋转
        this.shoot(shootDir.x, shootDir.y, shootDir.z, getSpeed(), 0);
        setRotationSafe(finalYaw, finalPitch);
    }

    private void handleFlyingPhase() {
        if (useCustomDirection()) {
            if (autoTargetingEnabled()) handleSmartTracking();
            else maintainCustomDirection();
        } else {
            if (useSmartTracking) handleSmartTracking();
            else handleLegacyTracking();
        }
    }

    protected void handleSmartTracking() {
        Optional<Entity> locked = getLockTarget().filter(Entity::isAlive);
        if (locked.isPresent()) {
            Entity t = locked.get();
            directSteerToTarget(t);
            if (checkDirectHit(t)) return;
        }
        if (dynamicTarget != null) {
            if (!dynamicTarget.isAlive()) dynamicTarget = null;
            else {
                directSteerToTarget(dynamicTarget);
                if (checkDirectHit(dynamicTarget)) return;
            }
        }
        if (dynamicTarget == null && searchCooldown <= 0) {
            searchNearbyTargets();
            searchCooldown = SEARCH_COOLDOWN;
        } else searchCooldown--;
    }

    protected void handleLegacyTracking() {
        Optional<Entity> opt = getLockTarget();
        if (opt.isPresent()) {
            Entity t = opt.get();
            directSteerToTarget(t);
            checkDirectHit(t);
        }
    }

    protected Vec3 calculateTargetPosition(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    protected void searchNearbyTargets() {
        AABB box = getBoundingBox().inflate(trackingRange);
        List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e instanceof Enemy && e.isAlive() && getOwner() instanceof LivingEntity p
                        && TargetSelector.lockon.test(p, e));
        LivingEntity best = null;
        double min = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = distanceToSqr(e);
            if (d < min) {
                min = d;
                best = e;
            }
        }
        dynamicTarget = best;
    }

    private boolean isEntityPart(Entity e) {
        return e instanceof EnderDragonPart || e.getClass().getName().matches(".*(part|segment|section).*");
    }

    @Override
    protected void onHitEntity(EntityHitResult res) {
        Entity t = getMainEntity(res.getEntity());
        if (currentMainTarget != null && currentMainTarget.equals(t)) return;
        currentMainTarget = t;
        applyHitEffects(t);
        discardSafely();
    }

    private void applyHitEffects(Entity e) {
        if (level().isClientSide()) return;
        if (e instanceof LivingEntity living) {
            float dmg = (float) getDamage();
            dmg += sharpnessLevel * 1.25f;
            if (living.getMobType() == MobType.UNDEAD) dmg += smiteLevel * 2.5f;
            else if (living.getMobType() == MobType.ARTHROPOD) dmg += baneLevel * 2.5f;
            setDamage(dmg);

            KnockBacks.cancel.action.accept(living);
            StunManager.setStun(living);
            BlackHoleUtil.addBlackHoleCount(living, 1);

            if (level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.FIREWORK, living.getX(), living.getY(1), living.getZ(), 1, 0,0,0,0);

            super.onHitEntity(new EntityHitResult(living));
        }
    }

    protected boolean checkDirectHit(Entity target) {
        if (isDiscarded || isEntityPart(target)) return false;
        Entity main = getMainEntity(target);
        if (currentMainTarget != null && currentMainTarget.equals(main)) return false;

        AABB box = main.getBoundingBox().inflate(0.3);
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        if (box.intersects(getBoundingBox()) || box.clip(from, to).isPresent()) {
            applyHitEffects(main);
            discardSafely();
            return true;
        }
        return false;
    }

    private void discardSafely() {
        isDiscarded = true;
        discard();
    }

    protected void directSteerToTarget(Entity target) {
        Vec3 pos = position();
        Vec3 tgt = calculateTargetPosition(target);
        Vec3 vel = getDeltaMovement();
        double spd = vel.length();

        Vec3 dir = tgt.subtract(pos).normalize();
        double dist = pos.distanceTo(tgt);
        double time = Math.min(dist / spd, 1);
        Vec3 pred = tgt.add(target.getDeltaMovement().scale(time * 0.8));
        Vec3 want = pred.subtract(pos).normalize();
        Vec3 fin = dir.scale(1 - directTrackingPrecision).add(want.scale(directTrackingPrecision)).normalize();

        setDeltaMovement(fin.scale(spd));
        updateRotationFromVelocity(fin);
    }

    // ==============================================
    // 核心修复7：旋转更新用安全方法，确保生效
    // ==============================================
    private void updateRotationFromVelocity(Vec3 vel) {
        double h = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        float y = Mth.wrapDegrees((float) Mth.atan2(vel.x, vel.z) * Mth.RAD_TO_DEG);
        float p = Mth.wrapDegrees((float) Mth.atan2(vel.y, h) * Mth.RAD_TO_DEG);
        // 用安全方法设置旋转，不会被父类拦截
        setRotationSafe(y, p);
    }

    private void extractEnchantments() {
        if (!(getOwner() instanceof LivingEntity p)) return;
        ItemStack stack = p.getMainHandItem();
        if (stack.isEmpty()) return;
        Map<Enchantment, Integer> ench = EnchantmentHelper.getEnchantments(stack);
        sharpnessLevel = ench.getOrDefault(Enchantments.SHARPNESS, 0);
        smiteLevel = ench.getOrDefault(Enchantments.SMITE, 0);
        baneLevel = ench.getOrDefault(Enchantments.BANE_OF_ARTHROPODS, 0);
    }

    public static void spawnSwords(LivingEntity owner, Level world, Vec3 center, SpawnMode mode, int count,
                                   boolean change, float y, float p, float zj, float k,
                                   double dmg, int color, boolean clip, int delay) {
        if (owner == null || !owner.isAlive()) return;
        for (int i = 0; i < count; i++) {
            EntityNRBlisteringSword sword = new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, world);
            sword.setOwner(owner);
            sword.setCenterPosition(center);
            sword.spawnMode = mode;
            sword.lifeTime = 200;
            sword.setChange(change);
            sword.setDamage(dmg);
            sword.setColor(color);
            sword.setDelayTicks(delay);
            sword.setNoClip(clip); // 原生NoClip，完全兼容

            if (y != 0 || p != 0) {
                sword.setYAW(y + k * i + zj);
                sword.setPITCH(p + k * i + zj);
            }

            Vec3 spawnPos;
            if (mode == SpawnMode.RANDOM) {
                double rx = center.x + (world.random.nextDouble() - 0.5) * 5;
                double ry = center.y + world.random.nextDouble() * 5;
                double rz = center.z + (world.random.nextDouble() - 0.5) * 5;
                spawnPos = new Vec3(rx, ry, rz);
            } else {
                double ang = Math.PI * 2 * i / count;
                spawnPos = center.add(3 * Math.cos(ang), 0, 3 * Math.sin(ang));
            }

            sword.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            // 初始化固定位置
            sword.fixedDelayPos = spawnPos;

            if (y == 0 && p == 0) {
                sword.setRotationSafe(-owner.getYHeadRot(), -owner.getXRot());
            } else {
                sword.setRotationSafe(-(sword.getDiyPitch() + k * i + zj), -(sword.getDiyYaw() + k * i + zj));
            }

            world.addFreshEntity(sword);
        }
    }

    public enum SpawnMode { CIRCLE, RANDOM }
}