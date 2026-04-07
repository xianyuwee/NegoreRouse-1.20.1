package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

import static mods.flammpfeil.slashblade.SlashBladeConfig.REFINE_DAMAGE_MULTIPLIER;
import static mods.flammpfeil.slashblade.SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER;

//用原版模型
public class EntityNRSDrive extends EntityDrive {
    // ========== 关键修改1：新增InitialYaw/Pitch的同步器 ==========
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RANK = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_OFFSET = SynchedEntityData
            .defineId(EntityNRSDrive.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_ROLL = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASESIZE = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LIFETIME = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DELAYTICK = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DELAYSPEED = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IN_DELAY = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(EntityNRSDrive.class, EntityDataSerializers.INT);
    // 新增：InitialYaw/Pitch同步器
    private static final EntityDataAccessor<Float> INITIAL_YAW = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> INITIAL_PITCH = SynchedEntityData.defineId(EntityNRSDrive.class,
            EntityDataSerializers.FLOAT);

    private KnockBacks action = KnockBacks.cancel;
    private double damage = 7.0D;

    // 改为通过同步器访问，不再用普通字段
    private int delayTicks;
    private Vec3 initialVelocity;
    private int remainingDelayTicks;
    private boolean indelay = true;
    private Vec3 initialDirection = Vec3.ZERO;
    private float initialSpeed = 0;

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

    public EntityNRSDrive(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        this.setNoGravity(true);
        this.delayTicks = this.getDelayTick();
        // 初始化同步数据
        this.setInitialYaw(this.getYRot());
        this.setInitialPitch(this.getXRot());
        this.initialSpeed = this.getSpeed();
    }

    public static EntityNRSDrive createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityNRSDrive(NrEntitiesRegistry.NRSDrive, worldIn);
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
        // 初始化新增的同步数据
        this.entityData.define(INITIAL_YAW, 0.0f);
        this.entityData.define(INITIAL_PITCH, 0.0f);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        NBTHelper.getNBTCoupler(compound)
                .put("RotationOffset", this.getRotationOffset())
                .put("RotationRoll", this.getRotationRoll())
                .put("BaseSize", this.getBaseSize())
                .put("Speed", this.getSpeed())
                .put("Color", this.getColor())
                .put("Rank", this.getRank())
                .put("damage", this.damage)
                .put("Lifetime", this.getLifetime())
                .put("Knockback", this.getKnockBack().ordinal())
                .put("InitialYaw", this.getInitialYaw())  // 保存同步的InitialYaw
                .put("InitialPitch", this.getInitialPitch())  // 保存同步的InitialPitch
                .put("delayTicks", this.getDelayTick())
                .put("delayspeed", this.getDelaySpeed())
                .put("indelay", this.isIndelay())
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
                .get("InitialYaw", this::setInitialYaw, 0f)  // 读取同步的InitialYaw
                .get("InitialPitch", this::setInitialPitch, 0f)  // 读取同步的InitialPitch
                .get("delayTicks", this::setDelayTick, 20)
                .get("delayspeed", this::setDelaySpeed, 0.5f)
                .get("RemainingDelayTicks", (Integer v) -> this.remainingDelayTicks = v, 0);
    }

    // ========== 关键修改2：InitialYaw/Pitch的get/set方法（关联同步器） ==========
    public float getInitialYaw() {
        return this.entityData.get(INITIAL_YAW);
    }

    public void setInitialYaw(float value) {
        this.entityData.set(INITIAL_YAW, value);
    }

    public float getInitialPitch() {
        return this.entityData.get(INITIAL_PITCH);
    }

    public void setInitialPitch(float value) {
        this.entityData.set(INITIAL_PITCH, value);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
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
            newValue = this.entityData.get(FLAGS);
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
        refreshFlags();

        // 只在首次tick时初始化延迟
        if (remainingDelayTicks == 0 && getDelayTick() > 0) {
            remainingDelayTicks = getDelayTick();
            setInDelay(true);
            this.initialDirection = this.getDeltaMovement().normalize();
        }

        // ========== 关键修改3：调整执行顺序，先执行父类tick，再强制设置旋转 ==========
        super.tick();

        if (isIndelay()) {
            // 延迟期间处理
            remainingDelayTicks--;

            // 保存自定义旋转角度
            float customYaw = this.getRotationOffset();
            float customRoll = this.getRotationRoll();

            // 强制应用同步的初始旋转角度
            float initialYaw = this.getInitialYaw();
            float initialPitch = this.getInitialPitch();
            this.setYRot(initialYaw);
            this.yRotO = initialYaw;
            this.setXRot(initialPitch);
            this.xRotO = initialPitch;
            this.setRotationRoll(customRoll);
            this.setRotationOffset(customYaw);

            float delaySpeed = this.getDelaySpeed();
            this.setDeltaMovement(initialDirection.scale(delaySpeed));
            this.setSpeed(delaySpeed);

            checkCollisions();

            // 延迟结束处理
            if (remainingDelayTicks <= 0) {
                setInDelay(false);
                remainingDelayTicks = -1;
                // 恢复原始速度
                this.setDeltaMovement(initialDirection.scale(this.initialSpeed));
            }
        } else {
            // 正常状态处理
            float customYaw = this.getRotationOffset();
            float customRoll = this.getRotationRoll();

            // 强制应用同步的初始旋转角度
            float initialYaw = this.getInitialYaw();
            float initialPitch = this.getInitialPitch();
            this.setYRot(initialYaw);
            this.yRotO = initialYaw;
            this.setXRot(initialPitch);
            this.xRotO = initialPitch;
            this.setRotationRoll(customRoll);
            this.setRotationOffset(customYaw);
            this.setSpeed(initialSpeed);
        }

        String debugMsg = String.format(
                "[NRDrive渲染调试] ID:%d | 同步InitialYaw:%.2f | InitialPitch:%.2f | 实体原生Yaw:%.2f | 原生Pitch:%.2f | Roll:%.2f | pos_x:%.2f | pos_y:%.2f | pos_z:%.2f |",
                this.getId(),
                this.getInitialYaw(), // 改用同步器获取
                this.getInitialPitch(),
                this.getYRot(),
                this.getXRot(),
                this.getRotationRoll(),
                this.getX(),
                this.getY(),
                this.getZ()
        );
        SlashBlade.LOGGER.info(debugMsg);
        tryDespawn();
    }

    private void checkCollisions() {
        // 计算新位置
        Vec3 newPos = this.position().add(this.getDeltaMovement());
        Entity shooter = this.getShooter(); // 获取释放者

        // 检测实体碰撞 - 排除释放者
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                this.level(), this, this.position(), newPos,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable()
                        && entity != shooter
        );

        if (entityHit != null) {
            this.onHitEntity(entityHit);
        }

        // 检测方块碰撞
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                this.position(), newPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
        ));

        if (blockHit.getType() != HitResult.Type.MISS) {
            this.onHitBlock(blockHit);
        }
    }

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
        this.initialDirection = vec3d;
        float f = Mth.sqrt((float)vec3d.horizontalDistanceSqr());
        this.setPos(this.position());

        this.initialSpeed = velocity;

        // 计算并设置同步的初始旋转角度
        float initialYaw = (float)(Mth.atan2(vec3d.x, vec3d.z) * 57.2957763671875F);
        float initialPitch = (float)(Mth.atan2(vec3d.y, (double)f) * 57.2957763671875F);
        this.setInitialYaw(initialYaw); // 存入同步器
        this.setInitialPitch(initialPitch);

        // 立即应用初始旋转
        this.setYRot(initialYaw);
        this.setXRot(initialPitch);
        this.yRotO = initialYaw;
        this.xRotO = initialPitch;
    }

    // ========== 原有get/set方法保持不变 ==========
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

    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity targetEntity = entityHitResult.getEntity();
        Entity shooter = this.getShooter();

        // 排除释放者
        if (targetEntity == shooter) {
            return;
        }

        float damageValue = (float) this.getDamage();
        DamageSource damagesource;

        if (shooter == null) {
            damagesource = this.damageSources().indirectMagic(this, this);
        } else {
            damagesource = this.damageSources().indirectMagic(this, shooter);
            if (shooter instanceof LivingEntity living) {
                Entity hits = targetEntity;
                if (targetEntity instanceof PartEntity) {
                    hits = ((PartEntity<?>) targetEntity).getParent();
                }
                living.setLastHurtMob(hits);
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
                            .map(ISlashBladeState::getRefine)
                            .orElse(0);
                    int level = player.experienceLevel;
                    rankDamageBonus = (float) Math.max(
                            rankDamageBonus,
                            Math.min(level, refine) * REFINE_DAMAGE_MULTIPLIER.get()
                    );
                }
                damageValue += rankDamageBonus;
            }

            damageValue *= (float) (AttackManager.getSlashBladeDamageScale(living) * SLASHBLADE_DAMAGE_MULTIPLIER.get());

            if (this.isCritical()) {
                damageValue += this.random.nextInt((Mth.ceil(damageValue) / 2 + 2));
            }
        }

        if (targetEntity.hurt(damagesource, damageValue)) {
            Entity hits = targetEntity;
            if (targetEntity instanceof PartEntity) {
                hits = ((PartEntity<?>) targetEntity).getParent();
            }

            if (hits instanceof LivingEntity targetLivingEntity) {
                StunManager.setStun(targetLivingEntity);

                if (!this.level().isClientSide() && shooter instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(targetLivingEntity, shooter);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity) shooter, targetLivingEntity);
                }

                affectEntity(targetLivingEntity, getPotionEffects(), 1.0f);

                if (targetLivingEntity != shooter && targetLivingEntity instanceof Player
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
    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        this.setRemoved(RemovalReason.DISCARDED);
    }

    @Nullable
    public EntityHitResult getRayTrace(Vec3 p_213866_1_, Vec3 p_213866_2_) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, p_213866_1_, p_213866_2_,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), (entity) -> {
                    return !entity.isSpectator() && entity.isAlive() && entity.isPickable()
                            && (entity != this.getShooter());
                });
    }

    public void setCritical(boolean critical) {
        this.setIsCritical(critical);
    }

    public boolean isCritical() {
        return this.getIsCritical();
    }

    public void setNoClip(boolean noClip) {
        super.setNoClip(noClip);
    }

    public boolean isNoClip() {
        return super.isNoClip();
    }

    public void setInDelay(boolean inDelay) {
        this.indelay = inDelay;
        this.entityData.set(IN_DELAY, inDelay); // 同步IN_DELAY状态
    }

    public boolean isIndelay() {
        return this.entityData.get(IN_DELAY); // 从同步器读取
    }

    public int getDelayTick() {
        return this.getEntityData().get(DELAYTICK);
    }

    public void setDelayTick(int tick) {
        if (this.remainingDelayTicks <= 0) {
            this.remainingDelayTicks = tick;
        }
        this.getEntityData().set(DELAYTICK, tick);
    }

    public float getDelaySpeed() {
        return this.getEntityData().get(DELAYSPEED);
    }

    public void setDelaySpeed(float delaySpeed) {
        this.getEntityData().set(DELAYSPEED, delaySpeed);
    }
}