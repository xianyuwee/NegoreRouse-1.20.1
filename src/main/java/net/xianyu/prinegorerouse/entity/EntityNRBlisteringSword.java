package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.BlackHoleUtil;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class EntityNRBlisteringSword extends EntityAbstractSummonedSword {
    public static final EntityDataAccessor<Boolean> IT_FIRED = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> TARGET = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Vector3f> CENTER_POS = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Float> DIRECTION_YAW = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DIRECTION_PITCH = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DIY_YAW = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DIY_PITCH = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> LOCK_TARGET = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> LOCK_TARGET_ID = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DELAY_TICK = SynchedEntityData.defineId(EntityNRBlisteringSword.class,EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IT_CHANGED = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> USE_CUSTOM_DIRECTION = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AUTO_TARGETING = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);
    /** 「磁暴」样式（MagneticStormSword 生成的幻影剑）：加速冲刺 + 平滑追踪 + 客户端拖尾。默认 false。 */
    private static final EntityDataAccessor<Boolean> MAGNETIC_STORM = SynchedEntityData.defineId(EntityNRBlisteringSword.class, EntityDataSerializers.BOOLEAN);

    private final float rotationAngle = 0.0f;
    private int delayTicks = 20;
    public SpawnMode spawnMode = SpawnMode.CIRCLE;
    private Vec3 prevPos = Vec3.ZERO;
    private float prevRotationAngle = 0.0f;

    public int lifeTime = 200;
    private boolean hasInitialized = false;

    private float initialYaw;
    private float initialPitch;

    private float diyYaw;
    private float diyPitch;
    private Entity lockTarget;

    protected float trackingRange = 8.0f;
    protected Entity homingTarget;

    // ==============================================
    // 统一追踪体系（本 mod 全部幻影剑共用）
    // ==============================================
    /** 捕获过的目标是否已死亡/被移除：为 true 后不再捕获新目标，按当时方向直线飞行到自然消亡或命中。 */
    protected boolean homingLost;

    /**
     * 期望转弯半径与「到目标距离」的比例上限。纯追踪的收敛条件是转弯半径 {@code r=v/ω} 小于目标距离；
     * 固定角速度会让 r 与速度成正比、与距离无关，r 一旦大于距离，剑就绕着目标画大圈越飘越远。
     */
    protected static final double TURN_RADIUS_RATIO = 0.45;
    /**
     * 每 tick 最大转角下限（弧度）：决定“背离目标时掉头的最快时间” = π/本值。
     * 太小会让远距锁定时先朝外飞出上百格才回头的弧形浪费寿命与客户端同步范围，
     * 故取 0.25（约 14°/tick，掉头约 13 tick），近距时仍由上限 1.2 主导手感。
     */
    protected static final double MIN_TURN_STEP = 0.25;
    /** 每 tick 最大转角上限（弧度）：贴脸时避免抖动式急转。 */
    protected static final double MAX_TURN_STEP = 1.2;

    private int smiteLevel;
    private int sharpnessLevel;
    private int baneLevel;

    private Entity currentMainTarget = null;

    private static final int SEARCH_COOLDOWN = 5;
    private int searchCooldown = 0;
    boolean isDiscarded = false;

    // ==============================================
    // 「磁暴」样式参数（MagneticStormSword 专用）
    //   与十二式同款三段式：冲刺 → 恒定角速度平滑转向（限速）→ 对准后直线加速冲刺。
    //   只由 setMagneticStorm(true) 开启，其它剑（Clear / Fate / 十二式…）行为不变。
    // ==============================================
    /** 冲刺段 tick 数：先沿发射方向直冲，再开始平滑转向。 */
    private static final int MAG_BLAST_TICKS = 1;
    /** 平滑转向的最大角速度（弧度/tick）：0.55 ≈ 31.5°/tick，恒定角速度画出来是圆弧，不会抖头。 */
    private static final double MAG_TURN_MAX = 0.55;
    /** 转向阶段限速（格/tick）：转弯半径 r = v/ω，压低速度才能让弧平滑、并保证一定收敛。 */
    private static final double MAG_TURN_SPEED = 3.6;
    /** 对准目标后的直线冲刺速度（格/tick）。 */
    private static final double MAG_MAX_SPEED = 7.0;
    /** 与目标方向夹角小于该值即视为“已对准”，之后纯直线（弧度）。 */
    private static final double MAG_ALIGN_EPS = 0.06;
    /** 速度缓动系数：每 tick 朝目标速度收敛的比例，避免加减速突跳。 */
    private static final double MAG_SPEED_EASE = 0.5;
    /**
     * 拖尾寿命（毫秒）：<b>用真实时间而不是 tick 数</b>来老化。
     *
     * <p>关键教训：原来用「每个 tick 把年龄 +1」的老化方式，一旦这把剑在某一帧之后不再执行
     * {@code tick()}（命中地面/方块后被父类冻结、被插附到实体上跟随、客户端实体被同步残留等），
     * 年龄就永远停在原地 —— 于是那截拖尾以全亮度定格在命中点，直到整把剑消失为止，也就是
     * 「拖尾不淡化、不消失」的现象。改成挂钟时间后，渲染端自己就能把过期顶点丢掉，
     * 与实体是否还在 tick 完全解耦。</p>
     */
    public static final long TRAIL_LIFE_MILLIS = 1000L;
    /** 拖尾寿命的 tick 等价量（20 tick ≈ 1 秒，仅用于日志/注释与旧代码兼容）。 */
    public static final int TRAIL_LIFE_TICKS = 20;
    /** 拖尾采样点数量上限：防止实体长时间卡住时列表无限增长。 */
    private static final int TRAIL_MAX_SAMPLES = 96;
    /** 拖尾最小采样间隔（平方距离）：剑没真的移动就不追加新点，避免停住的剑把拖尾「钉」在原地。 */
    private static final double TRAIL_MIN_STEP_SQR = 1.0E-4;
    /** 「剑已经停下」的判定半径平方（0.5 格）：围绕锚点来回抖动不算移动。 */
    private static final double TRAIL_STUCK_RADIUS_SQR = 0.25;
    /** 在锚点附近停留超过该毫秒数即视为停下：此后不再追加采样点，让已有拖尾自然渐隐。 */
    private static final long TRAIL_STUCK_MILLIS = 400L;

    /** 磁暴样式已经飞行的 tick 数（用于冲刺段与转向段切分）。 */
    private int magneticFlightTicks = 0;
    /** 客户端拖尾采样点（index 0 = 最新）；仅客户端写入，渲染线程只读。 */
    private final List<Vec3> trailPositions = new ArrayList<>(TRAIL_MAX_SAMPLES);
    /** 与 {@link #trailPositions} 一一对应的采样时刻（{@link Util#getMillis()}，毫秒）。 */
    private final List<Long> trailTimes = new ArrayList<>(TRAIL_MAX_SAMPLES);
    /** 停下判定的位置锚点与时刻：剑在锚点附近待够了时间就不再刷新拖尾。 */
    private Vec3 trailAnchor = null;
    private long trailAnchorMillis = 0L;

    /** 是否为「磁暴」样式。 */
    public boolean isMagneticStorm() {
        return this.entityData.get(MAGNETIC_STORM);
    }

    /** 设置「磁暴」样式（加速冲刺 + 平滑追踪 + 拖尾）。只影响飞行/渲染，不影响生成方式。 */
    public void setMagneticStorm(boolean magneticStorm) {
        this.entityData.set(MAGNETIC_STORM, magneticStorm);
    }

    /** 是否记录拖尾：磁暴剑默认开；十二式由 {@code EntityEnchantedSword} 扩展（isZenith）。 */
    protected boolean shouldRecordTrail() {
        return isMagneticStorm();
    }

    /** 供渲染端判断这把剑是否要画拖尾。 */
    public boolean hasTrail() {
        return shouldRecordTrail();
    }

    /** 客户端拖尾采样点（index 0 = 最新）。 */
    public List<Vec3> getTrailPositions() {
        return trailPositions;
    }

    /** 第 index 个拖尾采样点的采样时刻（{@link Util#getMillis()} 毫秒），渲染端按挂钟时间算透明度。 */
    public long getTrailTime(int index) {
        return trailTimes.get(index);
    }

    /** 清空拖尾采样（样式关闭 / 实体已移除时）。 */
    private void clearTrail() {
        trailPositions.clear();
        trailTimes.clear();
        trailAnchor = null;
        trailAnchorMillis = 0L;
    }

    /**
     * 立即让"剑已停下"成立：命中方块（{@link #onHitBlock}）等明确结束飞行的时刻调用。
     *
     * <p>做法是把停下判定的锚点钉在当前坐标，并把锚点时刻推到
     * {@link #TRAIL_STUCK_MILLIS} 之前，于是这一 tick 起就不再追加采样点，
     * 已有采样点仍会按 {@link #TRAIL_LIFE_MILLIS} 自然渐隐。</p>
     *
     * <p>这里刻意<b>不</b>用一个永久的"禁止采样"开关：客户端 {@code isNoClip()} 读的是同步标志，
     * 生成后头一两个 tick 可能还没同步到（此时剑若正好卡在方块里会误触发本方法）。
     * 用锚点方案时，剑只要离开锚点 0.5 格就会自动恢复采样，误判不会导致整把剑没有拖尾。</p>
     */
    protected void stopTrailSampling() {
        this.trailAnchor = position();
        this.trailAnchorMillis = Util.getMillis() - TRAIL_STUCK_MILLIS - 1L;
    }

    // ==============================================
    // 新增：delay阶段固定位置缓存
    // ==============================================
    private Vec3 fixedDelayPos = Vec3.ZERO;

    // ==============================================
    // 反射强制修改父类 private inGround 字段
    // ==============================================
    private static final Field IN_GROUND_FIELD;
    static {
        Field tmp = null;
        try {
            tmp = EntityAbstractSummonedSword.class.getDeclaredField("inGround");
            tmp.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        IN_GROUND_FIELD = tmp;
    }

    // ==============================================
    // 核心修复1：旋转锁，拦截父类的强制旋转/反向
    // ==============================================
    private boolean allowRotationUpdate = false;

    @Override
    public void setYRot(float yaw) {
        // 只有我们自己允许修改时，才更新旋转，彻底拦截父类的强制反向
        if (allowRotationUpdate || !isNoClip()) {
            super.setYRot(yaw);
            this.yRotO = yaw; // 同步旧值，防止插值混乱
        }
    }

    @Override
    public void setXRot(float pitch) {
        // 只有我们自己允许修改时，才更新旋转
        if (allowRotationUpdate || !isNoClip()) {
            super.setXRot(pitch);
            this.xRotO = pitch; // 同步旧值，防止插值混乱
        }
    }

    // 我们自己的旋转设置专用方法，解锁旋转锁
    private void setRotationSafe(float yaw, float pitch) {
        allowRotationUpdate = true;
        setYRot(yaw);
        setXRot(pitch);
        allowRotationUpdate = false;
    }

    public EntityNRBlisteringSword(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
        // ⚠️ 穿透次数必须保持 0！父类 EntityAbstractSummonedSword.tick() 里有这样一段“穿透循环”：
        //     while (isAlive()) { hit = getRayTrace(from, to); ...; onHit(hit);
        //                         if (getPierce() <= 0) break; /* 否则用同一个 from/to 无限重试 */ }
        // 它唯一的推进手段是 super.onHitEntity() 内部的 alreadyHits 记录 + setPierce(pierce-1)/burst()。
        // 而本类与各子类为了“剑穿过同源目标/非敌对实体”有大量直接 return 的早退分支，
        // 一旦早退没有消耗穿透次数，父类就会在同一个 server tick 里用同样的射线结果原地死循环
        // → 服务端主线程永久卡死（表现为游戏未响应，日志里是 ModernFix watchdog
        //   “A single server tick has taken 40001ms” + 线程栈停在幻影剑的 tick/onHit/getRayTrace）。
        // 多段穿透已经由本类的 currentMainTarget、EntityZenithSword 的 piercedIds/pierceCount 自行实现，
        // 不需要也不应该再依赖父类的 pierce 机制。
        this.setPierce((byte) 0);
        this.setNoGravity(true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // 1.4.3 及更早版本的实体把 pierce=5 一起存进了 NBT，读档会盖掉构造器里的归零，
        // 老存档里的幻影剑因此仍可能触发父类穿透死循环。这里统一强制归零（单机/服务端均为权威值）。
        this.setPierce((byte) 0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IT_FIRED, false);
        this.entityData.define(TARGET, Optional.empty());
        this.entityData.define(SPEED, 2.0F);
        this.entityData.define(CENTER_POS, Vec3.ZERO.toVector3f());
        this.entityData.define(DIRECTION_YAW, 0.0f);
        this.entityData.define(DIRECTION_PITCH, 0.0f);
        this.entityData.define(IT_CHANGED, true);
        this.entityData.define(DIY_YAW, 0F);
        this.entityData.define(DIY_PITCH, 0F);
        this.entityData.define(LOCK_TARGET, Optional.empty());
        this.entityData.define(LOCK_TARGET_ID, -1);
        this.entityData.define(DELAY_TICK, delayTicks);
        this.entityData.define(USE_CUSTOM_DIRECTION, false);
        this.entityData.define(AUTO_TARGETING, false);
        this.entityData.define(MAGNETIC_STORM, false);
    }

    // ==============================================
    // 核心修复2：完全重写tick逻辑，分离delay和flying阶段
    // ==============================================
    @Override
    public void tick() {
        // 1. 先处理NoClip的inGround强制清除
        if (isNoClip() && IN_GROUND_FIELD != null) {
            try {
                IN_GROUND_FIELD.setBoolean(this, false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (isDiscarded) return;
        currentMainTarget = null;

        // 2. 初始化逻辑（只执行一次）
        if (!hasInitialized) {
            initializeEntity();
            // 生成阶段源头排除：把与主人重叠的出生点推出主人碰撞箱，再缓存延迟固定位
            nudgeOutOfOwner();
            prevPos = position();
            // 初始化时记录固定位置
            fixedDelayPos = position();
        }

        // 3. 生命周期控制
        if (!level().isClientSide && --lifeTime <= 0) {
            discard();
            return;
        }

        // 4. 分离处理：delay阶段和flying阶段完全分开
        if (!itFired()) {
            if (getOwner() == null || !getOwner().isAlive()) {
                discard();
                return;
            }
            handleDelayPhase();
        } else {
            Vec3 vel = getDeltaMovement();
            if (!Double.isFinite(vel.x) || !Double.isFinite(vel.y) || !Double.isFinite(vel.z)) {
                // 意外产生了 NaN/无穷大，直接移除实体，防止崩溃
                discard();
                return;
            }
            super.tick();
            handleFlyingPhase();
            updateTrail();
        }
    }

    /**
     * 客户端拖尾维护：按<b>挂钟时间</b>淘汰过期点 → 只有真的在飞才追加新点。
     *
     * <p><b>为什么用毫秒而不是 tick 计数</b>：剑一旦命中地面/方块，父类
     * {@code EntityAbstractSummonedSword#tick} 会把它判定为 inGround 并直接 return（或插附到实体上跟随），
     * 之后再也不会移动；更极端的情况是 tick 本身被冻结。只要老化依赖 tick，那截拖尾就会以全亮度
     * 定格在原地直到整把剑被移除（"不淡化、不消失"）。用挂钟时间后，渲染端只画
     * {@link #TRAIL_LIFE_MILLIS} 内的采样点，所以无论剑是否还在 tick，旧拖尾都会在 1 秒内渐隐消失。</p>
     *
     * <p><b>防止抖动续命</b>：命中点附近剑可能会被碰撞/插附逻辑每帧推动零点几格来回抖，
     * 若只按"距离上一个点 > 0.01"判断就会一直追加新点、拖尾永远不淡。因此额外用
     * {@link #trailAnchor} 做"停下判定"：0.5 格范围内待满 {@link #TRAIL_STUCK_MILLIS} 毫秒就停止采样。</p>
     */
    private void updateTrail() {
        if (!level().isClientSide()) {
            return;
        }
        long now = Util.getMillis();
        // 1) 淘汰过期采样点（列表按时间从新到旧排列）
        while (!trailTimes.isEmpty() && now - trailTimes.get(trailTimes.size() - 1) >= TRAIL_LIFE_MILLIS) {
            trailTimes.remove(trailTimes.size() - 1);
            trailPositions.remove(trailPositions.size() - 1);
        }
        if (!itFired() || isRemoved() || !shouldRecordTrail()) {
            clearTrail();
            return;
        }

        Vec3 pos = position();
        // 2) 停下判定：锚点外 > 0.5 格才算真的在移动
        if (trailAnchor == null || pos.distanceToSqr(trailAnchor) > TRAIL_STUCK_RADIUS_SQR) {
            trailAnchor = pos;
            trailAnchorMillis = now;
        }
        // 已插附到实体 / 速度归零 / 在锚点附近磨了很久（含命中方块后 stopTrailSampling 的立刻停止）
        // → 判定停下，不再追加采样点
        boolean noLongerFlying = now - trailAnchorMillis > TRAIL_STUCK_MILLIS
                || getHitEntity() != null
                || getDeltaMovement().lengthSqr() < 1.0E-6;
        if (noLongerFlying) {
            return;
        }

        // 3) 只有真的移动了才追加新点
        if (trailPositions.isEmpty() || trailPositions.get(0).distanceToSqr(pos) > TRAIL_MIN_STEP_SQR) {
            trailPositions.add(0, pos);
            trailTimes.add(0, now);
            while (trailPositions.size() > TRAIL_MAX_SAMPLES) {
                trailPositions.remove(trailPositions.size() - 1);
                trailTimes.remove(trailTimes.size() - 1);
            }
        }
    }

    // ==============================================
    // 核心修复4：NoClip时方块命中完全失效
    // ==============================================
    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (isNoClip()) {
            return;
        }
        // 传送门类方块：直接穿过，不交互不消失
        Block block = level().getBlockState(result.getBlockPos()).getBlock();
        if (block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL || block == Blocks.END_GATEWAY) {
            return;
        }
        // 命中方块＝这把剑的旅程到此为止（父类 EntityAbstractSummonedSword#onHitBlock 会把速度改成
        // "指向命中面的短向量"、把剑回退 0.05 格并置 inGround，随后 tick 里 inGround 分支直接 return，
        // 剑就在命中点附近停住并最终 burst()）。若继续按位置采样，剑在命中面附近的微小位移/抖动会
        // 每 tick 刷新一次拖尾采样，整条尾迹就永远以全亮度钉在落点上、直到剑消失 —— 就是
        // 「命中地面后拖尾不淡化不消失」。这里直接停止采样，已有采样点仍会按时间自然渐隐。
        stopTrailSampling();
        super.onHitBlock(result);
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

    // ==============================================
    // 注册修复：客户端工厂方法
    // ==============================================
    public static EntityNRBlisteringSword createInstance(PlayMessages.SpawnEntity packet, Level world) {
        return new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, world);
    }

    public void doFire() {
        this.getEntityData().set(IT_FIRED, true);
    }

    public boolean itFired() {
        return this.entityData.get(IT_FIRED);
    }

    public void setChange(boolean change) {
        this.getEntityData().set(IT_CHANGED, change);
    }

    public void setUseCustomDirection(boolean use) {
        this.entityData.set(USE_CUSTOM_DIRECTION, use);
    }

    public boolean useCustomDirection() {
        return this.entityData.get(USE_CUSTOM_DIRECTION);
    }

    public void setAutoTargeting(boolean auto) {
        this.entityData.set(AUTO_TARGETING, auto);
    }

    public boolean autoTargetingEnabled() {
        return this.entityData.get(AUTO_TARGETING);
    }

    public void doChange() {
        this.getEntityData().set(IT_CHANGED, true);
    }

    public boolean itChanged() {
        return this.entityData.get(IT_CHANGED);
    }

    public void setSpeed(float speed) {
        this.getEntityData().set(SPEED, speed);
    }

    public float getSpeed() {
        return this.entityData.get(SPEED);
    }

    public int getDelayTicks() {
        return this.entityData.get(DELAY_TICK);
    }

    public void setDelayTicks(int delayTick) {
        this.entityData.set(DELAY_TICK, delayTick);
    }

    public void setCenterPosition(Vec3 pos) {
        this.entityData.set(CENTER_POS, pos.toVector3f());
    }

    public float getDiyYaw() {
        return this.entityData.get(DIY_YAW);
    }

    public void setYAW(Float diyYaw) {
        this.entityData.set(DIY_YAW, diyYaw);
    }

    public float getDiyPitch() {
        return this.entityData.get(DIY_PITCH);
    }

    public void setPITCH(Float diyPitch) {
        this.entityData.set(DIY_PITCH, diyPitch);
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    private Entity getMainEntity(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof EnderDragonPart part) {
            return part.getParent();
        }

        // 1) parent 字段
        Field f = PARENT_FIELD_CACHE.get(entity.getClass());
        if (f != null) {
            try {
                Object p = f.get(entity);
                if (p instanceof Entity pe) return pe;
            } catch (Throwable ignored) {}
        }

        // 2) getParent() 方法
        Method m = GET_PARENT_CACHE.get(entity.getClass());
        if (m != null) {
            try {
                Object p = m.invoke(entity);
                if (p instanceof Entity pe) return pe;
            } catch (Throwable ignored) {}
        }

        return entity;
    }

    public Optional<Entity> getLockTarget() {
        int targetId = this.entityData.get(LOCK_TARGET_ID);
        if (targetId == -1) return Optional.empty();
        Entity target = this.level().getEntity(targetId);
        if (target != null) {
            Entity main = getMainEntity(target);
            // 读取端同样排除：历史误锁（含老存档）到释放者一侧时视为无锁定
            if (isOwnerRelated(main)) return Optional.empty();
            return Optional.ofNullable(main);
        }
        return Optional.empty();
    }

    /** 缓存每种实体类型的 parent 字段（无则 null），避免热路径反复反射 + 抛异常。 */
    private static final ClassValue<Field> PARENT_FIELD_CACHE = new ClassValue<>() {
        @Override
        protected Field computeValue(Class<?> type) {
            try {
                Field f = type.getDeclaredField("parent");
                f.setAccessible(true);
                return f;
            } catch (Throwable t) {
                return null; // 缓存 null，以后不再尝试
            }
        }
    };

    /** 缓存每种实体类型的 getParent() 方法（无则 null），避免热路径反复反射 + 抛异常。 */
    private static final ClassValue<Method> GET_PARENT_CACHE = new ClassValue<>() {
        @Override
        protected Method computeValue(Class<?> type) {
            try {
                Method m = type.getMethod("getParent");
                m.setAccessible(true);
                return m;
            } catch (Throwable t) {
                return null; // 缓存 null，以后不再尝试
            }
        }
    };

    /**
     * 统一判定：某实体是否与本剑的<b>释放者同源</b>——本人、其坐骑/载具、或其所属驯服生物
     * （Touhou Little Maid 女仆、原版狼/猫等宠物）。幻影剑的任何阶段（生成、锁定、追踪、
     * 射线、命中、插附跟随）都必须排除这些对象，否则会出现“剑插在发射者身上”。
     * <p>注意：骑乘中命中坐骑等同于命中本人——父类会把剑插附到被命中实体上，
     * 且 {@link #getMainEntity} 会把骑乘玩家归并到载具，单靠 {@code e == owner} 判不出来。</p>
     */
    public boolean isOwnerRelated(Entity e) {
        if (e == null) {
            return false;
        }
        Entity owner = getOwner();
        if (owner == null) {
            return false;
        }
        if (e == owner || e.getUUID().equals(owner.getUUID())) {
            return true;
        }
        Entity main = getMainEntity(e);
        if (main == owner || (main != null && main.getUUID().equals(owner.getUUID()))) {
            return true;
        }
        Entity vehicle = owner.getVehicle();
        if (vehicle != null && (e == vehicle || main == vehicle)) {
            return true;
        }
        return MaidCompat.isOwnedBy(e, owner) || MaidCompat.isOwnedBy(main, owner);
    }

    /**
     * 生成阶段源头排除：若出生点落在释放者碰撞箱内（撒在玩家周围的圆阵/随机球壳均可能），
     * 沿“主人→剑”的水平方向把剑推出箱外；正好在身体轴线上时向上顶出。
     * 只在首个 tick 初始化时调用一次，之后 {@code fixedDelayPos} 取的就是推出后的位置。
     */
    private void nudgeOutOfOwner() {
        Entity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        if (!owner.getBoundingBox().inflate(0.1).intersects(getBoundingBox())) {
            return;
        }
        Vec3 center = owner.position().add(0, owner.getBbHeight() * 0.5, 0);
        Vec3 dir = position().subtract(center);
        double hLen = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        double radius = Math.max(owner.getBbWidth() * 0.5, 0.35) + 0.35;
        if (hLen < 1.0E-3) {
            // 与主人同一竖直轴线：向上顶出到主人头顶略上
            setPos(getX(), center.y + owner.getBbHeight() * 0.5 + radius, getZ());
        } else {
            double y = Mth.clamp(dir.y, -owner.getBbHeight() * 0.5, owner.getBbHeight() * 0.5);
            setPos(center.x + dir.x / hLen * radius, center.y + y, center.z + dir.z / hLen * radius);
        }
        setDeltaMovement(Vec3.ZERO);
    }

    /**
     * 直接指定本剑的<b>硬锁定目标</b>（写入同步的 {@code LOCK_TARGET_ID}）。
     * 供炯恻之眼等外部逻辑复用：让追踪剑锁定调用方选定的生物，而非自行搜索/跟随释放者锁定。
     * {@code target==null} 表示清除锁定。
     */
    public void setLockTarget(Entity target) {
        // 写入端拒绝：外部逻辑（炯恻之眼、苍穹十二连等）误传释放者一侧的对象时直接清空锁定
        if (target != null && isOwnerRelated(target)) {
            this.entityData.set(LOCK_TARGET_ID, -1);
            return;
        }
        this.entityData.set(LOCK_TARGET_ID, target == null ? -1 : target.getId());
    }

    /**
     * 读取释放者（玩家）当前的<b>重锋锁定单位</b>：真正的锁定目标写在刀状态
     * {@link mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState#getTargetEntity(Level)}
     * 里（由 LockOnManager 写入）；<b>不能</b>拿
     * {@code TargetSelector.getTargettableEntitiesWithinAABB} 当锁定用——它只枚举近战射程内的候选。
     * 主手优先、其次副手；多段生物归并到本体；无锁定/失效/释放者同源均返回 {@code null}。
     */
    protected Entity resolveOwnerBladeLock() {
        if (!(getOwner() instanceof LivingEntity owner)) {
            return null;
        }
        Entity locked = bladeLockOf(owner.getMainHandItem());
        if (locked == null) {
            locked = bladeLockOf(owner.getOffhandItem());
        }
        if (locked == null || !locked.isAlive() || locked.isSpectator() || !(locked instanceof LivingEntity)) {
            return null;
        }
        Entity main = getMainEntity(locked);
        return isOwnerRelated(main) ? null : main;
    }

    /** 从单把刀读重锋锁定单位。非拔刀剑无此能力；<b>注意</b> LazyOptional.map 禁止返回 null（会 NPE），故先 resolve() 取真 Optional。 */
    private Entity bladeLockOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .resolve()
                .map(st -> st.getTargetEntity(level()))
                .orElse(null);
    }

    private void initializeEntity() {
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.delayTicks = this.getDelayTicks();
        if (!this.level().isClientSide()) extractEnchantments();

        if (this.getOwner() != null) {
            LivingEntity owner = (LivingEntity) this.getOwner();
            if (getDiyYaw() == 0 && getDiyPitch() == 0) {
                if (spawnMode == SpawnMode.RANDOM) {
                    this.initialYaw = owner.getYHeadRot();
                    this.initialPitch = owner.getXRot();
                } else {
                    this.initialYaw = -owner.getYHeadRot();
                    this.initialPitch = -owner.getXRot();
                }
                this.setDeltaMovement(this.getLookAngle());
                if (!itChanged()) {
                    // 初始化用安全旋转方法
                    setRotationSafe(initialYaw, initialPitch);
                }
                this.entityData.set(DIRECTION_YAW, initialYaw);
                this.entityData.set(DIRECTION_PITCH, initialPitch);
            } else {
                this.initialYaw = getDiyYaw();
                this.initialPitch = getDiyPitch();
                double yawRad = initialYaw * Mth.DEG_TO_RAD;
                double pitchRad = initialPitch * Mth.DEG_TO_RAD;
                double xzLen = Math.cos(pitchRad);
                double x = -Math.sin(yawRad) * xzLen;
                double y = -Math.sin(pitchRad);
                double z = Math.cos(yawRad) * xzLen;
                Vec3 direction = new Vec3(x, y, z).normalize();
                this.setDeltaMovement(direction);
                if (!itChanged()) {
                    // 初始化用安全旋转方法
                    setRotationSafe(-initialYaw, -initialPitch);
                }
                this.entityData.set(DIRECTION_YAW, initialYaw);
                this.entityData.set(DIRECTION_PITCH, initialPitch);
            }
        }
        this.hasInitialized = true;
    }

    // ==============================================
    // 核心修复5：完全重写handleDelayPhase，强制固定位置，只更新旋转
    // ==============================================
    private void handleDelayPhase() {
        // 强制固定在初始位置，绝对不让它动
        this.setPos(fixedDelayPos.x, fixedDelayPos.y, fixedDelayPos.z);
        this.setDeltaMovement(Vec3.ZERO);

        if (this.level().isClientSide()) {
            this.prevPos = this.position();
            this.prevRotationAngle = this.rotationAngle;
        }

        if (!this.level().isClientSide()) {
            if (this.delayTicks > 0) {
                this.delayTicks--;
                if (spawnMode == SpawnMode.RANDOM) {
                    if (!itChanged()) {
                        // change=false：保持初始方向，不动
                        setRotationSafe(-initialYaw, -initialPitch);
                    } else {
                        // change=true：跟随玩家视角
                        adjustInitialDirection();
                    }
                }
            } else {
                // delay结束，发射
                launchProjectile();
                this.entityData.set(IT_FIRED, true);
            }
        }

        if (this.level().isClientSide()) {
            if (itChanged()) {
                adjustInitialDirection();
            }
        }
    }

    private void maintainCustomDirection() {
        // 自定义方向用安全旋转方法
        setRotationSafe(initialYaw, initialPitch);
    }

    private void adjustInitialDirection() {
        if (itChanged()) {
            LivingEntity owner = (LivingEntity) this.getOwner();
            if (owner == null) return;
            this.initialYaw = -owner.getYHeadRot();
            this.initialPitch = -owner.getXRot();
            // 跟随视角用安全旋转方法
            setRotationSafe(initialYaw, initialPitch);
        }
    }

// ==============================================
// 核心修复：完全重写 launchProjectile，确保 change=false 时方向正确
// ==============================================
    private void launchProjectile() {
        if (this.getOwner() == null) {
            this.discard();
            return;
        }

        Vec3 shootDir;
        float finalYaw;
        float finalPitch;

        if (itChanged()) {
            // change=true：使用当前玩家视角（保持之前的正确逻辑）
            LivingEntity owner = (LivingEntity) this.getOwner();
            float yaw = owner.getYHeadRot();
            float pitch = owner.getXRot();

            // 计算方向向量
            float yawRad = yaw * Mth.DEG_TO_RAD;
            float pitchRad = pitch * Mth.DEG_TO_RAD;
            double xzLen = Math.cos(pitchRad);
            double x = -Math.sin(yawRad) * xzLen;
            double y = -Math.sin(pitchRad);
            double z = Math.cos(yawRad) * xzLen;
            shootDir = new Vec3(x, y, z).normalize();

            // 设置旋转（和方向一致）
            finalYaw = -yaw;
            finalPitch = -pitch;
        } else {
            // ==============================================
            // 核心修复：change=false 时，直接用 initialYaw/initialPitch 计算，不再加负号
            // ==============================================
            float yawRad = initialYaw * Mth.DEG_TO_RAD;
            float pitchRad = initialPitch * Mth.DEG_TO_RAD;
            double xzLen = Math.cos(pitchRad);

            // 这里的计算要和 initializeEntity 里的 setDeltaMovement 完全一致
            double x = -Math.sin(yawRad) * xzLen;
            double y = -Math.sin(pitchRad);
            double z = Math.cos(yawRad) * xzLen;
            shootDir = new Vec3(x, y, z).normalize();

            // ==============================================
            // 旋转改用与实际发射速度同款的“速度朝向”约定（与 updateRotationFromVelocity 一致），
            // 而非直接使用 initialYaw/initialPitch（那是 MC 视线约定，与飞行转向约定互为 x 镜像）。
            // 否则发射 tick 与首个追踪 tick 之间会出现一次可见的旋转跳变。
            // ==============================================
            double hh = Math.sqrt(shootDir.x * shootDir.x + shootDir.z * shootDir.z);
            finalYaw = (float) (Mth.atan2(shootDir.x, shootDir.z) * Mth.RAD_TO_DEG);
            finalPitch = (float) (Mth.atan2(shootDir.y, hh) * Mth.RAD_TO_DEG);
        }

        if (shootDir == null || shootDir.lengthSqr() < 1.0E-6 || !Double.isFinite(shootDir.lengthSqr())) {
            discard();
            return;
        }

        this.shoot(shootDir.x, shootDir.y, shootDir.z, getSpeed(), 0);
        setRotationSafe(finalYaw, finalPitch);
    }

    private void handleFlyingPhase() {
        if (useCustomDirection() && !autoTargetingEnabled()) {
            // 仅“固定自定义方向且关掉自动索敌”时才不追踪（全仓只有 BlackHoleAttack 走这里且它开了自动索敌）
            maintainCustomDirection();
            return;
        }
        handleSmartTracking();
    }

    /**
     * 统一飞行追踪：捕获到目标就逐渐弧过去并结算命中；无目标或目标已死亡则保持当时方向直线飞行。
     */
    protected void handleSmartTracking() {
        if (isMagneticStorm()) {
            handleMagneticTracking();
            return;
        }
        Entity t = resolveHomingTarget();
        if (t == null) {
            keepCurrentCourse();
            return;
        }
        steerTowardsTarget(t);
        checkDirectHit(t);
    }

    /**
     * 「磁暴」样式的平滑追踪：冲刺 → 恒定角速度平滑转向（限速）→ 对准后直线加速冲刺。
     *
     * <p>与默认 {@link #steerTowardsTarget}（转角随距离自适应）的区别：转角是<b>恒定角速度</b>，
     * 轨迹是一段圆弧；夹角小于 {@link #MAG_ALIGN_EPS} 后直接把方向设为指向目标，末段是严格直线；
     * 速度全程指数缓动（转向时限速以压小转弯半径，对准后加速到 {@link #MAG_MAX_SPEED}）。
     * 因此既不会绕大圈飞走，也不会出现方向突跳。</p>
     */
    private void handleMagneticTracking() {
        Entity t = resolveHomingTarget();
        if (t == null) {
            keepCurrentCourse();
            return;
        }
        magneticFlightTicks++;
        Vec3 vel = getDeltaMovement();
        double spd = vel.length();
        if (magneticFlightTicks <= MAG_BLAST_TICKS) {
            // 冲刺段：方向不变，只把速度缓动到冲刺速度
            if (spd > 1.0E-4) {
                setDeltaMovement(vel.scale((spd + (MAG_TURN_SPEED - spd) * MAG_SPEED_EASE) / spd));
            }
            return;
        }

        Vec3 pos = position();
        Vec3 toTarget = magneticAimPoint(t).subtract(pos);
        double dist = toTarget.length();
        if (dist < 1.0E-3) {
            return;
        }
        Vec3 want = toTarget.scale(1.0 / dist);
        Vec3 cur = spd < 1.0E-4 ? want : vel.scale(1.0 / spd);

        double remaining = Math.acos(Mth.clamp(cur.dot(want), -1.0, 1.0));
        boolean aligned = remaining < MAG_ALIGN_EPS;
        // 尊重各自配置的速度：比默认上限更快的剑（如炯恻之眼的 10.0）保持自己的冲刺速度
        double cruise = Math.max(getSpeed(), MAG_MAX_SPEED);
        double turnSpeed = Math.min(cruise, MAG_TURN_SPEED);
        double desired = aligned ? cruise : turnSpeed;
        double nextSpeed = spd + (desired - spd) * MAG_SPEED_EASE;

        double step = Math.min(remaining, MAG_TURN_MAX);
        // 目标已经进入「约 1.5 tick 的位移内」时直接对准目标中心：
        // 否则高速下这一 tick 的位移会从碰撞箱旁边擦过去（近距离擦身/绕圈都由此而来）。
        if (dist <= nextSpeed * 1.5) {
            step = remaining;
        }
        Vec3 newDir = rotateTowards(cur, want, step);
        setDeltaMovement(newDir.scale(nextSpeed));

        if (checkDirectHit(t)) {
            return;
        }
        updateRotationFromVelocity(newDir);
    }

    /** 磁暴样式的瞄准点：静止目标取身体中心；移动目标按飞行时间做短提前量。 */
    private Vec3 magneticAimPoint(Entity target) {
        Vec3 center = calculateTargetPosition(target);
        Vec3 move = target.getDeltaMovement();
        if (move.lengthSqr() < 1.0E-6) {
            return center;
        }
        double lead = Math.min(position().distanceTo(center) / MAG_MAX_SPEED, 6.0);
        return center.add(move.scale(lead));
    }

    // ==============================================
    // 追踪目标捕获：优先级 外部硬锁定 → 玩家重锋锁定 → 近身搜索。
    // 一旦捕获就<b>粘住不换手</b>；目标死亡/被移除后置 homingLost，此后不再重新捕获，
    // 剑按当时自身方向直线飞行，直到自然消亡（lifeTime）或路上撞到其它实体命中。
    // ==============================================
    protected Entity resolveHomingTarget() {
        if (homingLost) {
            return null;
        }
        if (homingTarget != null) {
            if (homingTarget.isAlive() && !isOwnerRelated(homingTarget)) {
                return homingTarget;
            }
            // 目标死亡/失效：放弃追踪，不换手
            homingTarget = null;
            homingLost = true;
            onHomingTargetLost();
            return null;
        }
        Entity t = getLockTarget().orElse(null);
        if (t == null) {
            t = resolveOwnerBladeLock();
        }
        if (t == null) {
            if (searchCooldown <= 0) {
                t = searchNearbyTarget();
                searchCooldown = SEARCH_COOLDOWN;
            } else {
                searchCooldown--;
            }
        }
        if (t != null && t.isAlive() && !isOwnerRelated(t)) {
            homingTarget = t;
            return t;
        }
        return null;
    }

    /** 捕获过的目标死亡/丢失时的钩子（默认无副作用；如十二连在此关闭穿墙）。 */
    protected void onHomingTargetLost() {
    }

    /** 无目标/目标已死亡：不改变速度（不弧回也不变向），仅让朝向跟住当前速度向量。 */
    protected void keepCurrentCourse() {
        Vec3 vel = getDeltaMovement();
        if (vel.lengthSqr() < 1.0E-8) {
            return;
        }
        updateRotationFromVelocity(vel);
    }

    protected Vec3 calculateTargetPosition(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    /** 近身兜底搜索（仅在没有硬锁定、也没有玩家重锋锁定时用）：取最近的可锁定敌对生物。 */
    protected Entity searchNearbyTarget() {
        AABB box = getBoundingBox().inflate(trackingRange);
        List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e instanceof Enemy && e.isAlive() && getOwner() instanceof LivingEntity p
                        && TargetSelector.lockon.test(p, e) && !isOwnerRelated(e));
        LivingEntity best = null;
        double min = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = distanceToSqr(e);
            if (d < min) {
                min = d;
                best = e;
            }
        }
        return best;
    }

    private boolean isEntityPart(Entity e) {
        return e instanceof EnderDragonPart || e.getClass().getName().matches(".*(part|segment|section).*");
    }

    // ==============================================
    // 主体检测加固（其一）：射线源头过滤。
    // 父类（slashblade EntityAbstractSummonedSword）默认 getRayTrace 仅在出手后前 5 tick
    // 排除射手，之后释放者会变成合法命中——表现为“幻影剑插在释放者身上”。这里统一交给
    // {@link #isOwnerRelated} 判定：命中结果只要与释放者同源就置空，剑直接穿过，
    // 不扣穿透、不结算、不插地。覆盖全部子类（Enchanted/Countable/Storm/Reverse/Zenith）。
    // ==============================================
    @Override
    protected EntityHitResult getRayTrace(Vec3 from, Vec3 to) {
        EntityHitResult hit = super.getRayTrace(from, to);
        if (hit == null) {
            return null;
        }
        return isOwnerRelated(hit.getEntity()) ? null : hit;
    }

    // ==============================================
    // 主体检测加固（其二）：插附状态的四道兜底关卡。
    // 父类把剑“插附”到实体（写入 HIT_ENTITY_ID）后，其 tick 顶部每 tick 跟随该实体坐标——
    // 这就是“剑扎在发射者身上”的表现本体。因此除射线过滤外，还须保证该状态永不指向释放者一侧：
    // onHit 分派拦截、setHitEntity 写入拒绝、getHitEntity 读取自愈、doForceHitEntity 外部强制拦截。
    // 其中 doForceHitEntity 是 public 且直接构造 EntityHitResult，会完全绕过 getRayTrace。
    // ==============================================
    @Override
    protected void onHit(HitResult result) {
        if (result instanceof EntityHitResult ehr && isOwnerRelated(ehr.getEntity())) {
            // 不能只是 return：父类 tick() 的穿透循环会拿同一个命中结果再次调用本方法，
            // 而 getRayTrace 又不会排除该实体（它不在父类的 alreadyHits 里），于是原地死循环。
            advancePierceForSkippedHit();
            return;
        }
        super.onHit(result);
    }

    /**
     * 保证父类 {@code EntityAbstractSummonedSword.tick()} 的穿透重试循环一定向前推进。
     *
     * <p>父类只有在 {@code getPierce() > 0} 时才会重试，而重试能否结束取决于
     * {@code super.onHitEntity()} 里写入的 {@code alreadyHits} 与 {@code setPierce(pierce-1)}。
     * 因此凡是“主动跳过这次命中”的早退分支（同源目标、已处理目标、非敌对实体……）都必须调用本方法：
     * 有穿透配额就消耗一次，配额为 0 时什么都不做（此时父类本身只跑一趟，不会重试）。
     * 漏调用会让服务端主线程在单个 tick 里无限循环、游戏卡死。</p>
     */
    protected void advancePierceForSkippedHit() {
        byte pierce = this.getPierce();
        if (pierce > 0) {
            this.setPierce((byte) (pierce - 1));
        }
    }

    @Override
    public void setHitEntity(Entity entity) {
        // 父类实现依赖 entity.getId()，null 会直接 NPE；同源对象则一律拒绝写入插附状态
        if (entity == null || isOwnerRelated(entity)) {
            return;
        }
        super.setHitEntity(entity);
    }

    @Override
    public Entity getHitEntity() {
        Entity hit = super.getHitEntity();
        // 读取端自愈：历史误插（含老存档、骑乘载具判定遗漏）到释放者一侧时视为未插附，恢复自由飞行
        return isOwnerRelated(hit) ? null : hit;
    }

    @Override
    public void doForceHitEntity(Entity entity) {
        if (isOwnerRelated(entity)) {
            return;
        }
        super.doForceHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult res) {
        Entity t = getMainEntity(res.getEntity());
        // 穿过释放者同源对象：不结算、不标记目标、不销毁，避免刚生成即撞到主人而自毁。
        if (isOwnerRelated(res.getEntity()) || isOwnerRelated(t)) {
            advancePierceForSkippedHit(); // 跳过命中但仍需推进父类穿透循环，否则单 tick 死循环
            return;
        }
        if (currentMainTarget != null && currentMainTarget.equals(t)) {
            advancePierceForSkippedHit(); // 同一目标重复命中：同样必须推进，否则死循环
            return;
        }
        currentMainTarget = t;
        applyHitEffects(t);
        discardSafely();
    }

    protected void applyHitEffects(Entity e) {
        if (level().isClientSide()) return;
        // 召唤剑不得对释放者同源对象生效（本人 / 坐骑载具 / 其所属女仆·宠物，伤害·眩晕·转移一律跳过）
        if (isOwnerRelated(e)) return;
        if (e instanceof LivingEntity living) {
            float dmg = (float) getDamage();
            dmg += sharpnessLevel * 1.25f;
            if (living.getMobType() == MobType.UNDEAD) dmg += smiteLevel * 2.5f;
            else if (living.getMobType() == MobType.ARTHROPOD) dmg += baneLevel * 2.5f;
            setDamage(dmg);

            KnockBacks.cancel.action.accept(living);
            StunManager.setStun(living);
            BlackHoleUtil.addBlackHoleCount(living, 1);

            if (level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.FIREWORK, living.getX(), living.getY(1), living.getZ(), 1, 0,0,0,0);

            super.onHitEntity(new EntityHitResult(living));
            // 命中钩子：子类可在此追加“命中时转移到目标”的效果（如负向神力转移 debuff）。
            onTargetHit(living);
        }
    }

    /** 命中生效后的扩展钩子（父类默认空实现）。{@code target} 已归并到本体、为服务端存活的生物。 */
    protected void onTargetHit(LivingEntity target) {
    }

    protected boolean checkDirectHit(Entity target) {
        if (isDiscarded || isEntityPart(target)) return false;
        // 追踪目标为释放者同源时按“未命中”处理：既不误伤也不自毁，交由射线/插附关卡让其穿过
        if (isOwnerRelated(target)) return false;
        Entity main = getMainEntity(target);
        if (currentMainTarget != null && currentMainTarget.equals(main)) return false;

        AABB box = main.getBoundingBox().inflate(0.3);
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        if (box.intersects(getBoundingBox()) || box.clip(from, to).isPresent()) {
            this.isDiscarded = true; // 提前标记，防止重入
            applyHitEffects(main);
            discardSafely(); // 确保销毁
            return true;
        }
        return false;
    }

    private void discardSafely() {
        isDiscarded = true;
        discard();
    }

    // ==============================================
    // 逐渐追踪（自适应转弯半径）
    //   旧实现把方向按 directTrackingPrecision=0.98 一次性拉到目标方向（近乎瞬转），
    //   而十二连那种“固定角速度”又让转弯半径 r=v/ω 与速度成正比、与距离无关：
    //   一旦 r 大于到目标的距离，纯追踪不可能收敛，剑只会绕着目标画大圈飘走。
    //   这里改为每 tick 限角，且让 r 始终不超过 dist×TURN_RADIUS_RATIO，保证一定能弧回来。
    // ==============================================
    protected void steerTowardsTarget(Entity target) {
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        double spd = vel.length();
        if (spd < 1.0E-4) return;

        Vec3 tgt = calculateTargetPosition(target);
        Vec3 diff = tgt.subtract(pos);
        double dist = diff.length();
        if (dist < 1.0E-3) return;

        Vec3 cur = vel.scale(1.0 / spd);
        // 预测点：按飞行时间外推目标位移，以便截住移动中的生物
        double time = Math.min(dist / spd, 1.0);
        Vec3 pred = tgt.add(target.getDeltaMovement().scale(time * 0.8));
        Vec3 wantDiff = pred.subtract(pos);
        if (wantDiff.lengthSqr() < 1.0E-12) return;
        Vec3 want = wantDiff.normalize();

        Vec3 fin = rotateTowards(cur, want, maxTurnStepFor(dist, spd));
        setDeltaMovement(fin.scale(spd));
        updateRotationFromVelocity(fin);
    }

    /** 每 tick 允许的最大转角：由「转弯半径 r=v/ω 不超过 dist×比例」反解，再夹到上下限之间。 */
    protected static double maxTurnStepFor(double dist, double spd) {
        double desiredRadius = Math.max(dist * TURN_RADIUS_RATIO, 0.5);
        return Mth.clamp(spd / desiredRadius, MIN_TURN_STEP, MAX_TURN_STEP);
    }

    /**
     * a 朝 b 最多转 maxAngle 弧度（等价球面插值）。
     * <p><b>不能</b>用“按角度占比线性插值后归一化”：当 a 与 b 接近反向时
     * {@code a.lerp(b, t)}（t&lt;0.5）归一化后仍是 a，<b>转角恒为 0</b>，追踪彻底锁死在原方向——
     * 十二连的初始方向正是“由锥顶(目标)指向生成位置”，与“指向目标”严格反向，
     * 于是一把都不回头（浮点误差决定偏多少，表现为“有概率不返回”）。
     * 这里改为绕 {@code a×b} 轴做 Rodrigues 旋转，数值稳定；仅当叉乘退化（平行/反平行）时特殊处理。
     */
    protected static Vec3 rotateTowards(Vec3 a, Vec3 b, double maxAngle) {
        double dot = Mth.clamp(a.dot(b), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= maxAngle || angle < 1.0E-4) {
            return b;
        }
        Vec3 axis = a.cross(b);
        if (axis.lengthSqr() < 1.0E-12) {
            // 严格反向：绕任一垂直轴转，转向方位不确定，但保证一定转得动
            return rotateAroundAxis(a, anyPerpendicular(a), maxAngle);
        }
        return rotateAroundAxis(a, axis.normalize(), maxAngle);
    }

    /** 绕单位轴 k 转 angle 弧度；k 与 v 垂直，故 Rodrigues 公式的 (k·v)k 项为 0。 */
    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 k, double angle) {
        return v.scale(Math.cos(angle)).add(k.cross(v).scale(Math.sin(angle))).normalize();
    }

    /**
     * 取一条与 v 垂直的单位轴。
     * <p>旧实现用「与 v.x 比较后取世界 X/Y 轴」做叉乘，导致所有反向方向共享同一近似旋转轴——
     * 十二连的 12 把剑初始方向与目标方向严格反向，全部走这里，于是统一朝同一侧偏转，
     * 失去各自的方位感。这里改为取「v 水平分量绕 Y 轴旋转 90°」作为水平轴，
     * 使每把剑绕自己的方位翻转（从「向上外」翻到「水平外」再翻到「向下内」），
     * 保持各自的方位角 θ，从而散得开、汇得拢。</p>
     */
    private static Vec3 anyPerpendicular(Vec3 v) {
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        if (h < 1.0E-4) {
            // 纯竖直方向没有“方位”，任取一条水平轴
            return new Vec3(1.0, 0.0, 0.0);
        }
        // 水平轴 = 水平分量绕 Y 轴旋转 90°：保留方位角 θ 信息
        return new Vec3(v.z / h, 0.0, -v.x / h);
    }

    // ==============================================
    // 核心修复7：旋转更新用安全方法，确保生效
    // ==============================================
    private void updateRotationFromVelocity(Vec3 vel) {
        double h = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (h < 1.0E-6 && Math.abs(vel.y) < 1.0E-6) {
            return; // 零速度，不更新旋转
        }
        float y = Mth.wrapDegrees((float) Mth.atan2(vel.x, vel.z) * Mth.RAD_TO_DEG);
        float p = Mth.wrapDegrees((float) Mth.atan2(vel.y, h) * Mth.RAD_TO_DEG);
        setRotationSafe(y, p);
    }

    private void extractEnchantments() {
        if (!(getOwner() instanceof LivingEntity p)) return;
        ItemStack stack = p.getMainHandItem();
        if (stack.isEmpty()) return;
        Map<Enchantment, Integer> ench = EnchantmentHelper.getEnchantments(stack);
        sharpnessLevel = ench.getOrDefault(Enchantments.SHARPNESS, 0);
        smiteLevel = ench.getOrDefault(Enchantments.SMITE, 0);
        baneLevel = ench.getOrDefault(Enchantments.BANE_OF_ARTHROPODS, 0);
    }

    public static void spawnSwords(LivingEntity owner, Level world, Vec3 center, SpawnMode mode, int count,
                                   boolean change, float y, float p, float zj, float k,
                                   double dmg, int color, boolean clip, int delay) {
        // 默认开启「磁暴样式」：所有来源的幻影剑统一使用平滑追踪 + 拖尾
        spawnSwords(owner, world, center, mode, count, change, y, p, zj, k, dmg, color, clip, delay, true);
    }

    /**
     * @param magneticStorm 是否为「磁暴」样式（加速冲刺 + 平滑追踪 + 拖尾）。只影响飞行与渲染，
     *                      生成位置 / 延迟 / 颜色等参数与原来完全一致。
     */
    public static void spawnSwords(LivingEntity owner, Level world, Vec3 center, SpawnMode mode, int count,
                                   boolean change, float y, float p, float zj, float k,
                                   double dmg, int color, boolean clip, int delay, boolean magneticStorm) {
        if (owner == null || !owner.isAlive()) return;
        for (int i = 0; i < count; i++) {
            EntityNRBlisteringSword sword = new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, world);
            sword.setOwner(owner);
            sword.setCenterPosition(center);
            sword.spawnMode = mode;
            sword.lifeTime = 200;
            sword.setChange(change);
            sword.setDamage(dmg);
            sword.setColor(color);
            sword.setDelayTicks(delay);
            sword.setNoClip(clip); // 原生NoClip，完全兼容
            sword.setMagneticStorm(magneticStorm);

            if (y != 0 || p != 0) {
                sword.setYAW(y + k * i + zj);
                sword.setPITCH(p + k * i + zj);
            }

            Vec3 spawnPos;
            if (mode == SpawnMode.RANDOM) {
                double rx = center.x + (world.random.nextDouble() - 0.5) * 5;
                double ry = center.y + world.random.nextDouble() * 5;
                double rz = center.z + (world.random.nextDouble() - 0.5) * 5;
                spawnPos = new Vec3(rx, ry, rz);
            } else {
                double ang = Math.PI * 2 * i / count;
                spawnPos = center.add(3 * Math.cos(ang), 0, 3 * Math.sin(ang));
            }

            sword.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            // 初始化固定位置
            sword.fixedDelayPos = spawnPos;

            if (y == 0 && p == 0) {
                sword.setRotationSafe(-owner.getYHeadRot(), -owner.getXRot());
            } else {
                sword.setRotationSafe(-(sword.getDiyPitch() + k * i + zj), -(sword.getDiyYaw() + k * i + zj));
            }

            world.addFreshEntity(sword);
        }
    }

    public enum SpawnMode { CIRCLE, RANDOM }
}