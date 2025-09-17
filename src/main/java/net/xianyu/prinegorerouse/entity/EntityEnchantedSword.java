package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityEnchantedSword extends EntityNRBlisteringSword {
    public EntityEnchantedSword(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
    }

    public static EntityEnchantedSword createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, worldIn);
    }

    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Level level = entity.level();
        if (!level.isClientSide()) {
            BlockPos blockPos = entity.blockPosition();
            if (level.canSeeSky(blockPos)) {
                LightningBolt lightningBolt1 = (LightningBolt) EntityType.LIGHTNING_BOLT.create(level);
                LightningBolt lightningBolt2 = (LightningBolt) EntityType.LIGHTNING_BOLT.create(level);
                if (lightningBolt1 != null && lightningBolt2 != null) {
                    lightningBolt1.setVisualOnly(true);
                    lightningBolt2.setDamage(50.0F);
                    lightningBolt1.moveTo(entity.position());
                    if (getOwner() instanceof ServerPlayer) {
                        lightningBolt1.setCause((ServerPlayer) getOwner());
                        lightningBolt2.setCause((ServerPlayer) getOwner());
                    }
                    level.addFreshEntity(lightningBolt1);
                    entity.thunderHit((ServerLevel) level, lightningBolt2);
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 5.0F, 1.0F);
                }
            }
        }
        super.onHitEntity(result);
    }

    public static void spawnSwords(LivingEntity owner, Level world, Vec3 centerPos, EntityNRBlisteringSword.SpawnMode mode, int count, boolean change, float diyYaw, float diyPitch, float zj, float k, double damage, int colorCode, boolean clip, int delay) {
        for (int i = 0; i < count; i++) {
            EntityEnchantedSword sword = new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, world);
            if (diyPitch != 0.0F && diyYaw != 0.0F) {
                sword.setYAW(Float.valueOf(diyYaw + k * i + zj));
                sword.setPITCH(Float.valueOf(diyPitch + k * i + zj));
            }
            sword.setOwner(owner);
            sword.setCenterPosition(centerPos);
            sword.spawnMode = mode;
            sword.lifeTime = 200;
            sword.setChange(change);
            sword.setDamage(damage);
            sword.setColor(colorCode);
            sword.setNoClip(clip);
            sword.setDelayTicks(delay);
            if (mode == EntityNRBlisteringSword.SpawnMode.RANDOM) {
                Vec3 initPos = calculateInitialRandomPos(centerPos);
                sword.moveTo(initPos);
            } else {
                double radius = 3.0D;
                double angle = 6.283185307179586D * i / count;
                Vec3 pos = centerPos.add(radius * Math.cos(angle), 0.0D, radius * Math.sin(angle));
                sword.setPos(pos.x, pos.y, pos.z);
            }
            if (diyPitch == 0.0F && diyYaw == 0.0F) {
                sword.setYRot(-owner.getYRot());
                sword.setXRot(-owner.getXRot());
            } else {
                sword.setYRot(-(sword.getDiyPitch() + k * i + zj));
                sword.setXRot(-(sword.getDiyYaw() + k * i + zj));
            }
            world.addFreshEntity(sword);
        }
    }

    private static Vec3 calculateInitialRandomPos(Vec3 center) {
        double radius = 1.5D;
        double angle = 6.283185307179586D * Math.random();
        return center.add(radius * Math.cos(angle), 0.0D, radius * Math.sin(angle));
    }
}