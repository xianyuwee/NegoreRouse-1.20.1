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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityNRJudgementCut extends EntityJudgementCut {
    private static final EntityDataAccessor<Float> RENDER_SCALE =
            SynchedEntityData.defineId(EntityNRJudgementCut.class, EntityDataSerializers.FLOAT);
    // 新增：自定义伤害存储（避免依赖generic.attack_damage属性）
    private static final EntityDataAccessor<Float> CUSTOM_DAMAGE =
            SynchedEntityData.defineId(EntityNRJudgementCut.class, EntityDataSerializers.FLOAT);
    // 渲染修复核心：父类 lifetime 是私有字段且不进同步数据，客户端永远为 0，
    // 导致渲染器透明度公式除以 0 得 NaN → 整个模型不可见。这里用同步数据接管它
    //（父类内部读写均走 getLifetime/setLifetime 虚调用，override 即可全面生效）。
    private static final EntityDataAccessor<Integer> LIFETIME_SYNC =
            SynchedEntityData.defineId(EntityNRJudgementCut.class, EntityDataSerializers.INT);
    // 最大生命周期（防止实体无限存活）
    private static final int MAX_LIFETIME = 200;

    public EntityNRJudgementCut(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        this.entityData.set(RENDER_SCALE, 0.8f);
        this.entityData.set(CUSTOM_DAMAGE, 0.0f); // 初始化自定义伤害
        // 注意：不要在这里用 getLifetime() 回写——父类字段默认 0，回写会把同步值锁死在 0
    }

    // ============ 传送门适配：直接穿过，不与任何传送门交互 ============
    @Override
    protected void handleNetherPortal() {
        // 屏蔽原版下界 portal 的点燃/转换逻辑（baseTick 每 tick 调用）
    }

    @Override
    public boolean canChangeDimensions() {
        // 屏蔽下界传送门/末地传送门/末地折跃门的全部维度转移入口
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RENDER_SCALE, 0.8f);
        this.entityData.define(CUSTOM_DAMAGE, 0.0f); // 注册自定义伤害数据
        this.entityData.define(LIFETIME_SYNC, 0);
    }

    // 渲染修复：接管父类 lifetime，改走同步数据（服务端设置后客户端可见）
    @Override
    public int getLifetime() {
        return this.entityData.get(LIFETIME_SYNC);
    }

    @Override
    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME_SYNC, Mth.clamp(lifetime, 0, MAX_LIFETIME));
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
        // 缩放下限 0.5：避免低层数时模型小到视觉上“没渲染”；上限 1.6 控制渲染压力
        this.entityData.set(RENDER_SCALE, Mth.clamp(scale, 0.5f, 1.6f));
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
        // 主体检测：裁决斩不得结算到释放者本人（此前仅挡了女仆·宠物，漏了射手自身）
        if (mainTarget == this.getShooter()) {
            return;
        }
        // 女仆/宠物保护：归属发射者的驯服生物（Touhou Little Maid 等）不结算裁决斩伤害
        if (MaidCompat.isOwnedBy(mainTarget, this.getShooter())) {
            return;
        }
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