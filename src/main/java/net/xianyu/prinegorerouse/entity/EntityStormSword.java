package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityStormSword extends EntityBlisteringSword{
    public EntityStormSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public static EntityStormSword createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityStormSword(NrEntitiesRegistry.Storm_Sword, worldIn);
    }

    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Level level = entity.level();
        if (!level.isClientSide()) {
            Explosion explosion = level.explode(null, entity.getEyePosition().x,
                    entity.getEyePosition().y, entity.getEyePosition().z, 0.1f, true, Level.ExplosionInteraction.NONE);
            explosion.explode();
        }
        super.onHitEntity(result);
    }
}
