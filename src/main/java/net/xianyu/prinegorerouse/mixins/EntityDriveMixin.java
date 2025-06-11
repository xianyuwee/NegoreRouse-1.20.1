package net.xianyu.prinegorerouse.mixins;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityDrive.class)
public abstract class EntityDriveMixin {

    @ModifyVariable(
            method = "onHitEntity",
            at = @At(value = "STORE", ordinal = 0),
            ordinal = 0,
            name = "damageValue"
    )
    private float overrideDamageValue(float original, EntityHitResult entityHitResult) {
        EntityDrive self = (EntityDrive) (Object) this;
        return (float) self.getDamage(); // 直接返回getDamage()的浮点值
    }
}