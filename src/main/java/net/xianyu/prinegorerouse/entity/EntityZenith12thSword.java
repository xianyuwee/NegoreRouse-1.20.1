package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityZenith12thSword extends EntityBlisteringSword {
    public EntityZenith12thSword(EntityType< ? extends Projectile > entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public static EntityZenith12thSword createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityZenith12thSword(NrEntitiesRegistry.Zenith12th_Sword, worldIn);
    }

    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Level level = entity.level();
        if (!level.isClientSide()) {
            BlockPos blockPos = entity.getOnPos();
            if (this.level().isLoaded(blockPos)) {
                LightningBolt lightningBolt = (LightningBolt) EntityType.LIGHTNING_BOLT.create(this.level());
                if (lightningBolt != null) {
                    entity.thunderHit((ServerLevel) level, lightningBolt);
                    lightningBolt.setSecondsOnFire(0);
                    lightningBolt.setPos(entity.getEyePosition());
                    lightningBolt.setCause(this.getHitEntity() instanceof ServerPlayer ? (ServerPlayer) this.getHitEntity() : null);
                    this.level().addFreshEntity(lightningBolt);
                    this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 5.0F, 1.0F);
                }
            }
        }
        super.onHitEntity(result);
    }

}
