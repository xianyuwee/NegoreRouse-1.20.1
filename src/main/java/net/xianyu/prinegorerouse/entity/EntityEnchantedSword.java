package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrParticlesRegistry;

public class EntityEnchantedSword extends EntityNRBlisteringSword {
    public EntityEnchantedSword(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
    }

    public static EntityEnchantedSword createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, worldIn);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 释放者同源排除（本人 / 坐骑载具 / 其所属女仆·宠物）：雷光表现与伤害一律不生效
        if (isOwnerRelated(result.getEntity())) return;
        Entity entity = result.getEntity();
        Level level = entity.level();
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // 纯粒子闪电表现：不再生成 LightningBolt 实体（去掉实体闪电的渲染与全屏亮白，避免掉帧）
            double cx = entity.getX();
            double cy = entity.getY() + entity.getBbHeight() * 0.5;
            double cz = entity.getZ();
            // 一道竖直锯齿雷光劈在目标身体中心
            serverLevel.sendParticles(NrParticlesRegistry.LIGHTNING_BOLT.get(), cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
            // 周围撒 vanilla 电火花，增加“兹啪”电弧感
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 10, 0.35, 0.45, 0.35, 0.0);
            // 安静短促的“滋”：低音量+高音调把雷鸣变成清脆放电声（原 5.0 音量雷声已移除）
            this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.25F, 1.7F + this.random.nextFloat() * 0.3F);

            // 闪电伤害保留：1 倍拥有者面板攻击伤害，归因释放者
            if (entity instanceof LivingEntity targetEntity) {
                Entity owner = this.getOwner();
                // 防误伤：拥有者存在才造成伤害（释放者同源已由方法入口统一排除）
                if (owner instanceof LivingEntity livingOwner) {
                    DamageSource damageSource = createAttackerDamageSource(level, owner);
                    // 获取拥有者的基础攻击伤害（面板值），造成 1 倍面板伤害
                    double baseAttackDamage = livingOwner.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    targetEntity.hurt(damageSource, (float) baseAttackDamage);
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