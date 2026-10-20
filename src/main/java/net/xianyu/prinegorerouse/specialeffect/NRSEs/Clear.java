package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.energy.FEBladeStorage;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.entity.*;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class Clear extends NRSpecialEffect {
    public Clear() {
        super(1, false, false);
    }

    static Random random = new Random();

    // 电量恢复配置
    private static final int ENERGY_RESTORE_BASE = 10000;      // 满月时每秒恢复量
    private static final int ENERGY_REQUIRED_PER_HIT = 100; // 每次命中消耗的电量
    private static final float REAL_DAMAGE_PERCENT = 0.03F; // 3%总血量真实伤害
    private static final float MAX_HEALTH_REDUCE_PERCENT = 0.1F; // 降低10%生命上限
    private static final float SA_PROB = 0.1F;             // 10%概率触发SA
    private static final float RANDOM_ENTITY_PROB = 0.5F;   // 50%概率生成随机实体

    /** 锁敌时长（tick）：命中后把目标写入刀的重锋锁定，到期自动解除；无论 SE 是否激活都生效。 */
    private static final int LOCK_TICKS = 60;
    /** 锁敌到期时刻（gameTime）存放在刀根 tag 上的键。 */
    private static final String TAG_CLEAR_LOCK_EXP = "ClearLockExp";

    @SubscribeEvent
    public static void OnSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Clear.getId())) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!event.isSelected())
                return;

            // 锁敌到期解除：3 秒后清空写入的目标（未激活时也会命中上锁，见 OnSlashBladeHit）
            Level lockWorld = player.level();
            if (!lockWorld.isClientSide()) {
                CompoundTag rootTag = event.getBlade().getOrCreateTag();
                if (rootTag.contains(TAG_CLEAR_LOCK_EXP)
                        && lockWorld.getGameTime() >= rootTag.getLong(TAG_CLEAR_LOCK_EXP)) {
                    state.setTargetEntityId((Entity) null);
                    rootTag.put("bladeState", state.serializeNBT()); // 写回刀 tag：持久化并同步客户端
                    rootTag.remove(TAG_CLEAR_LOCK_EXP);
                }
            }

            int level = player.experienceLevel;
            // 原有夜视/抗性效果
            if (!NRSpecialEffect.isAquaActive(event.getBlade(), NrSpecialEffectsRegistry.Clear.getId())) {
                if (player.isUsingItem()) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20, 2));
                }
            } else {
                if (player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                }
            }

            // 新增：夜晚根据月相恢复电量（每1秒一次）
            Level world = player.level();
            if (!world.isClientSide() && world.isNight()) {
                // 每20 tick（1秒）恢复一次
                if (world.getGameTime() % 20 == 0) {
                    // 获取月相因子 (满月=1, 新月=0)
                    int phase = world.getMoonPhase();
                    float moonFactor = 1.0F - (Math.abs(phase - 4) / 4.0F);
                    int energyToRestore = (int) (ENERGY_RESTORE_BASE * moonFactor);
                    if (energyToRestore > 0) {
                        // 获取拔刀剑的FE能力
                        var energyOpt = event.getBlade().getCapability(ForgeCapabilities.ENERGY);
                        if (energyOpt.isPresent()) {
                            FEBladeStorage storage = (FEBladeStorage) energyOpt.orElse(null);
                            if (storage != null && storage.canReceive()) {
                                storage.receiveEnergy(energyToRestore, false);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void OnSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (!state.hasSpecialEffect(NrSpecialEffectsRegistry.Clear.getId()))
            return;

        if (!(event.getUser() instanceof Player))
            return;

        Player player = (Player) event.getUser();
        int level = player.experienceLevel;
        boolean isEffective = NRSpecialEffect.isAquaActive(event.getBlade(), NrSpecialEffectsRegistry.Clear.getId());

        // 锁敌 3 秒：命中即把目标写入刀的重锋锁定（追踪剑/黑洞等按此优先级索敌）；
        // 无论 SE 是否激活都生效（未激活时的保底效果），到期由 OnSlashBladeUpdate 解除
        Entity hitTarget = event.getTarget();
        Level bladeWorld = player.level();
        if (!bladeWorld.isClientSide() && hitTarget != null) {
            state.setTargetEntityId(hitTarget);
            CompoundTag rootTag = event.getBlade().getOrCreateTag();
            rootTag.putLong(TAG_CLEAR_LOCK_EXP, bladeWorld.getGameTime() + LOCK_TICKS);
            rootTag.put("bladeState", state.serializeNBT()); // 写回刀 tag：持久化并同步客户端
        }

        // 获取FE能力
        LazyOptional<FEBladeStorage> energyOpt = event.getBlade().getCapability(ForgeCapabilities.ENERGY)
                .cast();
        boolean hasEnergy = energyOpt.isPresent();
        boolean isPowered = false;
        int energyStored = 0;
        FEBladeStorage storage = null;
        if (hasEnergy) {
            storage = (FEBladeStorage) energyOpt.orElse(null);
            if (storage != null) {
                isPowered = storage.isPowered();
                energyStored = storage.getEnergyStored();
            }
        }

        // 判断是否激活电刀且有足够电量
        boolean electricActive = isPowered && energyStored >= ENERGY_REQUIRED_PER_HIT;
        // 不退：电用完不关闭模式——电量不足时只要时间素足够，就用同额时间素无缝衔接
        boolean aquaPaid = isPowered && !electricActive && storage != null
                && AquaTimeData.get(player) >= ENERGY_REQUIRED_PER_HIT;

        if (isEffective && (electricActive || aquaPaid)) {
            // 消耗电量或时间素
            if (electricActive && storage != null) {
                storage.extractEnergy(ENERGY_REQUIRED_PER_HIT, false);
                // 恰好抽干时 FEBladeStorage 会自动关模式：时间素还够下一刀就保持开启（不退）
                if (!storage.isPowered()
                        && AquaTimeData.get(player) >= ENERGY_REQUIRED_PER_HIT) {
                    storage.setPowered(true);
                }
            } else {
                AquaTimeData.add(player, -ENERGY_REQUIRED_PER_HIT);
            }

            // 1. 5%概率直接进行一次SA
            if (random.nextFloat() < SA_PROB) {
                performSpecialAttack(event, player);
            }

            // 2. 50%概率生成mod自定义实体的随机一种（伤害与刀面板相同）
            if (random.nextFloat() < RANDOM_ENTITY_PROB) {
                spawnRandomCustomEntity(event, player, (float) state.getDamage());
            }

            // 3. 每次攻击附带目标3%总血量的真实伤害（女仆/宠物保护：主人自己的驯服生物免疫）
            Entity target = event.getTarget();
            if (target instanceof LivingEntity livingTarget && !MaidCompat.isOwnedBy(target, player)) {
                float maxHealth = livingTarget.getMaxHealth();
                float realDamage = maxHealth * REAL_DAMAGE_PERCENT;
                DamageSource trueDamageSource = player.damageSources().fellOutOfWorld();
                target.hurt(trueDamageSource, realDamage);
            }

            // 4. 1%概率直接降低目标10%的生命上限（排除玩家、末影龙与主人自己的女仆/宠物）
            if (random.nextFloat() < 0.01f && target instanceof LivingEntity livingTarget
                    && !MaidCompat.isOwnedBy(target, player)) {
                if (!(livingTarget instanceof Player) && !livingTarget.getType().getDescriptionId().contains("dragon")) {
                    AttributeInstance maxHealthAttr = livingTarget.getAttribute(Attributes.MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        double newMax = maxHealthAttr.getBaseValue() * (1 - MAX_HEALTH_REDUCE_PERCENT);
                        newMax = Math.max(1.0, newMax); // 至少保留1点生命上限
                        maxHealthAttr.setBaseValue(newMax);
                        // 如果当前生命值超过新上限，调整当前生命值
                        if (livingTarget.getHealth() > newMax) {
                            livingTarget.setHealth((float) newMax);
                        }
                    }
                }
            }

            // 注意：电刀激活状态下，不执行原有的白天/夜晚逻辑
            return;
        }

        // 原有逻辑（没电或未激活时）
        if (!isEffective) {
            // 未激活效果时，原逻辑只给夜视，无其他
            return;
        }

        Level world = event.getUser().level();
        double decimal = random.nextDouble();
        if (world.isDay()) {
            // 白天：降低耐久 + 概率给经验
            int blade_damage = state.getDamage();
            state.setDamage(blade_damage + 1);
            if (decimal <= 0.2d) {
                player.giveExperiencePoints(200);
            }
        } else {
            // 夜晚：恢复耐久 + 爆炸
            if (decimal <= 0.5d) {
                int blade_damage = state.getDamage();
                state.setDamage(blade_damage - 2);
                Entity target = event.getTarget();

                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                float explosionRadius = 3.0F;
                float fixedDamage = (float) (0.1 * player.getAttributeValue(Attributes.ATTACK_DAMAGE));

                // 仅爆炸特效（粒子+音效），不做真实爆炸，避免把被击生物向上击飞
                if (world instanceof ServerLevel serverLevel) {
                    // EXPLOSION_EMITTER 即大爆炸闪光（1.20.1 无 HUGE_EXPLOSION 命名）
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                            x, y + target.getBbHeight() * 0.5, z, 1, 0.0, 0.0, 0.0, 0.0);
                    serverLevel.playSound(null, x, y, z,
                            SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 0.8F);
                }

                AABB damageRange = new AABB(
                        x - explosionRadius, y - explosionRadius, z - explosionRadius,
                        x + explosionRadius, y + explosionRadius, z + explosionRadius
                );
                List<Entity> entities = world.getEntities(player, damageRange);
                DamageSource damageSource = player.damageSources().playerAttack(player);

                for (Entity entity : entities) {
                    if (entity.isAlive() && !entity.isSpectator()
                            // 女仆/宠物保护：爆炸波及不伤主人自己的驯服生物
                            && !MaidCompat.isOwnedBy(entity, player)) {
                        entity.hurt(damageSource, fixedDamage);
                    }
                }
            }
        }
    }

    private static void performSpecialAttack(SlashBladeEvent.HitEvent event, Player player) {
        Level world = player.level();
        if (!world.isClientSide()) {
            EntityNRJudgementCut sa = new EntityNRJudgementCut(NrEntitiesRegistry.NRJudgementCut, world);
            sa.setShooter(player);
            sa.setPos(event.getTarget().getX(), event.getTarget().getY() + 1, event.getTarget().getZ());
            sa.setDamage(event.getSlashBladeState().getDamage() * 3);
            sa.setRenderScale(0.5f);
            world.addFreshEntity(sa);
        }
    }

    // 随机生成幻影剑（从 BlisteringSword 子类中随机选择，调用各自的 spawnSwords 方法）
    private static void spawnRandomCustomEntity(SlashBladeEvent.HitEvent event, Player player, float damage) {
        Entity target = event.getTarget();
        Level world = target.level();
        if (world.isClientSide()) return;

        // 三种幻影剑类型
        List<Class<?>> swordClasses = Arrays.asList(
                EntityEnchantedSword.class,
                EntityCountableSwords.class,
                EntityStormSword.class
        );
        Class<?> chosenClass = swordClasses.get(random.nextInt(swordClasses.size()));

        Vec3 center = player.getEyePosition();
        // 根据不同类型调用对应的 spawnSwords 静态方法（参数完全一致）
        try {
            if (chosenClass == EntityEnchantedSword.class) {
                EntityEnchantedSword.spawnSwords(player, world, center,
                        EntityNRBlisteringSword.SpawnMode.RANDOM, 1, false, 0, 0, 0, 0,
                        damage, 0xAA00FF, true, 5);
            } else if (chosenClass == EntityCountableSwords.class) {
                EntityCountableSwords.spawnSwords(player, world, center,
                        EntityNRBlisteringSword.SpawnMode.RANDOM, 1, false, 0, 0, 0, 0,
                        damage, 0x33FF33, true, 5);
            }
        } catch (Exception e) {
            // 降级：生成一个普通的 NRBlisteringSword
            EntityNRBlisteringSword.spawnSwords(player, world, center,
                    EntityNRBlisteringSword.SpawnMode.RANDOM, 1, false, 0, 0, 0, 0,
                    damage, 0xFFFFFF, true, 5);
        }
    }
}