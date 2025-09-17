package net.xianyu.prinegorerouse.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.boss.EnderDragonPart;
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
  
  private static final EntityDataAccessor<Integer> DELAY_TICK = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.INT);
  
  public static final EntityDataAccessor<Boolean> IT_CHANGED = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);
  
  private static final EntityDataAccessor<Boolean> USE_CUSTOM_DIRECTION = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);
  
  private static final EntityDataAccessor<Boolean> AUTO_TARGETING = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);
  
  private final float rotationAngle = 0.0F;
  
  private int delayTicks = 20;
  
  public SpawnMode spawnMode = SpawnMode.CIRCLE;
  
  private Vec3 prevPos = Vec3.ZERO;
  
  private float prevRotationAngle = 0.0F;
  
  public int lifeTime = 200;
  
  private boolean hasInitialized = false;
  
  private float initialYaw;
  
  private float initialPitch;
  
  private float diyYaw;
  
  private float diyPitch;
  
  private Entity lockTarget;
  
  protected float trackingRange = 8.0F;
  
  protected LivingEntity dynamicTarget;
  
  protected boolean useSmartTracking = true;
  
  protected double directTrackingPrecision = 0.98D;
  
  protected double maxPredictionDistance = 10.0D;
  
  private int smiteLevel;
  
  private int sharpnessLevel;
  
  private int baneLevel;
  
  private Entity currentMainTarget = null;
  
  private static final int SEARCH_COOLDOWN = 5;
  
  private int searchCooldown = 0;
  
  private boolean isDiscarded = false;
  
  public EntityNRBlisteringSword(EntityType<? extends Projectile> type, Level world) {
    super(type, world);
    setPierce((byte)5);
  }
  
  protected void defineSynchedData() {
    super.defineSynchedData();
    this.entityData.define(IT_FIRED, Boolean.valueOf(false));
    this.entityData.define(TARGET, Optional.empty());
    this.entityData.define(SPEED, Float.valueOf(2.0F));
    this.entityData.define(CENTER_POS, Vec3.ZERO.toVector3f());
    this.entityData.define(DIRECTION_YAW, Float.valueOf(0.0F));
    this.entityData.define(DIRECTION_PITCH, Float.valueOf(0.0F));
    this.entityData.define(IT_CHANGED, Boolean.valueOf(true));
    this.entityData.define(DIY_YAW, Float.valueOf(0.0F));
    this.entityData.define(DIY_PITCH, Float.valueOf(0.0F));
    this.entityData.define(LOCK_TARGET, Optional.empty());
    this.entityData.define(LOCK_TARGET_ID, Integer.valueOf(-1));
    this.entityData.define(DELAY_TICK, Integer.valueOf(this.delayTicks));
    this.entityData.define(USE_CUSTOM_DIRECTION, Boolean.valueOf(false));
    this.entityData.define(AUTO_TARGETING, Boolean.valueOf(false));
  }
  
  public static EntityNRBlisteringSword createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
    return new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, worldIn);
  }
  
  public void doFire() {
    getEntityData().set(IT_FIRED, Boolean.valueOf(true));
  }
  
  public boolean itFired() {
    return ((Boolean)this.entityData.get(IT_FIRED)).booleanValue();
  }
  
  public void setChange(boolean change) {
    getEntityData().set(IT_CHANGED, Boolean.valueOf(change));
  }
  
  public void setUseCustomDirection(boolean use) {
    this.entityData.set(USE_CUSTOM_DIRECTION, Boolean.valueOf(use));
  }
  
  public boolean useCustomDirection() {
    return ((Boolean)this.entityData.get(USE_CUSTOM_DIRECTION)).booleanValue();
  }
  
  public void setAutoTargeting(boolean auto) {
    this.entityData.set(AUTO_TARGETING, Boolean.valueOf(auto));
  }
  
  public boolean autoTargetingEnabled() {
    return ((Boolean)this.entityData.get(AUTO_TARGETING)).booleanValue();
  }
  
  public void doChange() {
    getEntityData().set(IT_CHANGED, Boolean.valueOf(true));
  }
  
  public boolean itChanged() {
    return ((Boolean)this.entityData.get(IT_CHANGED)).booleanValue();
  }
  
  public void setSpeed(float speed) {
    getEntityData().set(SPEED, Float.valueOf(speed));
  }
  
  public float getSpeed() {
    return ((Float)this.entityData.get(SPEED)).floatValue();
  }
  
  public int getDelayTicks() {
    return ((Integer)this.entityData.get(DELAY_TICK)).intValue();
  }
  
  public void setDelayTicks(int delayTick) {
    getEntityData().set(DELAY_TICK, Integer.valueOf(delayTick));
  }
  
  public void setCenterPosition(Vec3 pos) {
    this.entityData.set(CENTER_POS, pos.toVector3f());
  }
  
  public float getDiyYaw() {
    return ((Float)this.entityData.get(DIY_YAW)).floatValue();
  }
  
  public void setYAW(Float diyYaw) {
    this.entityData.set(DIY_YAW, diyYaw);
  }
  
  public float getDiyPitch() {
    return ((Float)this.entityData.get(DIY_PITCH)).floatValue();
  }
  
  public void setPITCH(Float diyPitch) {
    this.entityData.set(DIY_PITCH, diyPitch);
  }
  
  public int getLifeTime() {
    return this.lifeTime;
  }
  
  public void setLifeTime(int lifeTime) {
    this.lifeTime = lifeTime;
  }
  
  private Entity getMainEntity(Entity entity) {
    if (entity instanceof EnderDragonPart) {
      EnderDragonPart part = (EnderDragonPart)entity;
      return part.parentMob;
    } 
    if (entity != null) {
      try {
        Field parentField = entity.getClass().getDeclaredField("parent");
        parentField.setAccessible(true);
        Object parent = parentField.get(entity);
        if (parent instanceof Entity)
          return (Entity)parent; 
      } catch (NoSuchFieldException noSuchFieldException) {
      
      } catch (Exception exception) {}
      try {
        Method getParent = entity.getClass().getMethod("getParent", new Class[0]);
        Object parent = getParent.invoke(entity, new Object[0]);
        if (parent instanceof Entity)
          return (Entity)parent; 
      } catch (NoSuchMethodException noSuchMethodException) {
      
      } catch (Exception exception) {}
    } 
    return entity;
  }
  
  public Optional<Entity> getLockTarget() {
    int targetId = ((Integer)this.entityData.get(LOCK_TARGET_ID)).intValue();
    if (targetId == -1)
      return Optional.empty(); 
    Entity target = level().getEntity(targetId);
    if (target != null) {
      Entity mainEntity = getMainEntity(target);
      return Optional.ofNullable(mainEntity);
    } 
    return Optional.ofNullable(target);
  }
  
  public void enableSmartTracking(boolean enable) {
    this.useSmartTracking = enable;
  }
  
  public void tick() {
    if (this.isDiscarded)
      return; 
    this.currentMainTarget = null;
    if (!this.hasInitialized) {
      initializeEntity();
      this.prevPos = position();
    } 
    if (!(level()).isClientSide && --this.lifeTime <= 0) {
      discard();
      return;
    } 
    if (!itFired()) {
      if (getOwner() == null || !getOwner().isAlive()) {
        discard();
        return;
      } 
      handleDelayPhase();
    } else {
      handleFlyingPhase();
      super.tick();
    } 
  }
  
  private void initializeEntity() {
    setNoGravity(true);
    setInvulnerable(true);
    this.delayTicks = getDelayTicks();
    if (!(level()).isClientSide)
      extractEnchantments(); 
    if (getOwner() != null) {
      LivingEntity owner = (LivingEntity)getOwner();
      if (getDiyYaw() == 0.0F && getDiyPitch() == 0.0F) {
        if (this.spawnMode == SpawnMode.RANDOM) {
          this.initialYaw = owner.getYRot();
          this.initialPitch = owner.getXRot();
        } else {
          this.initialYaw = -owner.getYRot();
          this.initialPitch = -owner.getXRot();
        } 
        setDeltaMovement(getDeltaMovement());
        if (!itChanged()) {
          setYRot(this.initialYaw);
          setXRot(this.initialPitch);
        } 
        this.entityData.set(DIRECTION_YAW, Float.valueOf(this.initialYaw));
        this.entityData.set(DIRECTION_PITCH, Float.valueOf(this.initialPitch));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
      } else {
        this.initialYaw = getDiyYaw();
        this.initialPitch = getDiyPitch();
        double yawRad = (this.initialYaw * 0.017453292F);
        double pitchRad = (this.initialPitch * 0.017453292F);
        double xzLen = Math.cos(pitchRad);
        double x = -Math.sin(yawRad) * xzLen;
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * xzLen;
        Vec3 direction = (new Vec3(x, y, z)).normalize();
        setDeltaMovement(direction);
        if (!itChanged()) {
          setYRot(-this.initialYaw);
          setXRot(-this.initialPitch);
        } 
        this.entityData.set(DIRECTION_YAW, Float.valueOf(this.initialYaw));
        this.entityData.set(DIRECTION_PITCH, Float.valueOf(this.initialPitch));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
      } 
    } 
    this.hasInitialized = true;
  }
  
  private void handleDelayPhase() {
    if ((level()).isClientSide) {
      this.prevPos = position();
      Objects.requireNonNull(this);
      this.prevRotationAngle = 0.0F;
    } 
    if (!(level()).isClientSide)
      if (this.delayTicks > 0) {
        this.delayTicks--;
        if (this.spawnMode == SpawnMode.RANDOM)
          if (!itChanged()) {
            setYRot(-this.initialYaw);
            setXRot(-this.initialPitch);
            adjustInitialDirection();
          } else {
            adjustInitialDirection();
          }  
      } else {
        launchProjectile();
        this.entityData.set(IT_FIRED, Boolean.valueOf(true));
      }  
    if ((level()).isClientSide)
      adjustInitialDirection(); 
  }
  
  private void maintainCustomDirection() {
    setYRot(this.initialYaw);
    setXRot(this.initialPitch);
  }
  
  private void adjustInitialDirection() {
    if (itChanged()) {
      LivingEntity owner = (LivingEntity)getOwner();
      if (owner == null)
        return; 
      this.initialYaw = -owner.getYRot();
      this.initialPitch = -owner.getXRot();
      setYRot(this.initialYaw);
      setXRot(this.initialPitch);
    } 
  }
  
  private void launchProjectile() {
    float yawRad;
    float pitchRad;
    double xzLen;
    double x;
    double y;
    double z;
    if (getOwner() == null) {
      discard();
      return;
    } 
    switch (this.spawnMode) {
      case CIRCLE:
        if (itChanged()) {
          float currentYaw = getOwner().getYRot();
          float currentPitch = getOwner().getXRot();
          float f1 = currentYaw * 0.017453292F;
          float f2 = currentPitch * 0.017453292F;
          double d1 = Math.cos(f2);
          double d2 = -Math.sin(f1) * d1;
          double d3 = -Math.sin(f2);
          double d4 = Math.cos(f1) * d1;
          shoot(d2, d3, d4, getSpeed(), 0.0F);
          break;
        } 
        yawRad = -this.initialYaw * 0.017453292F;
        pitchRad = -this.initialPitch * 0.017453292F;
        xzLen = Math.cos(pitchRad);
        x = -Math.sin(yawRad) * xzLen;
        y = -Math.sin(pitchRad);
        z = Math.cos(yawRad) * xzLen;
        shoot(x, y, z, getSpeed(), 0.0F);
        break;
      case RANDOM:
        if (itChanged()) {
          float currentYaw = getOwner().getYRot();
          float currentPitch = getOwner().getXRot();
          float f1 = currentYaw * 0.017453292F;
          float f2 = currentPitch * 0.017453292F;
          double d1 = Math.cos(f2);
          double d2 = -Math.sin(f1) * d1;
          double d3 = -Math.sin(f2);
          double d4 = Math.cos(f1) * d1;
          shoot(d2, d3, d4, getSpeed(), 0.0F);
          break;
        } 
        yawRad = this.initialYaw * 0.017453292F;
        pitchRad = this.initialPitch * 0.017453292F;
        xzLen = Math.cos(pitchRad);
        x = -Math.sin(yawRad) * xzLen;
        y = -Math.sin(pitchRad);
        z = Math.cos(yawRad) * xzLen;
        shoot(x, y, z, getSpeed(), 0.0F);
        break;
    } 
  }
  
  private void handleFlyingPhase() {
    if (useCustomDirection()) {
      if (autoTargetingEnabled()) {
        handleSmartTracking();
      } else {
        maintainCustomDirection();
      } 
    } else if (this.useSmartTracking) {
      handleSmartTracking();
    } else {
      handleLegacyTracking();
    } 
    super.tick();
  }
  
  protected void handleSmartTracking() {
    Optional<Entity> lockedTarget = getLockTarget().filter(Entity::isAlive);
    if (lockedTarget.isPresent()) {
      Entity target = lockedTarget.get();
      directSteerToTarget(target);
      if (checkDirectHit(target))
        return; 
    } 
    if (this.dynamicTarget != null)
      if (!this.dynamicTarget.isAlive()) {
        this.dynamicTarget = null;
      } else {
        directSteerToTarget((Entity)this.dynamicTarget);
        if (checkDirectHit((Entity)this.dynamicTarget))
          return; 
      }  
    if (this.dynamicTarget == null && this.searchCooldown <= 0) {
      searchNearbyTargets();
      this.searchCooldown = 5;
    } else {
      this.searchCooldown--;
    } 
  }
  
  protected void handleLegacyTracking() {
    Optional<Entity> targetOpt = getLockTarget();
    if (targetOpt.isPresent()) {
      Entity target = targetOpt.get();
      directSteerToTarget(target);
      checkDirectHit(target);
    } 
  }
  
  protected Vec3 calculateTargetPosition(Entity target) {
    return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
  }
  
  protected void searchNearbyTargets() {
    AABB area = new AABB(getX() - this.trackingRange, getY() - this.trackingRange, getZ() - this.trackingRange, getX() + this.trackingRange, getY() + this.trackingRange, getZ() + this.trackingRange);
    LivingEntity bestTarget = null;
    double closestDistSq = Double.MAX_VALUE;
    for (Entity entity : level().getEntities((Entity)this, area)) {
      if (entity instanceof LivingEntity) {
        LivingEntity living = (LivingEntity)entity;
        if (!(entity instanceof net.minecraft.world.entity.monster.Enemy) || 
          !living.isAlive() || 
          isEntityPart((Entity)living))
          continue; 
        Entity entity1 = getOwner();
        if (entity1 instanceof LivingEntity) {
          LivingEntity owner = (LivingEntity)entity1;
          if (!TargetSelector.lockon.test(owner, living))
            continue; 
        } 
        double distSq = distanceToSqr((Entity)living);
        if (distSq < closestDistSq) {
          closestDistSq = distSq;
          bestTarget = living;
        } 
      } 
    } 
    this.dynamicTarget = bestTarget;
  }
  
  private boolean isEntityPart(Entity entity) {
    if (entity instanceof EnderDragonPart)
      return true; 
    String className = entity.getClass().getName().toLowerCase();
    return (className.contains("part") || className
      .contains("segment") || className
      .contains("section"));
  }
  
  protected void onHitEntity(EntityHitResult result) {
    Entity target = result.getEntity();
    Entity mainEntity = getMainEntity(target);
    if (this.currentMainTarget != null && this.currentMainTarget.equals(mainEntity))
      return; 
    this.currentMainTarget = mainEntity;
    applyHitEffects(mainEntity);
    discard();
  }
  
  private void applyHitEffects(Entity mainEntity) {
    if (!mainEntity.level().isClientSide && 
      mainEntity instanceof LivingEntity) {
      LivingEntity livingTarget = (LivingEntity)mainEntity;
      float baseDamage = (float)getDamage();
      float finalDamage = calculateEnchantedDamage(baseDamage, (Entity)livingTarget);
      setDamage(finalDamage);
      KnockBacks.cancel.action.accept(livingTarget);
      StunManager.setStun(livingTarget);
      BlackHoleUtil.addBlackHoleCount(livingTarget, 1);
      if (livingTarget.level() instanceof ServerLevel) {
        ServerLevel serverLevel = (ServerLevel)livingTarget.level();
        serverLevel.sendParticles((ParticleOptions)ParticleTypes.CRIT, livingTarget
            .getX(), livingTarget.getY() + 1.0D, livingTarget.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
      } 
      super.onHitEntity(new EntityHitResult(mainEntity));
    } 
  }
  
  private float calculateEnchantedDamage(float baseDamage, Entity target1) {
    float damage = baseDamage;
    if (target1 instanceof LivingEntity) {
      LivingEntity target = (LivingEntity)target1;
      MobType mobType = target.getMobType();
      damage += this.sharpnessLevel * 1.25F;
      float typeBonus = 0.0F;
      if (mobType == MobType.UNDEAD && this.smiteLevel > 0) {
        typeBonus = this.smiteLevel * 2.5F;
      } else if (mobType == MobType.ARTHROPOD && this.baneLevel > 0) {
        typeBonus = this.baneLevel * 2.5F;
      } 
      damage += typeBonus;
    } 
    return damage;
  }
  
  protected void onHitBlock(BlockHitResult result) {
    super.onHitBlock(result);
  }
  
  protected boolean checkDirectHit(Entity target) {
    if (this.isDiscarded)
      return true; 
    if (isEntityPart(target))
      return false; 
    Entity mainEntity = getMainEntity(target);
    if (this.currentMainTarget != null && this.currentMainTarget.equals(mainEntity))
      return false; 
    AABB targetBB = mainEntity.getBoundingBox().inflate(0.3D);
    Vec3 currentPos = position();
    Vec3 nextPos = currentPos.add(getDeltaMovement());
    if (targetBB.intersects(getBoundingBox())) {
      applyHitEffects(mainEntity);
      discardSafely();
      return true;
    } 
    Optional<Vec3> hitPos = targetBB.clip(currentPos, nextPos);
    if (hitPos.isPresent()) {
      applyHitEffects(mainEntity);
      discardSafely();
      return true;
    } 
    return false;
  }
  
  private void discardSafely() {
    this.isDiscarded = true;
    discard();
  }
  
  public static void spawnSwords(LivingEntity owner, Level world, Vec3 centerPos, SpawnMode mode, int count, boolean change, float diyYaw, float diyPitch, float zj, float k, double damage, int colorCode, boolean clip, int delay) {
    if (owner == null || !owner.isAlive())
      return; 
    for (int i = 0; i < count; i++) {
      EntityNRBlisteringSword sword = new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, world);
      if (diyPitch != 0.0F && diyYaw != 0.0F) {
        sword.setYAW(Float.valueOf(diyYaw + k * i + zj));
        sword.setPITCH(Float.valueOf(diyPitch + k * i + zj));
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
        double halfSize = 2.5D;
        double x = centerPos.x + (world.random.nextDouble() - 0.5D) * 5.0D;
        double y = centerPos.y + world.random.nextDouble() * 5.0D;
        double z = centerPos.z + (world.random.nextDouble() - 0.5D) * 5.0D;
        sword.moveTo(x, y, z);
      } else if (mode == SpawnMode.CIRCLE) {
        double radius = 3.0D;
        double angle = 6.283185307179586D * i / count;
        Vec3 pos = centerPos.add(radius * 
            Math.cos(angle), 0.0D, radius * Math.sin(angle));
        sword.moveTo(pos.x, pos.y, pos.z);
      } 
      if (diyPitch == 0.0F && diyYaw == 0.0F) {
        sword.setYRot(-owner.getYRot());
        sword.setXRot(-owner.getXRot());
      } else {
        sword.setYRot(-(sword.getDiyPitch() + k * i + zj));
        sword.setXRot(-(sword.getDiyYaw() + k * i + zj));
      } 
      world.addFreshEntity((Entity)sword);
    } 
  }
  
  protected void directSteerToTarget(Entity target) {
    Vec3 currentPos = position();
    Vec3 targetPos = calculateTargetPosition(target);
    Vec3 currentVelocity = getDeltaMovement();
    double currentSpeed = currentVelocity.length();
    Vec3 directDirection = targetPos.subtract(currentPos).normalize();
    Vec3 targetVelocity = target.getDeltaMovement();
    double distance = currentPos.distanceTo(targetPos);
    double timeToTarget = Math.min(distance / currentSpeed, 1.0D);
    Vec3 predictedPos = targetPos.add(targetVelocity.scale(timeToTarget * 0.8D));
    Vec3 desiredDirection = predictedPos.subtract(currentPos).normalize();
    Vec3 newDirection = directDirection.scale(1.0D - this.directTrackingPrecision).add(desiredDirection.scale(this.directTrackingPrecision)).normalize();
    Vec3 newVelocity = newDirection.scale(currentSpeed);
    setDeltaMovement(newVelocity);
    updateRotationFromVelocity(newVelocity);
  }
  
  private void updateRotationFromVelocity(Vec3 velocity) {
    double horizontalDistance = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    float yaw = (float)Math.toDegrees(Math.atan2(velocity.x, velocity.z));
    float pitch = (float)Math.toDegrees(Math.atan2(velocity.y, horizontalDistance));
    setYRot(yaw);
    setXRot(pitch);
    this.yRotO = yaw;
    this.xRotO = pitch;
  }
  
  private void extractEnchantments() {
    LivingEntity owner = (LivingEntity)getOwner();
    if (owner == null)
      return; 
    ItemStack mainHand = owner.getMainHandItem();
    if (mainHand.isEmpty())
      return; 
    Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(mainHand);
    this.sharpnessLevel = ((Integer)enchants.getOrDefault(Enchantments.SHARPNESS, Integer.valueOf(0))).intValue();
    this.smiteLevel = ((Integer)enchants.getOrDefault(Enchantments.SMITE, Integer.valueOf(0))).intValue();
    this.baneLevel = ((Integer)enchants.getOrDefault(Enchantments.BANE_OF_ARTHROPODS, Integer.valueOf(0))).intValue();
  }
  
  public enum SpawnMode {
    CIRCLE, RANDOM;
  }
}