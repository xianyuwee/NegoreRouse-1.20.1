package net.xianyu.prinegorerouse.entity;

import com.google.common.collect.Lists;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
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

public class EntityDrive_5ye extends EntityAbstractSummonedSword {
    private static final EntityDataAccessor<Integer> COLOR;
    private static final EntityDataAccessor<Integer> FLAGS;
    private static final EntityDataAccessor<Float> RANK;
    private static final EntityDataAccessor<Float> ROTATION_OFFSET;
    private static final EntityDataAccessor<Float> ROTATION_ROLL;
    private static final EntityDataAccessor<Float> BASESIZE;
    private static final EntityDataAccessor<Float> SPEED;
    private static final EntityDataAccessor<Float> LIFETIME;
    private KnockBacks action;
    private double damage;
    private final List<Entity> alreadyHits;

    public KnockBacks getKnockBack() {
        return this.action;
    }

    public void setKnockBack(KnockBacks action) {
        this.action = action;
    }

    public void setKnockBackOrdinal(int ordinal) {
        if (0 <= ordinal && ordinal < KnockBacks.values().length) {
            this.action = KnockBacks.values()[ordinal];
        } else {
            this.action = KnockBacks.cancel;
        }

    }

    public EntityDrive_5ye(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        this.action = KnockBacks.cancel;
        this.damage = 7.0;
        this.alreadyHits = Lists.newArrayList();
        this.setNoGravity(true);
    }

    public static EntityDrive_5ye createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityDrive_5ye(NrEntitiesRegistry.Drive5_ye, worldIn);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COLOR, 0x3333FF);
        this.entityData.define(FLAGS, 0);
        this.entityData.define(RANK, 0.0F);
        this.entityData.define(LIFETIME, 10.0F);
        this.entityData.define(ROTATION_OFFSET, 0.0F);
        this.entityData.define(ROTATION_ROLL, 0.0F);
        this.entityData.define(BASESIZE, 1.0F);
        this.entityData.define(SPEED, 0.5F);
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        NBTHelper.getNBTCoupler(compound).put("RotationOffset", new Float[]{this.getRotationOffset()}).put("RotationRoll", new Float[]{this.getRotationRoll()}).put("BaseSize", new Float[]{this.getBaseSize()}).put("Speed", new Float[]{this.getSpeed()}).put("Color", new Integer[]{this.getColor()}).put("Rank", new Float[]{this.getRank()}).put("damage", new Double[]{this.damage}).put("crit", new Boolean[]{this.getIsCritical()}).put("clip", new Boolean[]{this.isNoClip()}).put("Lifetime", new Float[]{this.getLifetime()}).put("Knockback", new Integer[]{this.getKnockBack().ordinal()});
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        NBTHelper.getNBTCoupler(compound).get("RotationOffset", this::setRotationOffset, new Float[0]).get("RotationRoll", this::setRotationRoll, new Float[0]).get("BaseSize", this::setBaseSize, new Float[0]).get("Speed", this::setSpeed, new Float[0]).get("Color", this::setColor, new Integer[0]).get("Rank", this::setRank, new Float[0]).get("damage", (v) -> {
            this.damage = v;
        }, new Double[]{this.damage}).get("crit", this::setIsCritical, new Boolean[0]).get("clip", this::setNoClip, new Boolean[0]).get("Lifetime", this::setLifetime, new Float[0]).get("Knockback", this::setKnockBackOrdinal, new Integer[0]);
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(d0)) {
            d0 = 1.0;
        }

        d0 = d0 * 64.0 * getViewScale();
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

    public void tick() {
        super.tick();
        if (this.getShooter() != null && this.tickCount % 2 == 0) {
            boolean forceHit = true;
            Entity var4 = this.getShooter();
            List hits;
            if (var4 instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity)var4;
                float ratio = (float)this.damage * (this.getIsCritical() ? 1.1F : 1.0F);
                hits = AttackManager.areaAttack(shooter, this.action.action, ratio, forceHit, false, true, this.alreadyHits);
            } else {
                hits = AttackManager.areaAttack(this, this.action.action, 4.0, forceHit, false, this.alreadyHits);
            }

            this.alreadyHits.addAll(hits);
        }

        this.tryDespawn();
    }

    public List<Entity> getAlreadyHits() {
        return this.alreadyHits;
    }

    protected void tryDespawn() {
        if (!this.level().isClientSide() && this.getLifetime() < (float)this.tickCount) {
            this.remove(RemovalReason.DISCARDED);
        }

    }

    public int getColor() {
        return (Integer)this.getEntityData().get(COLOR);
    }

    public void setColor(int value) {
        this.getEntityData().set(COLOR, value);
    }

    public float getRank() {
        return (Float)this.getEntityData().get(RANK);
    }

    public void setRank(float value) {
        this.getEntityData().set(RANK, value);
    }

    public IConcentrationRank.ConcentrationRanks getRankCode() {
        return IConcentrationRank.ConcentrationRanks.getRankFromLevel(this.getRank());
    }

    public float getRotationOffset() {
        return (Float)this.getEntityData().get(ROTATION_OFFSET);
    }

    public void setRotationOffset(float value) {
        this.getEntityData().set(ROTATION_OFFSET, value);
    }

    public float getRotationRoll() {
        return (Float)this.getEntityData().get(ROTATION_ROLL);
    }

    public void setRotationRoll(float value) {
        this.getEntityData().set(ROTATION_ROLL, value);
    }

    public float getBaseSize() {
        return (Float)this.getEntityData().get(BASESIZE);
    }

    public void setBaseSize(float value) {
        this.getEntityData().set(BASESIZE, value);
    }

    public float getSpeed() {
        return (Float)this.getEntityData().get(SPEED);
    }

    public void setSpeed(float value) {
        this.getEntityData().set(SPEED, value);
    }

    public float getLifetime() {
        return (Float)this.getEntityData().get(LIFETIME);
    }

    public void setLifetime(float value) {
        this.getEntityData().set(LIFETIME, value);
    }

    @Nullable
    public Entity getShooter() {
        return this.getOwner();
    }

    public void setShooter(Entity shooter) {
        this.setOwner(shooter);
    }

    public List<MobEffectInstance> getPotionEffects() {
        List<MobEffectInstance> effects = PotionUtils.getAllEffects(this.getPersistentData());
        if (effects.isEmpty()) {
            effects.add(new MobEffectInstance(MobEffects.POISON, 1, 1));
        }

        return effects;
    }

    public void setDamage(double damageIn) {
        this.damage = damageIn;
    }

    public double getDamage() {
        return this.damage;
    }

    protected void onHitEntity(EntityHitResult p_213868_1_) {
        Entity targetEntity = p_213868_1_.getEntity();
        int i = Mth.ceil(this.getDamage());
        if (this.getIsCritical()) {
            i += this.random.nextInt(i / 2 + 2);
        }

        Entity shooter = this.getShooter();
        DamageSource damagesource;
        if (shooter == null) {
            damagesource = this.damageSources().indirectMagic(this, this);
        } else {
            damagesource = this.damageSources().indirectMagic(this, shooter);
            if (shooter instanceof LivingEntity) {
                Entity hits = targetEntity;
                if (targetEntity instanceof PartEntity) {
                    hits = ((PartEntity)targetEntity).getParent();
                }

                ((LivingEntity)shooter).setLastHurtMob(hits);
            }
        }

        int fireTime = targetEntity.getRemainingFireTicks();
        if (this.isOnFire() && !(targetEntity instanceof EnderMan)) {
            targetEntity.setSecondsOnFire(5);
        }

        targetEntity.invulnerableTime = 0;
        if (targetEntity.hurt(damagesource, (float)i)) {
            Entity hits = targetEntity;
            if (targetEntity instanceof PartEntity) {
                hits = ((PartEntity)targetEntity).getParent();
            }

            if (hits instanceof LivingEntity) {
                LivingEntity targetLivingEntity = (LivingEntity)hits;
                StunManager.setStun(targetLivingEntity);
                if (!this.level().isClientSide() && shooter instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(targetLivingEntity, shooter);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity)shooter, targetLivingEntity);
                }

                this.affectEntity(targetLivingEntity, this.getPotionEffects(), 1.0);
                if (shooter != null && targetLivingEntity != shooter && targetLivingEntity instanceof Player && shooter instanceof ServerPlayer) {
                    ((ServerPlayer)shooter).playNotifySound(this.getHitEntityPlayerSound(), SoundSource.PLAYERS, 0.18F, 0.45F);
                }
            }

            this.playSound(this.getHitEntitySound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        } else {
            targetEntity.setRemainingFireTicks(fireTime);
        }

    }

    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        this.setRemoved(RemovalReason.DISCARDED);
    }

    @Nullable
    public EntityHitResult getRayTrace(Vec3 p_213866_1_, Vec3 p_213866_2_) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, p_213866_1_, p_213866_2_, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), (entity) -> {
            return !entity.isSpectator() && entity.isAlive() && entity.isPickable() && entity != this.getShooter();
        });
    }

    static {
        COLOR = SynchedEntityData.defineId(EntityDrive_5ye.class, EntityDataSerializers.INT);
        FLAGS = SynchedEntityData.defineId(EntityDrive_5ye.class, EntityDataSerializers.INT);
        RANK = SynchedEntityData.defineId(EntityDrive_5ye.class, EntityDataSerializers.FLOAT);
        ROTATION_OFFSET = SynchedEntityData.defineId(EntityDrive_5ye.class, EntityDataSerializers.FLOAT);
        ROTATION_ROLL = SynchedEntityData.defineId(EntityDrive_5ye.class, EntityDataSerializers.FLOAT);
        BASESIZE = SynchedEntityData.defineId(EntityDrive_5ye.class, EntityDataSerializers.FLOAT);
        SPEED = SynchedEntityData.defineId(EntityDrive_5ye.class, EntityDataSerializers.FLOAT);
        LIFETIME = SynchedEntityData.defineId(EntityDrive_5ye.class, EntityDataSerializers.FLOAT);
    }
}
