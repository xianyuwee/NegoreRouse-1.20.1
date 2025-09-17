package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityShinyDrive extends EntityDrive {
    // 添加缺失的字段
    private int delayTick;
    private float delaySpeed;
    
    public EntityShinyDrive(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public static EntityShinyDrive createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityShinyDrive(NrEntitiesRegistry.ShinyDrive, worldIn);
    }

    // 添加缺失的方法
    public void setDelayTick(int delayTick) {
        this.delayTick = delayTick;
    }
    
    public int getDelayTick() {
        return this.delayTick;
    }
    
    public void setDelaySpeed(float delaySpeed) {
        this.delaySpeed = delaySpeed;
    }
    
    public float getDelaySpeed() {
        return this.delaySpeed;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(), 3, 0.1, 0.1, 0.1, 0.2);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity entity)) return;
        if (entity.isAlive()) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 2));
        }
    }
}