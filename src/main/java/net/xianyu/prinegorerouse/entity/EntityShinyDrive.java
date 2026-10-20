package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityShinyDrive extends EntityNRSDrive {

    /**
     * 是否穿透方块（命中方块不消失）。用同步数据存，保证客户端也会跳过移除——
     * 否则客户端的实体副本会在延迟期 checkCollisions() 撞方块时自行 setRemoved，
     * 表现为“实体和碰撞箱消失、服务端粒子却继续飞”。默认关闭，保持黑洞等既有行为不变。
     */
    private static final EntityDataAccessor<Boolean> PIERCE = SynchedEntityData.defineId(
            EntityShinyDrive.class, EntityDataSerializers.BOOLEAN);

    public EntityShinyDrive(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PIERCE, false);
    }

    public void setPierce(boolean pierce) {
        this.entityData.set(PIERCE, pierce);
    }

    public boolean isPierce() {
        return this.entityData.get(PIERCE);
    }

    public static EntityShinyDrive createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityShinyDrive(NrEntitiesRegistry.ShinyDrive, worldIn);
    }

    @Override
    public void tick() {
        // 穿透时同步关闭碰撞（noClip 非同步字段，需在客户端也显式设置）
        if (isPierce() && !isNoClip()) {
            this.setNoClip(true);
        }
        // 命中判定已由基类 EntityNRSDrive 的位移胶囊扫掠（HIT_RADIUS=2.0）统一承担，
        // 每发对同一目标只结算一次，不再依赖碰撞箱尺寸/位置
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(), 3, 0.1, 0.1, 0.1, 0.2);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 去重已由基类 hitIds 统一结算：同一目标只会走到这里一次
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity entity)) return;
        if (entity.isAlive()) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 2));
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // 穿透模式：命中方块不消失，靠存活时间自然销毁
        if (isPierce()) {
            return;
        }
        super.onHitBlock(result);
    }
}
