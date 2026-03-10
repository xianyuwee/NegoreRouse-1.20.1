package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.EnumSetConverter;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static mods.flammpfeil.slashblade.SlashBladeConfig.REFINE_DAMAGE_MULTIPLIER;
import static mods.flammpfeil.slashblade.SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER;

public class EntityNRDrive extends EntityAbstractSummonedSword {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RANK = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_OFFSET = SynchedEntityData
            .defineId(EntityNRDrive.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_ROLL = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASESIZE = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LIFETIME = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DELAYTICK = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DELAYSPEED = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IN_DELAY = SynchedEntityData.defineId(EntityNRDrive.class,
            EntityDataSerializers.BOOLEAN);
    // 新增：伤害冷却机制
    private final Map<Entity, Integer> hitCooldownMap = new WeakHashMap<>();

    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(EntityNRDrive.class, EntityDataSerializers.INT);;

    private KnockBacks action = KnockBacks.cancel;

    private double damage = 7.0D;

    // 新增：存储初始旋转角度
    private float initialYaw;
    private float initialPitch;

    private int delayTicks;
    // 新增字段存储初始速度和剩余延迟
    private Vec3 initialVelocity;
    private int remainingDelayTicks;

    private boolean indelay = true;
    private Vec3 initialDirection = Vec3.ZERO;
    private float initialSpeed = 0;

    // 1. 新增 NoClip 同步数据（类内静态字段）
    private static final EntityDataAccessor<Boolean> NO_CLIP = SynchedEntityData.defineId(EntityNRDrive.class, EntityDataSerializers.BOOLEAN);
    // 2. 新增本地字段：旋转稳定阈值（避免Z轴反复震荡）
    private static final float ROTATE_THRESHOLD = 0.5f;
    // 3. 新增本地字段：标记是否已锁定旋转（防抖）
    private boolean isRotationLocked = false;


    public KnockBacks getKnockBack() {
        return action;
    }

    public void setKnockBack(KnockBacks action) {
        this.action = action;
    }

    public void setKnockBackOrdinal(int ordinal) {
        if (0 <= ordinal && ordinal < KnockBacks.values().length)
            this.action = KnockBacks.values()[ordinal];
        else
            this.action = KnockBacks.cancel;
    }

    public EntityNRDrive(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        this.setNoGravity(true);
        this.delayTicks = this.getDelayTick();
        this.initialYaw = this.getYRot();
        this.initialPitch = this.getXRot();
        this.initialSpeed = this.getSpeed();
    }

    public static EntityNRDrive createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityNRDrive(NrEntitiesRegistry.NRDrive, worldIn);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COLOR, 0x3333FF);
        this.entityData.define(RANK, 0.0f);
        this.entityData.define(LIFETIME, 10.0f);
        this.entityData.define(FLAGS, 0);
        this.entityData.define(ROTATION_OFFSET, 0.0f);
        this.entityData.define(ROTATION_ROLL, 0.0f);
        this.entityData.define(BASESIZE, 1.0f);
        this.entityData.define(SPEED, 0.5f);
        this.entityData.define(DELAYTICK, delayTicks);
        this.entityData.define(DELAYSPEED, 0.5F);
        this.entityData.define(IN_DELAY, indelay);
        this.entityData.define(NO_CLIP, false); // 初始化NoClip为false
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        NBTHelper.getNBTCoupler(compound).put("RotationOffset", this.getRotationOffset())
                .put("RotationRoll", this.getRotationRoll())
                .put("BaseSize", this.getBaseSize())
                .put("Speed", this.getSpeed())
                .put("Color", this.getColor())
                .put("Rank", this.getRank())
                .put("damage", this.damage)
                .put("Lifetime", this.getLifetime())
                .put("Knockback", this.getKnockBack().ordinal())
                // 新增：保存初始旋转角度
                .put("InitialYaw", this.initialYaw)
                .put("InitialPitch", this.initialPitch)
                //新增：延迟速度和时间
                .put("delayTicks", this.getDelayTick())
                .put("delayspeed", this.getDelaySpeed())
                .put("indelay", this.isIndelay())// 保存时添加剩余延迟
                .put("RemainingDelayTicks", this.remainingDelayTicks);

    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        NBTHelper.getNBTCoupler(compound)
                .get("RotationOffset", this::setRotationOffset)
                .get("RotationRoll", this::setRotationRoll)
                .get("BaseSize", this::setBaseSize)
                .get("Speed", this::setSpeed)
                .get("Color", this::setColor)
                .get("Rank", this::setRank)
                .get("damage", (Double v) -> this.damage = v, this.damage)
                .get("Lifetime", this::setLifetime)
                .get("Knockback", this::setKnockBackOrdinal)
                // 新增：读取初始旋转角度
                .get("InitialYaw", this::setInitialYaw, 0f)
                .get("InitialPitch", this::setInitialPitch, 0f)
                .get("delayTicks", this::setDelayTick, 20)
                .get("delayspeed", this::setDelaySpeed, 0.5f)
                // 加载时读取剩余延迟
                .get("RemainingDelayTicks", (Integer v) -> this.remainingDelayTicks = v, 0);
    }

    // 新增：设置初始偏航角
    public void setInitialYaw(float value) {
        this.initialYaw = value;
    }

    // 新增：设置初始俯仰角
    public void setInitialPitch(float value) {
        this.initialPitch = value;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 10.0D;
        if (Double.isNaN(d0)) {
            d0 = 1.0D;
        }

        d0 = d0 * 64.0D * getViewScale();
        return distance < d0 * d0;
    }

    private void refreshFlags() {
        int newValue;
        if (this.level().isClientSide()) {
            newValue = (Integer)this.entityData.get(FLAGS);
            if (this.intFlags != newValue) {
                this.intFlags = newValue;

            }
        } else {
            newValue = EnumSetConverter.convertToInt(this.flags);
            if (this.intFlags != newValue) {
                this.entityData.set(FLAGS, newValue);
                this.intFlags = newValue;
            }
        }

    }

    @Override
    public void refreshDimensions() {
        // 获取渲染用的 baseSize（DivineCrossSA 中设置为 15.0F）
        float baseSize = this.getBaseSize();
        // 比例系数：根据渲染缩放比调整（可自行微调分母，比如 10/15/20）
        // 示例：15.0F 的 baseSize 对应 1.5F 的碰撞箱尺寸（15/10=1.5）

        // 构建新碰撞箱：
        // - X/Z 轴：从 -sizeScale/2 到 sizeScale/2（与渲染模型中心对齐）
        // - Y 轴：从 0 到 sizeScale（底部对齐实体位置，修正“高度从中心算”的问题）
        AABB newBoundingBox = new AABB(
                -baseSize / 2.0F,  // minX
                -baseSize,               // minY（底部对齐，不再以中心为中点）
                -baseSize / 2.0F,  // minZ
                baseSize / 2.0F,   // maxX
                baseSize,          // maxY（高度 = sizeScale）
                baseSize / 2.0F    // maxZ
        );

        // 设置新碰撞箱并刷新
        this.setBoundingBox(newBoundingBox);
        super.refreshDimensions();
    }

    @Override
    public void tick() {
        refreshFlags();
        updateHitCooldown();

        // ===== 新增：NoClip 核心处理逻辑 =====
        boolean noClip = this.isNoClip();
        // 1. NoClip=true 时，完全禁用物理碰撞和自动运动
        if (noClip) {
            this.setNoGravity(true); // 强制无重力
            this.setInvulnerable(true); // 可选：无敌（避免碰撞伤害干扰）
            this.noPhysics = true; // 禁用MC内置物理更新
            // 跳过方块/实体碰撞检测（避免物理引擎回退位移）
            this.verticalCollision = false;
        }

        // ===== 原有延迟逻辑改造（适配NoClip）=====
        if (indelay) {
            remainingDelayTicks--;

            // NoClip=true 时：旋转防抖（避免Z轴震荡）
            if (noClip && !isRotationLocked) {
                // 计算当前旋转与初始旋转的差值
                float yawDiff = Math.abs(this.getYRot() - initialYaw);
                float pitchDiff = Math.abs(this.getXRot() - initialPitch);
                // 差值小于阈值时，锁定旋转（终止强制设置，避免循环）
                if (yawDiff < ROTATE_THRESHOLD && pitchDiff < ROTATE_THRESHOLD) {
                    this.isRotationLocked = true;
                } else {
                    // 未锁定时才强制设置旋转（减少震荡）
                    this.setYRot(initialYaw);
                    this.yRotO = initialYaw;
                    this.setXRot(initialPitch);
                    this.xRotO = initialPitch;
                }
            } else if (!noClip) {
                // NoClip=false 时保留原有旋转逻辑
                this.setYRot(initialYaw);
                this.yRotO = initialYaw;
                this.setXRot(initialPitch);
                this.xRotO = initialPitch;
            }

            // NoClip=true 时：强制位移由代码控制，禁用物理插值
            Vec3 targetMotion = initialDirection.scale(this.getDelaySpeed());
            if (noClip) {
                this.setDeltaMovement(targetMotion); // 直接设置，无插值
            } else {
                // NoClip=false 时保留原有位移逻辑（带物理插值）
                this.setDeltaMovement(this.getDeltaMovement().lerp(targetMotion, 0.5));
            }

            // NoClip=true 时跳过碰撞检测（避免物理引擎干扰）
            if (!noClip) {
                checkCollisions();
            }

            if (remainingDelayTicks <= 0) {
                setInDelay(false);
                remainingDelayTicks = -1;
                Vec3 finalMotion = initialDirection.scale(this.initialSpeed);
                if (noClip) {
                    this.setDeltaMovement(finalMotion);
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().lerp(finalMotion, 0.5));
                }
                // 延迟结束后解锁旋转（仅NoClip模式）
                if (noClip) this.isRotationLocked = false;
            }
        } else {
            // 正常状态逻辑改造（适配NoClip）
            if (noClip && !isRotationLocked) {
                float yawDiff = Math.abs(this.getYRot() - initialYaw);
                float pitchDiff = Math.abs(this.getXRot() - initialPitch);
                if (yawDiff < ROTATE_THRESHOLD && pitchDiff < ROTATE_THRESHOLD) {
                    this.isRotationLocked = true;
                } else {
                    this.setYRot(initialYaw);
                    this.yRotO = initialYaw;
                    this.setXRot(initialPitch);
                    this.xRotO = initialPitch;
                }
            } else if (!noClip) {
                this.setYRot(initialYaw);
                this.yRotO = initialYaw;
                this.setXRot(initialPitch);
                this.xRotO = initialPitch;
            }

            if (noClip) {
                this.setDeltaMovement(initialDirection.scale(this.initialSpeed));
            }
            this.setSpeed(initialSpeed);
        }

        // ===== 原有逻辑 =====
        super.tick();
        tryDespawn();
    }

    private void checkCollisions() {
        // NoClip=true 时跳过所有碰撞检测
        if (this.isNoClip()) return;

        Vec3 newPos = this.position().add(this.getDeltaMovement());

        // 原有实体碰撞逻辑
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                this.level(), this, this.position(), newPos,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable() && entity != this.getShooter()
        );

        if (entityHit != null) {
            if (!hitCooldownMap.containsKey(entityHit.getEntity())) {
                this.onHitEntity(entityHit);
                hitCooldownMap.put(entityHit.getEntity(), 10);
            }
        }

        // 原有方块碰撞逻辑
        try {
            BlockHitResult blockHit = this.level().clip(new ClipContext(
                    this.position(), newPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
            ));
            if (blockHit.getType() != HitResult.Type.MISS) {
                this.onHitBlock(blockHit);
            }
        } catch (Exception e) {
            SlashBlade.LOGGER.error("NRDrive: 方块碰撞检测异常（大概率是传送门方块）", e);
        }
    }

    // 新增：更新伤害冷却
    private void updateHitCooldown() {
        Iterator<Map.Entry<Entity, Integer>> iterator = hitCooldownMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Integer> entry = iterator.next();
            int cooldown = entry.getValue() - 1;
            if (cooldown <= 0) {
                iterator.remove();
            } else {
                entry.setValue(cooldown);
            }
        }
    }


    protected void tryDespawn() {
        if (!this.level().isClientSide()) {
            if (getLifetime() < this.tickCount)
                this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3 vec3d = (new Vec3(x, y, z)).normalize().add(
                this.random.nextGaussian() * 0.007499999832361937 * (double)inaccuracy,
                this.random.nextGaussian() * 0.007499999832361937 * (double)inaccuracy,
                this.random.nextGaussian() * 0.007499999832361937 * (double)inaccuracy
        ).scale((double)velocity);

        // NoClip=true 时：直接设置位移，无物理随机偏移
        if (this.isNoClip()) {
            vec3d = (new Vec3(x, y, z)).normalize().scale((double)velocity);
        }

        this.setDeltaMovement(vec3d);
        this.initialDirection = vec3d;
        float f = Mth.sqrt((float)vec3d.horizontalDistanceSqr());

        this.setPos(this.position());
        this.initialSpeed = velocity;

        this.initialYaw = (float)(Mth.atan2(vec3d.x, vec3d.z) * 57.2957763671875F);
        this.initialPitch = (float)(Mth.atan2(vec3d.y, (double)f) * 57.2957763671875F);

        // NoClip=true 时：强制锁定初始旋转（无插值）
        if (this.isNoClip()) {
            this.setYRot(initialYaw);
            this.setXRot(initialPitch);
            this.yRotO = initialYaw;
            this.xRotO = initialPitch;
            this.isRotationLocked = true; // 直接锁定，避免震荡
        } else {
            this.setYRot(initialYaw);
            this.setXRot(initialPitch);
            this.yRotO = initialYaw;
            this.xRotO = initialPitch;
        }
    }

    public int getColor() {
        return this.getEntityData().get(COLOR);
    }

    public void setColor(int value) {
        this.getEntityData().set(COLOR, value);
    }

    public float getRank() {
        return this.getEntityData().get(RANK);
    }

    public void setRank(float value) {
        this.getEntityData().set(RANK, value);
    }

    public IConcentrationRank.ConcentrationRanks getRankCode() {
        return IConcentrationRank.ConcentrationRanks.getRankFromLevel(getRank());
    }

    public float getRotationOffset() {
        return this.getEntityData().get(ROTATION_OFFSET);
    }

    public void setRotationOffset(float value) {
        this.getEntityData().set(ROTATION_OFFSET, value);
    }

    public float getRotationRoll() {
        return this.getEntityData().get(ROTATION_ROLL);
    }

    public void setRotationRoll(float value) {
        this.getEntityData().set(ROTATION_ROLL, value);
    }

    public float getBaseSize() {
        return this.getEntityData().get(BASESIZE);
    }


    public void setBaseSize(float value) {
        this.getEntityData().set(BASESIZE, value);
        // 关键：更新 baseSize 后同步刷新碰撞箱
        this.refreshDimensions();
        // 服务端同步（可选，确保多端碰撞箱一致）
        if (!this.level().isClientSide()) {
            this.refreshDimensions();
        }
    }

    public float getSpeed() {
        return this.getEntityData().get(SPEED);
    }

    public void setSpeed(float value) {
        this.getEntityData().set(SPEED, value);
    }

    public float getLifetime() {
        return this.getEntityData().get(LIFETIME);
    }

    public void setLifetime(float value) {
        this.getEntityData().set(LIFETIME, value);
    }

    @Nullable
    @Override
    public Entity getShooter() {
        return this.getOwner();
    }

    @Override
    public void setShooter(Entity shooter) {
        setOwner(shooter);
    }

    public List<MobEffectInstance> getPotionEffects() {
        List<MobEffectInstance> effects = PotionUtils.getAllEffects(this.getPersistentData());

        if (effects.isEmpty())
            effects.add(new MobEffectInstance(MobEffects.POISON, 1, 1));

        return effects;
    }

    public void setDamage(double damageIn) {
        this.damage = damageIn;
    }

    @Override
    public double getDamage() {
        return this.damage;
    }

    // 伤害判定方法（增加兜底防护）
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity targetEntity = entityHitResult.getEntity();
        Entity shooter = this.getShooter();

        // 兜底：排除发射者自身（含实体部件）
        if (targetEntity == shooter) return;
        if (targetEntity instanceof PartEntity && ((PartEntity<?>) targetEntity).getParent() == shooter) return;

        float damageValue = (float) this.getDamage();
        DamageSource damagesource;
        if (shooter == null) {
            damagesource = this.damageSources().indirectMagic(this, this);
        } else {
            damagesource = this.damageSources().indirectMagic(this, shooter);
            if (shooter instanceof LivingEntity) {
                Entity hits = targetEntity;
                if (targetEntity instanceof PartEntity) {
                    hits = ((PartEntity<?>) targetEntity).getParent();
                }
                ((LivingEntity) shooter).setLastHurtMob(hits);
            }
        }

        int fireTime = targetEntity.getRemainingFireTicks();
        if (this.isOnFire() && !(targetEntity instanceof EnderMan)) {
            targetEntity.setSecondsOnFire(5);
        }

        targetEntity.invulnerableTime = 0;
        if (this.getOwner() instanceof LivingEntity living) {
            damageValue *= living.getAttributeValue(Attributes.ATTACK_DAMAGE);

            // 评分等级加成
            if (living instanceof Player player) {
                IConcentrationRank.ConcentrationRanks rankBonus = player
                        .getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                        .map(rp -> rp.getRank(player.getCommandSenderWorld().getGameTime()))
                        .orElse(IConcentrationRank.ConcentrationRanks.NONE);

                float rankDamageBonus = rankBonus.level / 2.0f;

                if (IConcentrationRank.ConcentrationRanks.S.level <= rankBonus.level) {
                    int refine = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                            .map(rp -> rp.getRefine())
                            .orElse(0);
                    int level = player.experienceLevel;
                    rankDamageBonus = (float) Math.max(
                            rankDamageBonus,
                            Math.min(level, refine) * REFINE_DAMAGE_MULTIPLIER.get()
                    );
                }
                damageValue += rankDamageBonus;
            }

            // 使用 ItemSlashBlade 的 getAttackDamage 方法替代
            damageValue *= AttackManager.getSlashBladeDamageScale(living) * SLASHBLADE_DAMAGE_MULTIPLIER.get();

            // 使用父类的isCritical方法
            if (this.isCritical()) {
                damageValue += this.random.nextInt((Mth.ceil(damageValue) / 2 + 2));
            }
        }

        if (targetEntity.hurt(damagesource, damageValue)) {
            Entity hits = targetEntity;
            if (targetEntity instanceof PartEntity) {
                hits = ((PartEntity<?>) targetEntity).getParent();
            }

            if (hits instanceof LivingEntity) {
                LivingEntity targetLivingEntity = (LivingEntity) hits;

                StunManager.setStun(targetLivingEntity);
                if (!this.level().isClientSide() && shooter instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(targetLivingEntity, shooter);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity) shooter, targetLivingEntity);
                }

                affectEntity(targetLivingEntity, getPotionEffects(), 1.0f);

                if (shooter != null && targetLivingEntity != shooter && targetLivingEntity instanceof Player
                        && shooter instanceof ServerPlayer) {
                    ((ServerPlayer) shooter).playNotifySound(this.getHitEntityPlayerSound(), SoundSource.PLAYERS, 0.18F,
                            0.45F);
                }
            }

            this.playSound(this.getHitEntitySound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        } else {
            targetEntity.setRemainingFireTicks(fireTime);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        try {
            // 1. 获取击中的方块状态，判断是否为传送门类方块
            BlockState hitBlockState = level().getBlockState(blockHitResult.getBlockPos());
            Block hitBlock = hitBlockState.getBlock();

            // 2. 排除下界传送门、末地传送门、末地折跃门等特殊方块
            if (hitBlock == Blocks.NETHER_PORTAL || hitBlock == Blocks.END_PORTAL || hitBlock == Blocks.END_GATEWAY) {
                // 传送门方块特殊处理：不移除实体，仅减速+减少生命周期
                if (!level().isClientSide()) {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
                    this.setLifetime(this.getLifetime() - 1);
                }
                return; // 跳过默认移除逻辑
            }

            // 3. 非传送门方块，仅在服务端执行移除操作（避免客户端同步问题）
            if (!level().isClientSide()) {
                this.setRemoved(RemovalReason.DISCARDED);
            }
        } catch (Exception e) {
            // 4. 捕获所有异常，防止崩溃并记录日志
            SlashBlade.LOGGER.error("NRDrive: 击中方块时发生异常（方块位置：{}）", blockHitResult.getBlockPos(), e);
            // 异常时安全移除实体
            if (!level().isClientSide()) {
                this.setRemoved(RemovalReason.DISCARDED);
            }
        }
    }

    @Nullable
    public EntityHitResult getRayTrace(Vec3 p_213866_1_, Vec3 p_213866_2_) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, p_213866_1_, p_213866_2_,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), (entity) -> {
                    return !entity.isSpectator() && entity.isAlive() && entity.isPickable()
                            && (entity != this.getShooter());
                });
    }

    // 使用父类方法操作标志位
    public void setCritical(boolean critical) {
        this.setIsCritical(critical);
    }

    public boolean isCritical() {
        return this.getIsCritical();
    }

    // 新增 NoClip 读写方法
    public boolean isNoClip() {
        return this.entityData.get(NO_CLIP);
    }
    public void setNoClip(boolean noClip) {
        this.entityData.set(NO_CLIP, noClip);
        // 关键：设置NoClip时同步禁用物理碰撞（兜底）
        this.noPhysics = noClip; // MC Entity 内置的noPhysics字段，禁用物理驱动
    }

    public void setInDelay(boolean inDelay) {
        this.indelay = inDelay;
    }

    public boolean isIndelay() {
        return this.indelay;
    }

    public int getDelayTick() {
        return this.getEntityData().get(DELAYTICK);
    }

    public void setDelayTick(int tick) {
        // 如果尚未开始延迟，更新剩余时间
        if (this.remainingDelayTicks <= 0) {
            this.remainingDelayTicks = tick;
        }
        this.getEntityData().set(DELAYTICK, tick);
    }

    public float getDelaySpeed() {
        return this.getEntityData().get(DELAYSPEED);
    }

    public void setDelaySpeed(float delaySpeed) {
        this.getEntityData().set(DELAYSPEED,delaySpeed);
    }
}