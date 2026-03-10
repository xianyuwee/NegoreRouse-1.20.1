package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityNRJudgementCut extends EntityJudgementCut {
    private static final EntityDataAccessor<Float> RENDER_SCALE =
            SynchedEntityData.defineId(EntityNRJudgementCut.class, EntityDataSerializers.FLOAT);
    // 新增：自定义伤害存储（避免依赖generic.attack_damage属性）
    private static final EntityDataAccessor<Float> CUSTOM_DAMAGE =
            SynchedEntityData.defineId(EntityNRJudgementCut.class, EntityDataSerializers.FLOAT);
    // 最大生命周期（防止实体无限存活）
    private static final int MAX_LIFETIME = 200;

    public EntityNRJudgementCut(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        this.entityData.set(RENDER_SCALE, 0.1f);
        this.entityData.set(CUSTOM_DAMAGE, 0.0f); // 初始化自定义伤害
        // 限制默认生命周期
        this.setLifetime(Math.min(this.getLifetime(), MAX_LIFETIME));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RENDER_SCALE, 0.1f);
        this.entityData.define(CUSTOM_DAMAGE, 0.0f); // 注册自定义伤害数据
    }

    // 重写setDamage，使用自定义存储而非属性
    @Override
    public void setDamage(double damage) {
        this.entityData.set(CUSTOM_DAMAGE, (float) Math.min(damage, 50)); // 上限50
    }

    // 重写getDamage，读取自定义存储
    @Override
    public double getDamage() {
        return this.entityData.get(CUSTOM_DAMAGE);
    }

    public void setRenderScale(float scale) {
        // 限制渲染缩放上限，避免客户端渲染压力
        this.entityData.set(RENDER_SCALE, Math.min(scale, 1.0f));
    }

    public float getRenderScale() {
        return this.entityData.get(RENDER_SCALE);
    }

    // 新增：获取主实体（处理末影龙部件）
    private Entity getMainEntity(Entity entity) {
        if (entity instanceof EnderDragonPart part) {
            return part.getParent(); // 返回末影龙主实体
        }
        // 兼容其他多部分实体
        if (entity != null) {
            try {
                var parentField = entity.getClass().getDeclaredField("parent");
                parentField.setAccessible(true);
                Object parent = parentField.get(entity);
                if (parent instanceof Entity) {
                    return (Entity) parent;
                }
            } catch (Exception e) {
                // 忽略反射异常
            }
        }
        return entity;
    }

    // 重写tick，核心加固：shooter合法性校验，无效直接销毁，规避父类属性读取崩溃
    @Override
    public void tick() {
        // 客户端仅执行基础tick，减少渲染开销
        if (this.level().isClientSide()) {
            super.tick();
            return;
        }
        // 核心加固：校验shooter必须是存活的LivingEntity，无效直接销毁实体，彻底规避崩溃
        Entity shooter = this.getShooter();
        if (!(shooter instanceof LivingEntity livingShooter) || !livingShooter.isAlive()) {
            this.discard();
            return;
        }
        // 服务端：生命周期到直接销毁，释放资源
        if (this.getLifetime() <= 0 || this.tickCount > MAX_LIFETIME) {
            this.discard();
            return;
        }
        // 修复：处理射击者为末影龙部件的情况
        if (shooter instanceof EnderDragonPart) {
            this.setShooter(getMainEntity(shooter));
        }
        super.tick();
    }

    // 关键修复：重写正确的onHitEntity方法（参数为EntityHitResult）
    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        // 1. 从EntityHitResult中提取命中的原始实体
        Entity rawTarget = hitResult.getEntity();
        if (rawTarget == null || rawTarget.level().isClientSide()) {
            return;
        }
        // 2. 获取主实体（处理末影龙部件）
        Entity mainTarget = getMainEntity(rawTarget);
        // 3. 伤害逻辑处理
        if (mainTarget instanceof EnderDragon dragon) {
            // 末影龙特殊处理：直接调用hurt方法，使用通用伤害源
            dragon.hurt(this.damageSources().magic(), (float) this.getDamage());
        }
        // 普通生物：强转并校验LivingEntity
        else if (mainTarget instanceof LivingEntity living && living.isAlive()) {
            living.hurt(this.damageSources().magic(), (float) this.getDamage());
        }
        // 4. 调用父类逻辑（传入正确的EntityHitResult）
        // 若需要让父类处理主实体，需重新构建EntityHitResult
        EntityHitResult mainHitResult = new EntityHitResult(mainTarget);
        super.onHitEntity(mainHitResult);
    }

    public static EntityNRJudgementCut createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityNRJudgementCut(NrEntitiesRegistry.NRJudgementCut, worldIn);
    }
}