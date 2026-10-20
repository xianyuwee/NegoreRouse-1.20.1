package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.ParticleTypes;
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
    private boolean isDiscarded = false;

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
        this.setPierce((byte) 5);
        this.setNoGravity(true);
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
        if (entity instanceof EnderDragonPart part) {
            return part.getParent();
        }
        try {
            Field parentField = entity.getClass().getDeclaredField("parent");
            parentField.setAccessible(true);
            Object parent = parentField.get(entity);
            if (parent instanceof Entity) return (Entity) parent;
        } catch (Exception ignored) {}
        try {
            Method getParent = entity.getClass().getMethod("getParent");
            Object parent = getParent.invoke(entity);
            if (parent instanceof Entity) return (Entity) parent;
        } catch (Exception ignored) {}
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
        Entity t = resolveHomingTarget();
        if (t == null) {
            keepCurrentCourse();
            return;
        }
        steerTowardsTarget(t);
        checkDirectHit(t);
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
            return;
        }
        super.onHit(result);
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
        if (isOwnerRelated(res.getEntity()) || isOwnerRelated(t)) return;
        if (currentMainTarget != null && currentMainTarget.equals(t)) return;
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
            applyHitEffects(main);
            discardSafely();
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

    /** 取一条与 v 垂直的单位轴：用与 v 夹角最大的世界轴做叉乘，避免结果接近零向量。 */
    private static Vec3 anyPerpendicular(Vec3 v) {
        Vec3 ref = Math.abs(v.x) < 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 axis = v.cross(ref);
        if (axis.lengthSqr() < 1.0E-12) {
            axis = v.cross(new Vec3(0.0D, 0.0D, 1.0D));
        }
        return axis.lengthSqr() < 1.0E-12 ? new Vec3(1.0D, 0.0D, 0.0D) : axis.normalize();
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