package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

import java.util.HashSet;
import java.util.Set;

public class EntityStormSword extends EntityNRBlisteringSword{
    // 爆炸冷却机制：防止同一实体在同一tick内多次爆炸
    private static final Set<Entity> explodedEntitiesThisTick = new HashSet<>();
    private static long lastTickProcessed = -1;

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
            long currentTick = level.getGameTime();

            // 每tick重置冷却列表
            if (currentTick != lastTickProcessed) {
                explodedEntitiesThisTick.clear();
                lastTickProcessed = currentTick;
            }
            if (!(explodedEntitiesThisTick.contains(entity))) {
                explodedEntitiesThisTick.add(entity);
                if (!(entity instanceof LivingEntity)) return;
                Explosion explosion = level.explode(null, entity.getEyePosition().x,
                        entity.getEyePosition().y, entity.getEyePosition().z, 0.1f, true, Level.ExplosionInteraction.NONE);
                explosion.explode();
            }
        }
        super.onHitEntity(result);
    }

    public static void spawnSwords(LivingEntity owner, Level world, Vec3 centerPos, SpawnMode mode, int count,
                                   boolean change , float diyYaw, float diyPitch, float zj , float k,
                                   double damage, int colorCode, boolean clip , int delay) {
        for (int i = 0; i < count; i++) {
            EntityStormSword sword = new EntityStormSword(NrEntitiesRegistry.Storm_Sword, world);
            if (diyPitch != 0 && diyYaw != 0) {
                sword.setYAW(diyYaw + k * i + zj);
                sword.setPITCH(diyPitch + k * i + zj);
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


            if (mode == SpawnMode.RANDOM) {
                Vec3 initPos = calculateInitialRandomPos(centerPos);
                sword.setPos(initPos);
            } else {
                double radius = 3.0;
                double angle = Math.PI * 2 * i / count;
                Vec3 pos = centerPos.add(
                        radius * Math.cos(angle), 0, radius * Math.sin(angle));
                sword.setPos(pos.x, pos.y, pos.z);
            }
            if (diyPitch == 0 && diyYaw == 0) {
                sword.setYRot(-owner.getYHeadRot());
                sword.setXRot(-owner.getXRot());
            } else {
                sword.setYRot(-(sword.getDiyPitch() + k * i + zj));
                sword.setXRot(-(sword.getDiyYaw() + k * i + zj));
            }
            world.addFreshEntity(sword);
        }
    }

    private static Vec3 calculateInitialRandomPos(Vec3 center) {
        double radius = 1.5;
        double angle = Math.PI * 2 * Math.random();
        return center.add(
                radius * Math.cos(angle), 0, radius * Math.sin(angle));
    }
}
