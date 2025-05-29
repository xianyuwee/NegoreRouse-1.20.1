package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.stream.Stream;

public class EntityBlisteringSword extends EntityAbstractSummonedSword {
    public static final EntityDataAccessor<Boolean> IT_FIRED;
    public static final EntityDataAccessor<Boolean> IS_LAUNCHED;
    public static final EntityDataAccessor<Float> SPEED;
    public static final EntityDataAccessor<Vector3f> OFFSET;
    long fire_time = -1L;

    public EntityBlisteringSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        this.setPierce((byte) 5);
    }

    public static EntityBlisteringSword createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityBlisteringSword(NrEntitiesRegistry.BlisteringSword, worldIn);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IT_FIRED,false);
        this.entityData.define(SPEED, 2.0F);
        this.entityData.define(OFFSET, Vec3.ZERO.toVector3f());
    }

    public void doFire() {
        this.getEntityData().set(IT_FIRED, true);
    }

    public boolean itFired() {
        return (Boolean)this.getEntityData().get(IT_FIRED);
    }

    public void Launch() {
        this.getEntityData().set(IS_LAUNCHED, true);
    }

    public boolean isLaunched() { return (boolean) this.getEntityData().get(IS_LAUNCHED);}

    public void setSpeed(float speed) {
        this.getEntityData().set(SPEED, speed);
    }

    public float getSpeed() {
        return (Float)this.getEntityData().get(SPEED);
    }

    public void setOffset(Vec3 offset) {
        this.getEntityData().set(OFFSET, offset.toVector3f());
    }

    public Vec3 getOffset() {
        return new Vec3((Vector3f) this.getEntityData().get(OFFSET));
    }

    public void tick() {
        if (!this.itFired() && this.level().isClientSide() && getVehicle() == null) {
            this.startRiding(this.getVehicle(),true);
        }

        super.tick();
    }

    public void rideTick() {
        if (this.itFired() && this.fire_time <= this.tickCount) {
            this.faceEntityStandby();
            Entity vehicle = this.getVehicle();
            Vec3 dir = this.getViewVector(0.0F);
            if (!(vehicle instanceof LivingEntity)) {
                this.shoot(dir.x, dir.y, dir.z, this.getSpeed(), 1.0F);
            } else {
                LivingEntity sender = (LivingEntity) this.getVehicle();
                this.stopRiding();
                this.tickCount = 0;
                Level worldIn = sender.level();
                Entity lockTarget = null;
                if (sender instanceof LivingEntity) {
                    lockTarget = (Entity) sender.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).filter((state) -> {
                        return state.getTargetEntity(worldIn) != null;
                    }).map((state) -> {
                        return state.getTargetEntity(worldIn);
                    }).orElse(null);
                }

                Optional<Entity> foundTarget = Stream.of(Optional.ofNullable(lockTarget), RayTraceHelper.rayTrace(sender.level(), sender, sender.getEyePosition(1.0F), sender.getLookAngle(), 12, 12.0, (e) -> {
                    return true;
                }).filter((r) -> {
                    return r.getType() == HitResult.Type.ENTITY;
                }).filter((r) -> {
                    EntityHitResult er = (EntityHitResult) r;
                    Entity target = er.getEntity();
                    boolean isMatch = true;
                    if (target instanceof LivingEntity) {
                        isMatch = TargetSelector.lockon.test(sender, (LivingEntity) target);
                    }

                    if (target instanceof IShootable) {
                        isMatch = ((IShootable) target).getShooter() != sender;
                    }

                    return isMatch;
                }).map((r) -> {
                    return ((EntityHitResult)r).getEntity();
                })).filter(Optional::isPresent).map(Optional::get).findFirst();
                Vec3 targetPos = (Vec3) foundTarget.map((e) -> {
                    return new Vec3(e.getX(), e.getY() + e.getEyeHeight() * 0.5, e.getZ());
                }).orElseGet(() -> {
                    Vec3 start = sender.getEyePosition(1.0F);
                    Vec3 end = start.add(sender.getLookAngle().scale(40.0));
                    HitResult result = worldIn.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sender));
                    return ((HitResult)result).getLocation();
                });
                Vec3 pos = this.getPosition(0.0F);
                dir = targetPos.subtract(pos).normalize();
                this.shoot(dir.x, dir.y, dir.z, this.getSpeed(), 1.0F);
                if (sender instanceof ServerPlayer) {
                    ((ServerPlayer)sender).playNotifySound(SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            if (this.canUpdate()) {
                this.baseTick();
            }

            this.faceEntityStandby();
            if (!this.itFired() && this.getVehicle() instanceof LivingEntity && this.tickCount >= this.getDelay()) {
                this.fire_time = (long) (this.tickCount + this.getDelay());
                this.doFire();
            }
        }
    }

    protected void faceEntityStandby() {
        int spawnNum = getDelay();

        boolean isRight = spawnNum % 2 == 0;
        int level = spawnNum / 2;

        Vec3 pos = new Vec3(0, 0, 0);

        if (this.getVehicle() == null) {
            doFire();
            return;
        }

        pos = pos.add(this.getVehicle().position()).add(0, this.getVehicle().getEyeHeight() * 0.8, 0);

        double xOffset = (1 - 0.1 * level) * (isRight ? 1 : -1);
        double yOffset = 0.25 * level;
        double zOffset = -0.1 * level;

        Vec3 offset = new Vec3(xOffset, yOffset, zOffset);

        offset = offset.xRot((float) Math.toRadians(-this.getVehicle().getXRot()));
        offset = offset.yRot((float) Math.toRadians(-this.getVehicle().getYRot()));

        pos = pos.add(offset);

        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();

        // ■初期位置・初期角度等の設定
        setPos(pos);

        setRot(-this.getVehicle().getYRot(), -this.getVehicle().getXRot());
    }

    protected void onHitEntity(EntityHitResult result) {
        Entity targetEntity = result.getEntity();
        if (targetEntity instanceof LivingEntity) {
            KnockBacks.cancel.action.accept((LivingEntity) targetEntity);
            StunManager.setStun((LivingEntity) targetEntity);
        }
        super.onHitEntity(result);
    }

    static {
        IT_FIRED = SynchedEntityData.defineId(EntityBlisteringSword.class, EntityDataSerializers.BOOLEAN);
        SPEED = SynchedEntityData.defineId(EntityBlisteringSword.class, EntityDataSerializers.FLOAT);
        OFFSET = SynchedEntityData.defineId(EntityBlisteringSword.class, EntityDataSerializers.VECTOR3);
        IS_LAUNCHED = SynchedEntityData.defineId(EntityBlisteringSword.class, EntityDataSerializers.BOOLEAN);
    }
}
