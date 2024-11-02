package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityDriveEx extends EntityDrive {
    public EntityDriveEx(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public static EntityDriveEx createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityDriveEx(NrEntitiesRegistry.DriveEx, worldIn);
    }
}
