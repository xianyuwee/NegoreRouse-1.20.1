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
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import org.jetbrains.annotations.NotNull;

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

    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(EntityNRDrive.class, EntityDataSerializers.INT);;

    private KnockBacks action = KnockBacks.cancel;

    private double damage = 7.0D;

    // 新增：存储初始旋转角度
    private float initialYaw;
    private float initialPitch;

    // 原有参数保留，仅新增这两行
    private static final EntityDataAccessor<Float> INITIAL_YAW = SynchedEntityData.defineId(EntityNRDrive.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> INITIAL_PITCH = SynchedEntityData.defineId(EntityNRDrive.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> CUSTOM_HITBOX_SCALE = SynchedEntityData.defineId(EntityNRDrive.class, EntityDataSerializers.FLOAT);

    private int delayTicks;
    // 新增字段存储初始速度和剩余延迟
    private Vec3 initialVelocity;
    private int remainingDelayTicks;

    private static final boolean HITBOX_DEBUG = true;

    private boolean indelay = true;
    private Vec3 initialDirection = Vec3.ZERO;
    private float initialSpeed = 0;

    private boolean hitboxInitialized = false;

    // 基准碰撞箱：对应 BASESIZE=1.0 时的大小（0.5x0.5x0.5 是默认极小碰撞箱，可按需放大）
    private static final float BASE_HITBOX_SIZE = 0.5F;


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
        this.entityData.define(INITIAL_YAW, 0.0f);
        this.entityData.define(INITIAL_PITCH, 0.0f);

        this.entityData.define(CUSTOM_HITBOX_SCALE, 1.0F);
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
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
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

    // 新增：客户端安全获取初始旋转（关键！渲染用这个）
    public float getInitialYaw() {
        return this.entityData.get(INITIAL_YAW);
    }
    public float getInitialPitch() {
        return this.entityData.get(INITIAL_PITCH);
    }

    /**
     * 实体位置/大小变化时，强制刷新碰撞箱
     */
    public void recalculateBoundingBox() {
        // 计算最终碰撞箱大小 = 基础尺寸 × 渲染缩放值
        float size = BASE_HITBOX_SIZE * getBaseSize();
        // 设置自定义碰撞箱（中心对称）
        this.setBoundingBox(new AABB(
                this.getX() - size, this.getY() - size, this.getZ() - size,
                this.getX() + size, this.getY() + size, this.getZ() + size
        ));
    }

    /**
     * 修改大小时，立即刷新碰撞箱
     */
    public void setBaseSize(float value) {
        this.getEntityData().set(BASESIZE, value);
        // 大小变化 → 立即刷新碰撞箱
        this.recalculateBoundingBox();
    }

    // 设置自定义碰撞箱缩放（给单个幻影刃单独放大）
    public void setHitboxScale(float scale) {
        this.entityData.set(CUSTOM_HITBOX_SCALE, Math.max(0.1F, scale)); // 防止负数
    }

    // 获取当前碰撞箱缩放
    public float getHitboxScale() {
        return this.entityData.get(CUSTOM_HITBOX_SCALE);
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
    public void tick() {
        this.forceCustomHitbox();
        this.refreshFlags();

        if (remainingDelayTicks == 0 && getDelayTick() > 0) {
            remainingDelayTicks = getDelayTick();
            setInDelay(true);
            this.initialDirection = this.getDeltaMovement().normalize();
        }

        if (indelay) {
            remainingDelayTicks--;
            float customYaw = this.getRotationOffset();
            float customRoll = this.getRoll();
            this.setRoll(customRoll);
            this.setRotationOffset(customYaw);
            float delaySpeed = this.getDelaySpeed();
            this.setDeltaMovement(initialDirection.scale(delaySpeed));
            this.setSpeed(delaySpeed);
            if (remainingDelayTicks <= 0) {
                setInDelay(false);
                remainingDelayTicks = -1;
                this.setDeltaMovement(initialDirection.scale(this.initialSpeed));
            }
        } else {
            float customYaw = this.getOffsetYaw();
            float customRoll = this.getRoll();
            this.setRoll(customRoll);
            this.setRotationOffset(customYaw);
            this.setSpeed(initialSpeed);
        }

        // 碰撞箱重叠=造成伤害
        if (!this.level().isClientSide()) {
            // 获取当前碰撞箱内的所有实体
            for (Entity entity : this.level().getEntities(this, this.getBoundingBox())) {
                // 过滤条件：存活、可命中、不是自己、不在冷却
                if (entity instanceof LivingEntity target
                        && entity.isAlive()
                        && !entity.isSpectator()
                        && entity != this.getShooter()) {

                    // 直接触发伤害（调用父类原生伤害逻辑，完美兼容拔刀剑）
                    this.onHitEntity(new EntityHitResult(entity));
                }
            }
        }

        super.tick();

//        // 原有的调试日志、tryDespawn 保留
//        if (HITBOX_DEBUG && !this.level().isClientSide()) {
//            AABB hitbox = this.getBoundingBox();
//            SlashBlade.LOGGER.info(String.format(
//                    "[NRDrive 碰撞箱] ID:%d | BASESIZE:%.1f | 缩放:%.1f | 尺寸:X=%.2f,Y=%.2f,Z=%.2f",
//                    this.getId(), this.getBaseSize(), this.getHitboxScale(),
//                    hitbox.getXsize(), hitbox.getYsize(), hitbox.getZsize()
//            ));
//        }

        this.tryDespawn();
    }

    //重写setPos，设置位置后立刻锁死碰撞箱
    @Override
    public void setPos(double x, double y, double z) {
        // 执行原生设置位置
        super.setPos(x, y, z);
        // 强制锁死自定义碰撞箱（永不被重置）
        forceCustomHitbox();
    }

    /** 自定义碰撞检测：完全依赖自定义碰撞箱，碰撞箱碰到就命中 */
    private void customCollisionDetection() {
        // NoClip状态（延迟期）：跳过检测
        if (this.isNoClip()) {
            this.setPos(this.position().add(this.getDeltaMovement()));
            return;
        }

        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(this.getDeltaMovement());
        AABB detectBox = this.getBoundingBox().expandTowards(this.getDeltaMovement()); // 扩大检测范围，避免漏判

        // 1. 实体命中检测（核心：用自定义碰撞箱）
        List<Entity> hitEntities = this.level().getEntities(
                this,
                detectBox,
                entity -> entity != null &&
                        entity.isAlive() &&
                        entity.isPickable() &&
                        !entity.isSpectator() &&
                        entity != this.getShooter()
        );

        // 命中多个实体时，逐个处理伤害
        for (Entity target : hitEntities) {
            // 排除实体部件（如BOSS的肢体）
            if (target instanceof PartEntity) {
                target = ((PartEntity<?>) target).getParent();
            }

            // 触发伤害逻辑
            this.onHitEntity(new EntityHitResult(target));
        }

        // 2. 方块碰撞检测（保留原有逻辑）
        try {
            BlockHitResult blockHit = this.level().clip(new ClipContext(
                    currentPos, nextPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
            ));
            if (blockHit.getType() != HitResult.Type.MISS) {
                this.onHitBlock(blockHit);
            }
        } catch (Exception e) {
            SlashBlade.LOGGER.error("NRDrive: 方块碰撞异常", e);
        }

        // 更新实体位置
        this.setPos(nextPos);
    }


    /** 强制设置自定义碰撞箱，阻止父类重置 */
    private void forceCustomHitbox() {
        // 固定计算：0.5 * BASESIZE = 半径（15级就是7.5半径，总尺寸15）
        float radius = 0.5F * this.getBaseSize() * this.getHitboxScale();
        // 设置超大碰撞箱
        this.setBoundingBox(
                new AABB(
                        this.getX() - radius, this.getY() - radius, this.getZ() - radius,
                        this.getX() + radius, this.getY() + radius, this.getZ() + radius
                )
        );
    }

//    // 新增：更新伤害冷却
//    private void updateHitCooldown() {
//        Iterator<Map.Entry<Entity, Integer>> iterator = hitCooldownMap.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry<Entity, Integer> entry = iterator.next();
//            int cooldown = entry.getValue() - 1;
//            if (cooldown <= 0) {
//                iterator.remove();
//            } else {
//                entry.setValue(cooldown);
//            }
//        }
//    }


    protected void tryDespawn() {
        if (!this.level().isClientSide()) {
            if (getLifetime() < this.tickCount)
                this.remove(RemovalReason.DISCARDED);
        }
    }

    // 新增：发射方法
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3 vec3d = (new Vec3(x, y, z)).normalize().add(
                this.random.nextGaussian() * 0.007499999832361937 * (double)inaccuracy,
                this.random.nextGaussian() * 0.007499999832361937 * (double)inaccuracy,
                this.random.nextGaussian() * 0.007499999832361937 * (double)inaccuracy
        ).scale((double)velocity);

        this.setDeltaMovement(vec3d);
        // 在构造函数或 shoot 方法中添加
        this.setPierce((byte) 127);
        this.initialDirection = vec3d;
        float f = Mth.sqrt((float)vec3d.horizontalDistanceSqr());
        this.setPos(this.position());

        this.initialSpeed = velocity;

        // 计算并设置初始旋转角度
        this.initialYaw = (float)(Mth.atan2(vec3d.x, vec3d.z) * 57.2957763671875F);
        this.initialPitch = (float)(Mth.atan2(vec3d.y, (double)f) * 57.2957763671875F);

        // 应用初始旋转
        this.setYRot(initialYaw);
        this.setXRot(initialPitch);
        this.yRotO = initialYaw;
        this.xRotO = initialPitch;

        this.entityData.set(INITIAL_YAW, initialYaw);
        this.entityData.set(INITIAL_PITCH, initialPitch);
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

    // 禁用父类垃圾射线检测（只打中心点），完全走我们的区域判定
    @Override
    @Nullable
    protected EntityHitResult getRayTrace(Vec3 startVec, Vec3 endVec) {
        return null;
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
            damageValue *= (float) living.getAttributeValue(Attributes.ATTACK_DAMAGE);

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
        if (this.isNoClip()) {
            return;
        }
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

    // 使用父类方法操作标志位
    public void setCritical(boolean critical) {
        this.setIsCritical(critical);
    }

    public boolean isCritical() {
        return this.getIsCritical();
    }

    public void setNoClip(boolean noClip) {
        super.setNoClip(noClip);
    }

    @Override
    public boolean isNoClip() {
        // 延迟期强制NoClip，非延迟期禁用父类NoClip（避免冲突）
        return this.indelay || super.isNoClip();
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