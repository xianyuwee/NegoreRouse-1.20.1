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
  private static final EntityDataAccessor<Float> RENDER_SCALE = SynchedEntityData.defineId(EntityNRJudgementCut.class, EntityDataSerializers.FLOAT);
  
  public EntityNRJudgementCut(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
    super(entityTypeIn, worldIn);
    this.entityData.set(RENDER_SCALE, Float.valueOf(0.1F));
  }
  
  protected void defineSynchedData() {
    super.defineSynchedData();
    this.entityData.define(RENDER_SCALE, Float.valueOf(0.1F));
  }
  
  public void setRenderScale(float scale) {
    this.entityData.set(RENDER_SCALE, Float.valueOf(scale));
  }
  
  public float getRenderScale() {
    return this.entityData.get(RENDER_SCALE).floatValue();
  }
  
  public static EntityNRJudgementCut createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
    return new EntityNRJudgementCut(NrEntitiesRegistry.NRJudgementCut, worldIn);
  }
  
  public void tick() {
    super.tick();
  }
}