package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

/**
 * 负向神力（ReversePower）幻影剑。
 * <p>继承本模组 {@link EntityNRBlisteringSword} 的追踪/命中体系，额外<b>携带一个药水效果</b>：
 * 由 {@code ReversePower} 在玩家攻击时把玩家身上的 debuff 转移而来（时间快照、不减），
 * 剑身颜色取该效果的粒子颜色（{@link MobEffect#getColor()}），命中时把效果以<b>快照时长</b>施加给目标。</p>
 * <p>追踪走父类的统一捕获链（外部硬锁定 → 玩家重锋锁定 → 近身兜底搜索），因此本剑自然优先飞向释放者锁定目标。</p>
 */
public class EntityReverseSword extends EntityNRBlisteringSword {

    /** 携带效果的注册 id；为空表示无效果携带。 */
    private String carried;
    /** 转移时快照的持续时间（tick），命中时按此施加、飞行期间不再衰减。 */
    private int duration;
    /** 转移时快照的等级（amplifier）。 */
    private int amplifier;

    public EntityReverseSword(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
    }

    public static EntityReverseSword createInstance(PlayMessages.SpawnEntity packet, Level world) {
        return new EntityReverseSword(NrEntitiesRegistry.Reverse_Sword, world);
    }

    /**
     * 服务端生成一把携带 {@code effect} 的追踪剑。
     *
     * @param owner     释放者（玩家），锁定目标与伤害属性均取自他
     * @param pos       生成位置
     * @param effect    携带的药水效果
     * @param duration  转移时快照的剩余时长（tick）
     * @param amplifier 转移时快照的等级
     * @param damage    伤害基准（玩家攻击属性）
     */
    public static void spawn(LivingEntity owner, Level world, Vec3 pos, MobEffect effect,
                             int duration, int amplifier, double damage) {
        if (world.isClientSide() || owner == null || !owner.isAlive()) {
            return;
        }
        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (key == null) {
            return;
        }
        // 生成点分布在玩家周围一圈“空心上半球”壳面上：以玩家身体中心为球心，
        // 方位角随机一整圈、仰角限制在上半球（略偏离正上/正下），半径取可见的一段距离；
        // 半径本身就大于玩家碰撞箱，天然避免了在身体内生成导致的首帧碰撞。
        double radius = 2.5 + world.random.nextDouble() * 1.5; // 2.5 ~ 4.0 格
        double azimuth = world.random.nextDouble() * Math.PI * 2.0;                 // [0, 2π)
        double elevation = Math.asin(world.random.nextDouble());                     // [0, π/2) 上半球
        double cosE = Math.cos(elevation);
        Vec3 center = owner.position().add(0.0, owner.getBbHeight() * 0.5, 0.0);
        Vec3 offset = new Vec3(
                cosE * Math.cos(azimuth),
                Math.sin(elevation),
                cosE * Math.sin(azimuth)
        ).scale(radius);
        Vec3 spawnPos = center.add(offset);
        EntityReverseSword sword = new EntityReverseSword(NrEntitiesRegistry.Reverse_Sword, world);
        sword.setOwner(owner);
        sword.setCenterPosition(spawnPos);
        sword.spawnMode = SpawnMode.RANDOM;
        sword.lifeTime = 200;
        sword.setChange(false);
        sword.setDamage(damage);
        sword.setColor(effect.getColor() & 0xFFFFFF); // 剑色 = buff 粒子颜色
        sword.setDelayTicks(0);
        sword.setNoClip(true);
        sword.carried = key.toString();
        // 保留无限时长哨兵 INFINITE_DURATION(-1)；普通时长保证至少 1 tick。
        sword.duration = duration == MobEffectInstance.INFINITE_DURATION
                ? MobEffectInstance.INFINITE_DURATION : Math.max(1, duration);
        sword.amplifier = amplifier;

        sword.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        sword.setYRot(-owner.getYHeadRot());
        sword.setXRot(-owner.getXRot());
        world.addFreshEntity(sword);
    }

    /** 命中目标时把携带的效果按快照时长施加过去；若目标免疫无法施加，则返还给释放者，避免 debuff 静默蒸发。 */
    @Override
    protected void onTargetHit(LivingEntity target) {
        if (level().isClientSide() || carried == null || target == null) {
            return;
        }
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(carried));
        if (effect == null) {
            return;
        }
        // 命中时按快照时长施加：无限时长保持无限，普通时长至少 1 tick。
        int dur = duration == MobEffectInstance.INFINITE_DURATION
                ? MobEffectInstance.INFINITE_DURATION : Math.max(1, duration);
        MobEffectInstance toApply = new MobEffectInstance(effect, dur, amplifier);
        // canBeAffected 为免疫预筛（如末影龙/凋零等），addEffect 返回值为最终权威；两者任一不通过即视为未生效。
        boolean applied = target.canBeAffected(toApply) && target.addEffect(toApply);
        if (!applied) {
            // 目标免疫/无法施加：debuff 不凭空消失，返还给释放者（仅当其存活且为生物时）。
            if (getOwner() instanceof LivingEntity owner && owner.isAlive()) {
                owner.addEffect(new MobEffectInstance(effect, dur, amplifier));
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (carried != null) {
            tag.putString("CarriedEffect", carried);
            tag.putInt("CarriedDuration", duration);
            tag.putInt("CarriedAmplifier", amplifier);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CarriedEffect")) {
            carried = tag.getString("CarriedEffect");
            duration = tag.getInt("CarriedDuration");
            amplifier = tag.getInt("CarriedAmplifier");
        }
    }
}
