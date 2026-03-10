package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Level level = entity.level();
        if (!level.isClientSide()) {
            BlockPos blockPos = entity.getOnPos();
            if (this.level().isLoaded(blockPos)) {
                // 1. 仅创建视觉特效雷电（无实际伤害/掉落物影响）
                LightningBolt visualLightning = EntityType.LIGHTNING_BOLT.create(this.level());
                if (visualLightning != null) {
                    visualLightning.setVisualOnly(true); // 纯视觉，不起火/不掉落物
                    visualLightning.setPos(entity.getEyePosition());

                    // 设置雷电视觉特效的触发者（仅显示）
                    if (this.getOwner() instanceof ServerPlayer owner) {
                        visualLightning.setCause(owner);
                    }

                    // 添加视觉雷电到世界
                    this.level().addFreshEntity(visualLightning);
                    this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 5.0F, 1.0F);

                    // 2. 手动处理伤害逻辑（替代原雷电伤害）
                    if (entity instanceof LivingEntity targetEntity) {
                        Entity owner = this.getOwner();
                        // 防误伤：目标不是释放者自身才造成伤害
                        if (targetEntity != owner) {
                            DamageSource damageSource = createAttackerDamageSource(level, owner);
                            // 施加50点伤害（与原逻辑一致）
                            targetEntity.hurt(damageSource, 50.0F);
                            // 保留雷击视觉反馈（无实际伤害）
                            targetEntity.thunderHit((ServerLevel) level, visualLightning);
                        }
                    }
                }
            }
        }
        super.onHitEntity(result);
    }

    /**
     * 适配新版DamageSource构造：基于DamageType创建伤害来源，明确归因释放者
     * @param level 世界对象（用于获取DamageType的Holder）
     * @param attacker 伤害来源实体（技能释放者）
     * @return 归因于attacker的DamageSource
     */
    private DamageSource createAttackerDamageSource(Level level, Entity attacker) {
        // 获取DamageType的注册器（核心：新版必须通过Holder<DamageType>构造）
        Holder<DamageType> damageTypeHolder;
        if (attacker instanceof Player) {
            // 玩家攻击：使用内置的player_attack类型
            damageTypeHolder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.PLAYER_ATTACK);
        } else if (attacker instanceof LivingEntity) {
            // 生物攻击：使用内置的mob_attack类型
            damageTypeHolder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.MOB_ATTACK);
        } else {
            // 兜底：间接魔法伤害（关联技能实体和释放者）
            damageTypeHolder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.INDIRECT_MAGIC);
        }

        // 构造DamageSource：
        // - directEntity：直接造成伤害的实体（本技能的剑实体）
        // - causingEntity：真正的伤害来源（技能释放者）
        return new DamageSource(damageTypeHolder, this, attacker);
    }

    public static void spawnSwords(LivingEntity owner, Level world, Vec3 centerPos, SpawnMode mode, int count,
                                   boolean change , float diyYaw, float diyPitch, float zj , float k,
                                   double damage, int colorCode, boolean clip, int delay) {
        for (int i = 0; i < count; i++) {
            EntityEnchantedSword sword = new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, world);
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