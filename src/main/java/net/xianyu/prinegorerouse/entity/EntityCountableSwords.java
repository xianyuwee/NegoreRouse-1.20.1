package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityCountableSwords extends EntityNRBlisteringSword{
    // 添加缺失的字段
    private boolean useCustomDirection;
    private boolean autoTargeting;
    private int lifeTime;
    
    public EntityCountableSwords(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
    }

    public static EntityCountableSwords createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityCountableSwords(NrEntitiesRegistry.Countable_Sword, worldIn);
    }

    // 添加缺失的方法
    public void setUseCustomDirection(boolean useCustomDirection) {
        this.useCustomDirection = useCustomDirection;
    }
    
    public boolean isUseCustomDirection() {
        return this.useCustomDirection;
    }
    
    public void setAutoTargeting(boolean autoTargeting) {
        this.autoTargeting = autoTargeting;
    }
    
    public boolean isAutoTargeting() {
        return this.autoTargeting;
    }
    
    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }
    
    public int getLifeTime() {
        return this.lifeTime;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Level level = target.level();
        if (!level.isClientSide()) {
            if (!(target instanceof LivingEntity living)) return;
            Vec3 position = target.getEyePosition();
            BlockPos blockPos = target.getOnPos();
            int count_25 = 10;
            int count_10 = 10;
            int count_5 = 10;
            int count_1 = 10;
            if (this.level().isLoaded(blockPos)) {
                float health = living.getHealth();
                double damage = this.getDamage();
                if (health - damage <= 0) {
                    int sup_damage = 25 * count_25 + 10 * count_10 + 5 * count_5 + count_1;
                    int over_damage = (int) Math.ceil(damage - health);
                    if (over_damage >= sup_damage) {
                        over_damage = sup_damage;
                    }
                    for (int i = 1; i <= count_25; i++) {
                        if (over_damage - 25 <= 0) break;
                        over_damage = over_damage - 25;
                        spawnSwords((LivingEntity) this.getOwner(), level, position, SpawnMode.RANDOM, 1, false, 0, 0,0
                                ,0 ,25, 111111111, false, 5);
                    }
                    for (int i = 1; i <= count_10; i++) {
                        if (over_damage - 10 <= 0) break;
                        over_damage = over_damage - 10;
                        spawnSwords((LivingEntity) this.getOwner(), level, position, SpawnMode.RANDOM, 1, false, 0, 0,0
                                ,0 ,10, 123123123, false, 5);
                    }
                    for (int i = 1; i <= count_5; i++) {
                        if (over_damage - 5 <= 0) break;
                        over_damage = over_damage - 5;
                        spawnSwords((LivingEntity) this.getOwner(), level, position, SpawnMode.RANDOM, 1, false, 0, 0,0
                                ,0 ,5, -111111111, false, 5);
                    }
                    for (int i = 1; i <= count_1; i++) {
                        if (over_damage - 1 <= 0) break;
                        over_damage = over_damage - 1;
                        spawnSwords((LivingEntity) this.getOwner(), level, position, SpawnMode.RANDOM, 1, false, 0, 0,0
                                ,0 ,1, -123123123, false, 5);
                    }
                }
            }
        }
        super.onHitEntity(result);
    }

    public static void spawnSwords(LivingEntity owner, Level world, Vec3 centerPos, SpawnMode mode, int count,
                                   boolean change , float diyYaw, float diyPitch, float zj , float k,
                                   double damage, int colorCode, boolean clip , int delay) {
        for (int i = 0; i < count; i++) {
            EntityCountableSwords sword = new EntityCountableSwords(NrEntitiesRegistry.Countable_Sword, world);
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