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
 *   <li>三段式轨迹：<b>炸开</b>（沿原「向外+向上」方向直线 3 tick，向外上方冲出）→
 *       <b>翻越</b>（恒定角速度 28.6°/tick、翻越段限速，约 7 tick 走完 ~200° 的平滑大弧）→
 *       <b>落砸</b>（对准后严格直线 + 加速到 6.5 格/tick，几乎垂直落下）；
 *       对静止目标即“向外炸开 → 飞高 → 平滑翻越 → 竖直砸下”，下落段是一条笔直的线；</li>
 *   <li>命中其它<b>敌对实体</b>时直接穿透并结算伤害，最多穿透 {@value #MAX_PIERCE} 次，第 3 次消失。</li>
 * </ul>
 * 复用 {@code Enchanted_Sword} 的 EntityType 与客户端渲染，无需额外注册；行为差异全部在服务端。
 */
public class EntityZenithSword extends EntityEnchantedSword {
    /** 最多穿透次数：第三次命中即执行消失逻辑。 */
    private static final int MAX_PIERCE = 3;

    // ==============================================
    // 飞行参数（1.4.4）：
    //   旧实现＝「向外+向上的直线 12 tick（飞出 40+ 格）→ 按距离自适应的迟钝转角绕回来」，
    //   结果是单把剑最远离目标 77 格、要 2.95 秒才命中，下落段还是个大弧。
    //   现在保留原「向外炸开」的发射方向，改成「炸开上升 3 tick → 恒定角速度平滑翻越（28.6°/tick）
    //   → 目标正上方竖直直线落砸」：12 把剑先向外上方炸开飞高、再垂直线落下。
    //   仿真（与下面代码 1:1）：顶点约 30 格高、下落角约 90°、末 5 tick 偏离直线 0.000 格、
    //   总命中 59 tick(2.95s) → 26 tick(1.30s)，横向最大离靶 77 格 → 13 格。
    // ==============================================
    /** 炸开段的 tick 数：维持发射方向（向外+向上）先冲出去。 */
    private static final int SPREAD_TICKS = 3;
    /** 炸开段速度（格/tick）：由 combo 传入的 3.0 缓动到该值，决定炸开半径与爬升高度。 */
    private static final double BLAST_SPEED = 3.6;
    /** 翻越时允许的最大角速度（弧度/tick）：0.50 ≈ 28.6°/tick，约 7 tick 完成 ~200° 的平滑大弧。 */
    private static final double TURN_MAX = 0.50;
    /** 翻越阶段限速（格/tick）：转弯半径 r = v/ω，速度压低才能让弧平滑、把下落段拉直。 */
    private static final double TURN_SPEED = 3.2;
    /** 对准目标后的竖直落砸最高速度（格/tick）。 */
    private static final float MAX_SPEED = 6.5F;
    /** 与目标方向夹角小于该值即视为“已对准”，之后纯直线飞行（弧度）。 */
    private static final double ALIGN_EPS = 0.05;
    /** 速度缓动系数：每 tick 朝目标速度收敛的比例，避免加减速出现突跳。 */
    private static final double SPEED_EASE = 0.5;
    /** 对移动目标做提前量预判的时间上限（tick）；静止目标不做预判，保证末段是直线。 */
    private static final double LEAD_MAX = 6.0;

    /** 飞行（已发射）经过的 tick 数，用于区分“炸开段”与“俯冲段”。 */
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
        if (t == null) {
            closeNoClipOnce();
            accelerateStraight();
            return;
        }
        if (flightTicks <= SPREAD_TICKS) {
            // ① 炸开段：维持发射方向（向外+向上），先把「向外炸开、向上升起」的半弧画出来
            blastStraight();
        } else {
            // ② 翻越＋落砸段：恒定角速度平滑翻到目标正上方，对准后竖直直线扎下
            diveAt(t);
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

    /** 炸开段：方向不变，速度从发射速度缓动到 {@link #BLAST_SPEED}，形成向外上方冲出的爆开感。 */
    private void blastStraight() {
        Vec3 vel = getDeltaMovement();
        double len = vel.length();
        if (len < 1.0E-4) return;
        double spd = len + (BLAST_SPEED - len) * SPEED_EASE;
        setDeltaMovement(vel.scale(spd / len));
    }

    /** 无目标时的直线巡航：保持方向，速度缓动到 {@link #MAX_SPEED}。 */
    private void accelerateStraight() {
        Vec3 vel = getDeltaMovement();
        double len = vel.length();
        if (len < 1.0E-4) return;
        double spd = len + (MAX_SPEED - len) * SPEED_EASE;
        setDeltaMovement(vel.scale(spd / len));
    }

    /** 瞄准点：静止目标取身体中心（无预判）；移动目标按飞行时间做短提前量。 */
    private Vec3 aimPoint(Entity target) {
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 move = target.getDeltaMovement();
        if (move.lengthSqr() < 1.0E-6) {
            return center;
        }
        double lead = Math.min(position().distanceTo(center) / MAX_SPEED, LEAD_MAX);
        return center.add(move.scale(lead));
    }

    /**
     * 俯冲段控制：
     * <ol>
     *   <li>方向：以恒定角速度（{@link #TURN_MAX}）朝目标拐，转够本 tick 的角度后继续；
     *       夹角小于 {@link #ALIGN_EPS} 时直接把方向设成「指向目标」，此后的轨迹是严格直线。</li>
     *   <li>速度：转弯时压到 {@link #TURN_SPEED}（小转弯半径），对准后缓动加速到 {@link #MAX_SPEED}，
     *       两头都用 {@link #SPEED_EASE} 做指数过渡，不会有速度突跳。</li>
     * </ol>
     * 两者合起来＝“平滑的一小段弧 + 一条笔直陡峭的下落线”，也就是本技能想要的观感。
     */
    private void diveAt(Entity target) {
        Vec3 pos = position();
        Vec3 aim = aimPoint(target);
        Vec3 toTarget = aim.subtract(pos);
        double dist = toTarget.length();
        if (dist < 1.0E-3) return;
        Vec3 want = toTarget.scale(1.0 / dist);

        Vec3 vel = getDeltaMovement();
        double spd = vel.length();
        Vec3 cur = spd < 1.0E-4 ? want : vel.scale(1.0 / spd);

        double remaining = Math.acos(Mth.clamp(cur.dot(want), -1.0, 1.0));
        boolean aligned = remaining < ALIGN_EPS;

        double desired = aligned ? MAX_SPEED : TURN_SPEED;
        double nextSpeed = spd + (desired - spd) * SPEED_EASE;

        Vec3 newDir;
        if (aligned) {
            newDir = want;                                  // 已对准：严格直线
        } else {
            newDir = rotateTowards(cur, want, Math.min(remaining, TURN_MAX));
        }
        setDeltaMovement(newDir.scale(nextSpeed));

        if (checkDirectHit(target)) return;
        faceDirection(newDir);
    }

    /** 让剑身朝向飞行方向（noClip 下父类会拦截 setYRot/setXRot，这里临时关掉）。 */
    private void faceDirection(Vec3 dir) {
        double hh = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float faceYaw = (float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
        float facePitch = (float) (Mth.atan2(dir.y, hh) * Mth.RAD_TO_DEG);
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
        if (level().isClientSide()) {
            advancePierceForSkippedHit(); // 客户端同样跑父类穿透循环，跳过命中必须推进
            return;
        }
        Entity t = res.getEntity();
        if (t instanceof net.minecraft.world.entity.boss.EnderDragonPart part) {
            t = part.parentMob;
        }
        // 穿过释放者同源对象（本人 / 坐骑载具 / 其所属女仆·宠物）
        if (isOwnerRelated(res.getEntity()) || isOwnerRelated(t)) {
            advancePierceForSkippedHit(); // 跳过命中必须推进父类穿透循环，否则单 tick 死循环
            return;
        }

        // 命中已捕获的追踪目标：结算完整命中效果并消失（追踪到达）
        if (homingTarget != null && t == homingTarget) {
            this.isDiscarded = true; // 标记
            applyHitEffects(t);
            discard();
            return;
        }
        // 命中其它敌对实体：穿透并执行伤害逻辑，最多三次，第三次消失
        if (t instanceof Enemy && !piercedIds.contains(t.getId())) {
            piercedIds.add(t.getId());
            applyPierceDamage(t);
            if (++pierceCount >= MAX_PIERCE) {
                this.isDiscarded = true;
                discard();
            }
            return;
        }
        // 其余情况（非敌对生物、假人、动物、已穿透过的目标……）本剑不作为：仅跳过，
        // 但必须让父类的穿透循环前进，否则它会在同一 tick 内反复命中同一实体导致服务端卡死。
        advancePierceForSkippedHit();
    }

    // ==============================================
    // 方块命中：目标存活（NoClip）时穿过；目标死亡后（NoClip 关闭）撞方块直接消失
    // ==============================================
    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (isNoClip()) {
            return; // 仍在追踪目标：无视方块
        }
        // 撞到实心方块＝这次飞行结束（服务端随后 discard）：先停掉拖尾采样，
        // 让已有尾迹按时间渐隐，而不是被命中点附近的微小位移每 tick 刷新、钉在落点上。
        stopTrailSampling();
        if (level().isClientSide()) return;
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
