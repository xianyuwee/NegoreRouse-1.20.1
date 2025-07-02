package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityLineDrive extends EntityNRDrive{
    public EntityLineDrive(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }
    public static EntityLineDrive createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityLineDrive(NrEntitiesRegistry.LineDrive, worldIn);
    }



    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    this.getX(), this.getY(), this.getZ(), 3, 0.1, 0.1, 0.1, 0.2);
        }
    }

    @Override
    public void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity entity)) return;
        if (entity.isAlive()) {
            if (entity instanceof Player) {
                entity.addEffect(new MobEffectInstance(MobEffects.HARM), entity);
            } else {
                entity.setHealth(0.8F * entity.getHealth());
            }
        }
    }
}
