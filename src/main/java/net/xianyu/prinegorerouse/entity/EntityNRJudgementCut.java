package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class EntityNRJudgementCut extends EntityJudgementCut {

    // === 新增字段：控制渲染大小 ===
    private static final EntityDataAccessor<Float> RENDER_SCALE =
            SynchedEntityData.defineId(EntityNRJudgementCut.class, EntityDataSerializers.FLOAT);

    public EntityNRJudgementCut(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        // 默认渲染大小设为0.1f（与原值相同）
        this.entityData.set(RENDER_SCALE, 0.1f);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RENDER_SCALE, 0.1f);
    }

    // === 新增方法：设置渲染大小 ===
    public void setRenderScale(float scale) {
        this.entityData.set(RENDER_SCALE, scale);
    }

    // === 新增方法：获取渲染大小 ===
    public float getRenderScale() {
        return this.entityData.get(RENDER_SCALE);
    }

    public static EntityNRJudgementCut createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityNRJudgementCut(NrEntitiesRegistry.NRJudgementCut, worldIn);
    }

    @Override
    public void tick() {
        super.tick();
    }
}
