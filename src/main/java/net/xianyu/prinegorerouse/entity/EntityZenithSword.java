package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * 穹顶十二式（Zenith12th）专用幻影剑。
 *
 * <p>追踪目标走父类 {@code EntityNRBlisteringSword#resolveHomingTarget()} 的统一捕获链
 * （外部硬锁定 → 玩家重锋锁定 → 近身兜底搜索），<b>捕获后不换手</b>；目标死亡则<b>取消穿墙</b>
 * 并按当时方向直线飞行（此后撞方块直接消失）。与普通追踪剑的差异：</p>
 * <ul>
 *   <li>用“限速弧回”代替瞬转，使“向外炸开→逐渐弧回”的过程可见；转角按距离自适应，
 *       保证转弯半径小于目标距离（否则纯追踪不收敛，剑会绕目标画大圈飘走）；</li>
 *   <li>命中其它<b>敌对实体</b>时直接穿透并结算伤害，最多穿透 {@value #MAX_PIERCE} 次，第 3 次消失。</li>
 * </ul>
 * 复用 {@code Enchanted_Sword} 的 EntityType 与客户端渲染，无需额外注册；行为差异全部在服务端。
 */
public class EntityZenithSword extends EntityEnchantedSword {
    /** 最多穿透次数：第三次命中即执行消失逻辑。 */
    private static final int MAX_PIERCE = 3;

    /** 加速时长：发射后 1 秒（20 tick）内由起始速度线性提升到 {@link #MAX_SPEED}。 */
    private static final double ACCEL_TICKS = 20.0;
    /** 加速上限（格/tick）；起始速度取 combo 传入的 setSpeed（现为 3.0）。 */
    private static final float MAX_SPEED = 5.0F;
    /** 飞行（已发射）经过的 tick 数，用于速度渐变。 */
    private int flightTicks = 0;

    /** 已经穿透结算过的实体 id，防止同一目标被连续多 tick 重复计入穿透次数。 */
    private final Set<Integer> piercedIds = new HashSet<>();
    /** 已穿透次数。 */
    private int pierceCount = 0;
    /** 初始目标是否已丢失（死亡/移除）——用于“只切换一次穿墙状态”。 */
    private boolean targetLost = false;

    public EntityZenithSword(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
    }

    public static EntityZenithSword createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityZenithSword(NrEntitiesRegistry.Enchanted_Sword, worldIn);
    }

    // ==============================================
    // 飞行追踪：复用父类统一捕获链（不换手），目标死亡则取消穿墙并直线飞行
    //   （父类 handleFlyingPhase 为 private，改写入其默认调用的 protected handleSmartTracking）
    // ==============================================
    @Override
    protected void handleSmartTracking() {
        flightTicks++;
        Entity t = resolveHomingTarget();
        if (t != null) {
            // 锁定并追踪已捕获的目标，不更换；采用“限速转向”使剑先向外飞再逐渐弧回目标
            steerArc(t);
        } else {
            // 无目标/目标已丢失：取消穿墙（此后撞方块消失），并保持当前方向按速度曲线逐渐加速
            closeNoClipOnce();
            accelerateStraight();
        }
    }

    @Override
    protected void onHomingTargetLost() {
        closeNoClipOnce();
    }

    /** 只切换一次穿墙状态（沿用旧行为：一旦无目标就不再穿墙）。 */
    private void closeNoClipOnce() {
        if (!targetLost) {
            targetLost = true;
            this.setNoClip(false);
        }
    }

    /** 当前应达到的飞行速度：起始速度 getSpeed()（≈本技能传入的 3.0）随 flightTicks 在 1s 内线性升到 5.0。 */
    private double currentSpeed() {
        double t = Mth.clamp(flightTicks / ACCEL_TICKS, 0.0, 1.0);
        return Mth.lerp(t, (double) getSpeed(), (double) MAX_SPEED);
    }

    /** 直线路径下的加速：保持当前方向，仅把速度模长拉到 {@link #currentSpeed()}。 */
    private void accelerateStraight() {
        Vec3 vel = getDeltaMovement();
        double len = vel.length();
        if (len < 1.0E-4) return;
        setDeltaMovement(vel.scale(currentSpeed() / len));
    }

    /**
     * 限速追踪：把当前速度方向朝目标靠拢，但<b>每 tick 最大转角按距离自适应</b>，
     * 保证转弯半径小于目标距离（父类 {@code maxTurnStepFor}），从而一定能弧回来；
     * 旧实现的固定角速度在 r=v/ω &gt; 距离时只会绕目标画大圈飘走。
     */
    private void steerArc(Entity target) {
        Vec3 pos = position();
        Vec3 tgt = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 vel = getDeltaMovement();
        double spd = vel.length();
        if (spd < 1.0E-4) return;

        Vec3 cur = vel.scale(1.0 / spd);
        Vec3 toTarget = tgt.subtract(pos);
        double dist = toTarget.length();
        if (dist < 1.0E-3) return;
        Vec3 want = toTarget.scale(1.0 / dist);

        double flight = currentSpeed();
        Vec3 newDir = rotateTowards(cur, want, maxTurnStepFor(dist, flight));
        setDeltaMovement(newDir.scale(flight));

        // 抵达/命中判定交给父类：它用「本 tick 位移线段 与 目标碰撞箱」做 clip，
        // 既不会在高速下因点采样跳过目标（穿隧），也不需要一个拍脑袋的抵达半径
        if (checkDirectHit(target)) return;

        // 旋转跟随新速度方向：速度朝向约定 atan2(v.x,v.z)；noClip 下 setYRot/setXRot 被拦截，临时关 noClip 写入
        double hh = Math.sqrt(newDir.x * newDir.x + newDir.z * newDir.z);
        float faceYaw = (float) (Mth.atan2(newDir.x, newDir.z) * Mth.RAD_TO_DEG);
        float facePitch = (float) (Mth.atan2(newDir.y, hh) * Mth.RAD_TO_DEG);
        this.setNoClip(false);
        this.setYRot(faceYaw);
        this.setXRot(facePitch);
        this.setNoClip(true);
    }

    // ==============================================
    // 实体命中：主目标=终结；其它敌对实体=穿透计数
    // ==============================================
    @Override
    protected void onHitEntity(EntityHitResult res) {
        if (level().isClientSide()) return;
        Entity t = res.getEntity();
        if (t instanceof net.minecraft.world.entity.boss.EnderDragonPart part) {
            t = part.parentMob;
        }
        // 穿过释放者同源对象（本人 / 坐骑载具 / 其所属女仆·宠物）
        if (isOwnerRelated(res.getEntity()) || isOwnerRelated(t)) return;

        // 命中已捕获的追踪目标：结算完整命中效果并消失（追踪到达）
        if (homingTarget != null && t == homingTarget) {
            applyHitEffects(t);
            discard();
            return;
        }
        // 命中其它敌对实体：穿透并执行伤害逻辑，最多三次，第三次消失
        if (t instanceof Enemy && !piercedIds.contains(t.getId())) {
            piercedIds.add(t.getId());
            applyPierceDamage(t);
            if (++pierceCount >= MAX_PIERCE) {
                discard();
            }
        }
    }

    // ==============================================
    // 方块命中：目标存活（NoClip）时穿过；目标死亡后（NoClip 关闭）撞方块直接消失
    // ==============================================
    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide()) return;
        if (isNoClip()) {
            return; // 仍在追踪目标：无视方块
        }
        Block block = level().getBlockState(result.getBlockPos()).getBlock();
        if (block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL || block == Blocks.END_GATEWAY) {
            return;
        }
        // 初始目标已死亡，撞到方块直接消失
        discard();
    }

    /** 穿透伤害：对其它敌对实体造成一次本剑伤害（不走父类“命中即插地消失”的逻辑）。 */
    private void applyPierceDamage(Entity e) {
        if (!(e instanceof LivingEntity living)) return;
        DamageSource src = createDamageSource();
        living.hurt(src, (float) getDamage());
        StunManager.setStun(living);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FIREWORK, living.getX(), living.getY(1.0), living.getZ(), 1, 0, 0, 0, 0);
        }
    }

    /** 归因释放者的伤害来源（按 EntityEnchantedSword 同款 holder 约定构造，适配 1.20.1）。 */
    private DamageSource createDamageSource() {
        Entity owner = getOwner();
        Holder<DamageType> holder;
        if (owner instanceof Player) {
            holder = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.PLAYER_ATTACK);
        } else if (owner instanceof LivingEntity) {
            holder = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.MOB_ATTACK);
        } else {
            holder = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.INDIRECT_MAGIC);
        }
        return new DamageSource(holder, this, owner);
    }
}
