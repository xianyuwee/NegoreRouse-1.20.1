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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

import javax.annotation.Nullable;
import java.util.List;

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
    private static final EntityDataAccessor<Float> LIFETIME = SynchedEntityData.defineId(EntityNRDrive.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(EntityNRDrive.class, EntityDataSerializers.INT);;

    private KnockBacks action = KnockBacks.cancel;

    private double damage = 7.0D;

    // 新增：存储初始旋转角度
    private float initialYaw;
    private float initialPitch;

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
        this.initialYaw = this.getYRot();
        this.initialPitch = this.getXRot();
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
                .put("InitialPitch", this.initialPitch);
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
                .get("InitialPitch", this::setInitialPitch, 0f);
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
    public void tick() {
        // 保存自定义旋转角度
        float customYaw = this.getRotationOffset();
        float customRoll = this.getRotationRoll();

        // 强制应用自定义旋转角度，避免父类重置
        this.setYRot(initialYaw); // 使用初始偏航角
        this.yRotO = initialYaw;  // 更新上一帧旋转角度
        this.setXRot(initialPitch); // 使用初始俯仰角
        this.xRotO = initialPitch; // 更新上一帧俯仰角
        this.setRotationRoll(customRoll); // 应用自定义旋转滚动
        this.setRotationOffset(customYaw);

        super.tick();
        tryDespawn();
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
        float f = Mth.sqrt((float)vec3d.horizontalDistanceSqr());
        this.setPos(this.position());

        // 计算并设置初始旋转角度
        this.initialYaw = (float)(Mth.atan2(vec3d.x, vec3d.z) * 57.2957763671875F);
        this.initialPitch = (float)(Mth.atan2(vec3d.y, (double)f) * 57.2957763671875F);

        // 应用初始旋转
        this.setYRot(initialYaw);
        this.setXRot(initialPitch);
        this.yRotO = initialYaw;
        this.xRotO = initialPitch;
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
        float damageValue = (float) this.getDamage();

        Entity shooter = this.getShooter();
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

    public boolean isNoClip() {
        return super.isNoClip();
    }
}