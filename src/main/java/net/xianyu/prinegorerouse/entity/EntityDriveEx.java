package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

import java.util.Random;

public class EntityDriveEx extends EntityNRDrive {
    private static final EntityDataAccessor<Float> ROTATION_YAW;
    private static final EntityDataAccessor<Float> ROTATION_PITCH;
    private static final EntityDataAccessor<String> PARTICLE;

    public EntityDriveEx(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public static EntityDriveEx createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityDriveEx(NrEntitiesRegistry.DriveEx, worldIn);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ROTATION_YAW, 0.0F);
        this.entityData.define(ROTATION_PITCH, 0.0F);
        this.entityData.define(PARTICLE, "");
    }


    public String getParticle() {
        return this.getEntityData().get(PARTICLE);
    }
    public void setParticle(SimpleParticleType value) {
        this.getEntityData().set(PARTICLE, value.toString());
    }

    static {
        ROTATION_YAW = SynchedEntityData.defineId(EntityDriveEx.class, EntityDataSerializers.FLOAT);
        ROTATION_PITCH = SynchedEntityData.defineId(EntityDriveEx.class, EntityDataSerializers.FLOAT);
        PARTICLE = SynchedEntityData.defineId(EntityDriveEx.class, EntityDataSerializers.STRING);
    }

    public void playParticle() {
        String particle = getParticle();
        if (particle != null && !particle.isEmpty()) {
            Random random1 = new Random();
            double var2 = random1.nextGaussian() * 0.02D;
            double var4 = random1.nextGaussian() * 0.02D;
            double var6 = random1.nextGaussian() * 0.02D;
            double var8 = 10.0D;
            this.level().addParticle(ParticleTypes.LAVA.getType(), this.position().x + (random1.nextFloat() * this.getBbWidth() * 2.0F) - this.getBbWidth() - var2 * var8,
                    this.position().y + (random1.nextFloat() * this.getEyeHeight() - var4 * var8),
                    this.position().z + (random1.nextFloat() * this.getBbWidth() * 2.0F) - this.getBbWidth() - var6 * var8,
                    var2, var4, var6);
        }
    }
}
