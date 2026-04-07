package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityCountableSwords extends EntityNRBlisteringSword {
    // NBT标签键：标记是否为"溢出伤害生成的剑"
    private static final String TAG_IS_CHAIN_SWORD = "IsChainSword";
    private boolean isChainSword = false;

    public EntityCountableSwords(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
    }

    public static EntityCountableSwords createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityCountableSwords(NrEntitiesRegistry.Countable_Sword, worldIn);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 1. 先调用父类逻辑（如果父类有基础伤害处理，需确保顺序）
        super.onHitEntity(result);

        Entity target = result.getEntity();
        Level level = target.level();
        if (level.isClientSide()) return;

        // 2. 安全检查：目标必须是生物，且自己不是"连锁剑"
        if (!(target instanceof LivingEntity living)) return;
        if (this.isChainSword) return; // 【关键】连锁剑不再触发新的连锁

        // 3. 安全检查：所有者必须存在且是生物
        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) return;

        Vec3 position = target.getEyePosition();
        BlockPos blockPos = target.getOnPos();
        if (!level.isLoaded(blockPos)) return;

        float health = living.getHealth();
        double damage = this.getDamage();

        // 4. 只有当伤害足以击杀目标时才处理溢出
        if (health - damage > 0) return;

        // 计算溢出伤害（向上取整）
        int overDamage = (int) Math.ceil(damage - health);
        if (overDamage <= 0) return;

        // 5. 按面额生成剑（修复后的循环逻辑）
        // 25伤害面额
        int max25 = Math.min(overDamage / 25, 10); // 最多10把，防止瞬间过多
        for (int i = 0; i < max25; i++) {
            spawnChainSword(livingOwner, level, position, 25, 111111111);
            overDamage -= 25;
        }
        // 10伤害面额
        int max10 = Math.min(overDamage / 10, 10);
        for (int i = 0; i < max10; i++) {
            spawnChainSword(livingOwner, level, position, 10, 123123123);
            overDamage -= 10;
        }
        // 5伤害面额
        int max5 = Math.min(overDamage / 5, 10);
        for (int i = 0; i < max5; i++) {
            spawnChainSword(livingOwner, level, position, 5, -111111111);
            overDamage -= 5;
        }
        // 1伤害面额（剩余所有伤害）
        int max1 = Math.min(overDamage, 10);
        for (int i = 0; i < max1; i++) {
            spawnChainSword(livingOwner, level, position, 1, -123123123);
            overDamage -= 1;
        }
    }

    /**
     * 专用的连锁剑生成方法
     */
    private void spawnChainSword(LivingEntity owner, Level world, Vec3 centerPos, double damage, int colorCode) {
        EntityCountableSwords sword = new EntityCountableSwords(NrEntitiesRegistry.Countable_Sword, world);

        // 标记为连锁剑，防止递归
        sword.isChainSword = true;

        sword.setOwner(owner);
        sword.setCenterPosition(centerPos);
        sword.spawnMode = SpawnMode.RANDOM;
        sword.lifeTime = 100; // 连锁剑生命周期缩短，避免堆积
        sword.setChange(false);
        sword.setDamage(damage);
        sword.setColor(colorCode);
        sword.setNoClip(true); // 强制穿透

        // 设置随机位置
        Vec3 initPos = calculateInitialRandomPos(centerPos);
        sword.setPos(initPos);

        // 计算并设置初始运动方向：朝向中心点
        Vec3 toCenter = centerPos.subtract(initPos).normalize();

        // 1. 先设置 DeltaMovement
        sword.setDeltaMovement(toCenter.scale(1.5F));

        // 2. 后设置延迟 (父类会在 tick 里自动把 DeltaMovement 赋值给 initialDirection)
        sword.setDelayTicks(5);

        // 设置朝向
        double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);
        float yaw = (float) Math.toDegrees(Math.atan2(toCenter.z, toCenter.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(toCenter.y, horizontalDistance));
        sword.setYRot(yaw);
        sword.setXRot(pitch);

        world.addFreshEntity(sword);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_IS_CHAIN_SWORD, this.isChainSword);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_IS_CHAIN_SWORD)) {
            this.isChainSword = tag.getBoolean(TAG_IS_CHAIN_SWORD);
        }
    }

    // 保留原有的 spawnSwords 供外部调用（非连锁场景）
    public static void spawnSwords(LivingEntity owner, Level world, Vec3 centerPos, SpawnMode mode, int count,
                                   boolean change, float diyYaw, float diyPitch, float zj, float k,
                                   double damage, int colorCode, boolean clip, int delay) {
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
        // 给Y轴也加一点随机，避免完全在同一平面
        double yOffset = (Math.random() - 0.5) * 1.0;
        return center.add(
                radius * Math.cos(angle), yOffset, radius * Math.sin(angle));
    }
}