package net.xianyu.prinegorerouse.entity;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.DamageTypeHolderUtils;

/**
 * AbsolutePower（绝对神力）激活后按 V 键（SA）生成的“幻影刃”。
 * <p>它是一把淡金色、透明、只含刀刃（{@code blade} 组）的悬浮刀，复刻触发刀（或卡俄斯被吞池）的建模与贴图。</p>
 * <p>行为状态机：无目标时低速环绕玩家；玩家命中存活目标后传送到目标附近，随机释放剑气特效并施加
 * “玩家攻击力一半”的真实伤害（{@code out_of_world}，归因玩家）；目标死亡后减速飞回环绕。寿命到自动淡出消失。</p>
 * <p>完全重写 {@link #tick()}，不借用父类的发射/追踪逻辑，仅服务端跑 AI，客户端做位置/朝向插值。</p>
 */
public class EntityAbsolutePhantom extends EntityAbstractSummonedSword {

    // ===== 同步字段 =====
    private static final EntityDataAccessor<String> MODEL_LOC =
            SynchedEntityData.defineId(EntityAbsolutePhantom.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEX_LOC =
            SynchedEntityData.defineId(EntityAbsolutePhantom.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(EntityAbsolutePhantom.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(EntityAbsolutePhantom.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> LIFE =
            SynchedEntityData.defineId(EntityAbsolutePhantom.class, EntityDataSerializers.INT);
    /** 自挥序号：服务端每次“玩家未挥刀的自主劈砍” +1，客户端据此启动一段本地挥砍动画。 */
    private static final EntityDataAccessor<Integer> SWING_SEQ =
            SynchedEntityData.defineId(EntityAbsolutePhantom.class, EntityDataSerializers.INT);

    private static final int PHASE_ORBIT = 0;
    private static final int PHASE_ATTACK = 1;
    private static final int PHASE_RETURN = 2;

    // ===== 微调旋钮 =====
    private static final double ORBIT_SPEED = 0.055;      // 环绕角速度（rad/tick，低速）
    private static final double ORBIT_BOB = 0.35;         // 竖直起伏幅度（格）
    private static final float ORBIT_PITCH = 90.0f;       // 环绕时刀身竖直（±90 使刀刃上下指向；取负则刀尖朝下）
    private static final double K_ORBIT = 0.16;           // 环绕跟随系数（加减速）
    private static final double K_RETURN = 0.30;          // 飞回跟随系数
    private static final double SMOOTH = 0.28;            // 速度平滑（越大越跟手）
    private static final int ATTACK_INTERVAL = 8;         // 每次剑技间隔（tick，玩家未挥刀时的兜底节奏）
    private static final float HIT_FRAME = 0.5F;          // 玩家挥刀进度中对齐的命中帧（视觉挥砍段末尾）
    private static final int SELF_SWING_TICKS = 6;        // 幻影刃自挥时长（tick），与玩家挥刀时长一致
    private static final double ATTACK_HOVER = 1.8;       // 悬停距目标半径（格）
    private static final double TELEPORT_MIN = 3.5;       // 距目标超过此格则再传送
    private static final double ARRIVE_EPS = 0.9;         // 到位判定阈值（需大于 K_RETURN 比例跟踪的稳态误差≈0.75，否则会卡在 RETURN）
    private static final int FADE_OUT = 8;                // 末段淡出 tick 数
    private static final int FADE_IN = 5;                 // 出生淡入 tick 数
    private static final int PHANTOM_COLOR = 0xFFD8AF4A;  // 淡金色（RGB，供渲染取色）

    private static final RandomSource RNG = RandomSource.create();

    // ===== 服务端本地字段 =====
    private int slotIndex = 0;
    private double orbitRadius = 3.0;
    private double orbitPhase = 0.0;
    private Vec3 smoothVel = Vec3.ZERO;
    private int attackCooldown = 0;
    private float prevSwingT = 0.0F;                      // 服务端：上一 tick 持有者的挥刀进度，用于命中帧检测
    private int selfSwingHit = 0;                         // 服务端：自挥命中帧倒计时（到 0 时结算伤害，与视觉同拍）

    // ===== 客户端本地字段：自挥动画推进 =====
    private float swingT = 0.0F;                          // 自挥进度（tick 级，0=未挥）
    private float swingTO = 0.0F;                         // 上一 tick 的自挥进度（供 partialTick 插值）
    private int lastSwingSeq = 0;                         // 已处理的挥刀序号
    private float prevOwnerSwingT = 0.0F;                 // 客户端：持有者上一 tick 挥刀进度（检测起挥沿）
    private int playerSwingCount = 0;                     // 客户端：持有者累计挥刀次数（跟随挥砍时逐挥轮换招式，模拟 combo）

    public EntityAbsolutePhantom(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setNoClip(true);
        this.setInvulnerable(true);
        this.setPierce((byte) 0);
    }

    public static EntityAbsolutePhantom createInstance(PlayMessages.SpawnEntity packet, Level level) {
        return new EntityAbsolutePhantom(NrEntitiesRegistry.Absolute_Phantom, level);
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

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        // 传送门类方块：直接穿过，不交互不消失
        net.minecraft.world.level.block.Block block = level().getBlockState(result.getBlockPos()).getBlock();
        if (block == net.minecraft.world.level.block.Blocks.NETHER_PORTAL
                || block == net.minecraft.world.level.block.Blocks.END_PORTAL
                || block == net.minecraft.world.level.block.Blocks.END_GATEWAY) {
            return;
        }
        super.onHitBlock(result);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MODEL_LOC, "");
        this.entityData.define(TEX_LOC, "");
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(PHASE, (byte) PHASE_ORBIT);
        this.entityData.define(LIFE, 0);
        this.entityData.define(SWING_SEQ, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SWING_SEQ.equals(key) && this.level().isClientSide()) {
            int seq = this.entityData.get(SWING_SEQ);
            if (seq > this.lastSwingSeq) {
                this.lastSwingSeq = seq;
                this.swingTO = 0.0F;
                this.swingT = 1.0F / SELF_SWING_TICKS; // 启动一段本地自挥动画
            }
        }
    }

    /** 供渲染：幻影刃自挥（玩家未挥刀的自主劈砍）进度，含 partialTick 插值。 */
    public float getSelfSwingAnim(float partialTick) {
        return Mth.lerp(partialTick, this.swingTO, this.swingT);
    }

    /** 供渲染：是否处于攻击相位（只有攻击中的刀才挥砍；环绕/归位保持静止姿态）。 */
    public boolean isAttackPhase() {
        return this.entityData.get(PHASE) == PHASE_ATTACK;
    }

    /** 供渲染：自挥序号（每次自主起挥 +1，用于招式轮换）。 */
    public int getSwingSeq() {
        return this.entityData.get(SWING_SEQ);
    }

    /** 供渲染：持有者累计挥刀次数（客户端按起挥沿计数，跟随挥砍时逐挥换招）。 */
    public int getPlayerSwingCount() {
        return this.playerSwingCount;
    }

    // ===== 生成参数写入 =====
    public void setupModel(ResourceLocation model, ResourceLocation tex) {
        this.entityData.set(MODEL_LOC, model == null ? "" : model.toString());
        this.entityData.set(TEX_LOC, tex == null ? "" : tex.toString());
    }

    public void setupOrbit(int slotIndex, double orbitRadius, double orbitPhase, int life) {
        this.slotIndex = slotIndex;
        this.orbitRadius = orbitRadius;
        this.orbitPhase = orbitPhase;
        this.entityData.set(LIFE, life);
    }

    public String getModelLocStr() {
        return this.entityData.get(MODEL_LOC);
    }

    public String getTextureLocStr() {
        return this.entityData.get(TEX_LOC);
    }

    public int getPhantomColor() {
        return PHANTOM_COLOR;
    }

    /** 供渲染：剩余寿命（服务端每 tick 递减并同步），用于末段淡出。 */
    public int getRemainingLife() {
        return this.entityData.get(LIFE);
    }

    /** 刷新持续时间：取当前剩余寿命与给定值的较大者（不缩短已有更长寿命）。 */
    public void refreshLife(int ticks) {
        if (this.level().isClientSide()) {
            return;
        }
        if (ticks > this.entityData.get(LIFE)) {
            this.entityData.set(LIFE, ticks);
        }
    }

    /** 命中事件调用：命令此幻影刃追击指定存活生物目标（物品/经验球等非生物忽略）。 */
    public void setCommandedTarget(Entity target) {
        if (this.level().isClientSide()) {
            return;
        }
        Entity resolved = resolveCombatTarget(target, this.getOwner());
        if (resolved == null) {
            return;
        }
        this.entityData.set(TARGET_ID, resolved.getId());
    }

    /**
     * 解析可战斗目标：把多段生物（末影龙等 {@link net.minecraftforge.entity.PartEntity}）归并到本体，
     * 仅接受存活的 {@link LivingEntity}，并排除持有者自身；其余（物品/经验球/掉落物等）返回 {@code null}。
     */
    public static Entity resolveCombatTarget(Entity raw, Entity owner) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof net.minecraftforge.entity.PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent != null) {
                raw = parent;
            }
        }
        if (!(raw instanceof LivingEntity living) || !living.isAlive()) {
            return null;
        }
        if (owner != null && raw.getId() == owner.getId()) {
            return null; // 不追击持有者自己
        }
        // 女仆/宠物保护：不追击、不攻击持有者所属的驯服生物（Touhou Little Maid 等）
        if (MaidCompat.isOwnedBy(raw, owner)) {
            return null;
        }
        return raw;
    }

    @Override
    public void tick() {
        // 注意：xOld/yOld/zOld、yRotO/xRotO 与 tickCount 已由 Level.tickNonPassenger
        // 在调用本方法前完成快照/自增（setOldPosAndRot + ++tickCount），此处不得重复处理，
        // 否则 tickCount 会双倍自增导致动画节奏错乱。
        if (this.level().isClientSide) {
            // 客户端：无 AI；位置由服务端每 tick 同步（updateInterval=1）直接落位，
            // 渲染层用 xOld->getX 的偏 tick 插值实现平滑运动，无需在此手动推进。
            // 唯一例外：本地自挥进度逐 tick 推进（与玩家 getAttackAnim 同为 tick 级 + pt 插值），
            // 并检测持有者起挥沿计数挥刀次数（供渲染层逐挥轮换招式）。
            this.swingTO = this.swingT;
            if (this.swingT > 0.0F) {
                this.swingT += 1.0F / SELF_SWING_TICKS;
                if (this.swingT >= 1.0F) {
                    this.swingT = 0.0F;
                }
            }
            float ownerT = 0.0F;
            if (this.getOwner() instanceof LivingEntity livingOwner) {
                ownerT = livingOwner.getAttackAnim(0.0F);
            }
            if (ownerT > 0.0F && this.prevOwnerSwingT <= 0.0F) {
                this.playerSwingCount++;
            }
            this.prevOwnerSwingT = ownerT;
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) {
            this.discard();
            return;
        }

        int life = this.entityData.get(LIFE) - 1;
        this.entityData.set(LIFE, life);
        if (life <= 0) {
            this.discard();
            return;
        }

        int phase = this.entityData.get(PHASE);
        switch (phase) {
            case PHASE_ATTACK -> tickAttack(livingOwner);
            case PHASE_RETURN -> tickReturn(livingOwner);
            default -> tickOrbit(livingOwner);
        }

        // 服务端权威推进：steerTo 只写了 deltaMovement，这里落到坐标；
        // teleportNear 已直接 setPos 并把 dv 归零，故其本 tick 不再叠加位移。
        Vec3 dv = this.getDeltaMovement();
        if (!Double.isFinite(dv.x) || !Double.isFinite(dv.y) || !Double.isFinite(dv.z)) {
            this.discard();
            return;
        }
        if (dv.lengthSqr() > 1.0E-8) {
            this.setPos(this.getX() + dv.x, this.getY() + dv.y, this.getZ() + dv.z);
        }
    }

    // ===== ORBIT：低速环绕玩家；若被命令的目标存活则切换到 ATTACK =====
    private void tickOrbit(LivingEntity owner) {
        Entity target = currentTarget();
        if (target != null && target.isAlive()) {
            teleportNear(target);
            this.entityData.set(PHASE, (byte) PHASE_ATTACK);
            this.attackCooldown = 0;
            spawnBurst(this.position(), 6, ParticleTypes.PORTAL);
            return;
        }

        Vec3 center = owner.getEyePosition().add(0, -0.2, 0);
        double ang = this.orbitPhase;
        Vec3 desired = center.add(
                Math.cos(ang) * this.orbitRadius,
                Math.sin(this.tickCount * 0.1 + this.slotIndex) * ORBIT_BOB,
                Math.sin(ang) * this.orbitRadius);
        this.orbitPhase += ORBIT_SPEED;

        steerTo(desired, K_ORBIT);
        // 刀身竖直立起：pitch=ORBIT_PITCH(±90) 使刀刃上下指向；yaw 取从玩家指向刀的水平方位角
        Vec3 outward = desired.subtract(center);
        double yaw = Mth.atan2(outward.z, outward.x) * Mth.RAD_TO_DEG - 90.0;
        faceDir((float) yaw, ORBIT_PITCH, 0.0f);
    }

    // ===== ATTACK：悬停于目标附近，周期性随机剑技 + 真实伤害；目标死亡切 RETURN =====
    private void tickAttack(LivingEntity owner) {
        Entity target = currentTarget();
        if (target == null || !target.isAlive()) {
            this.entityData.set(TARGET_ID, -1);
            this.entityData.set(PHASE, (byte) PHASE_RETURN);
            return;
        }

        Vec3 tc = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double dist = this.position().distanceTo(tc);
        // 目标移动较远时再传送贴住
        if (dist > TELEPORT_MIN) {
            spawnBurst(this.position(), 5, ParticleTypes.PORTAL);
            teleportNear(target);
            spawnBurst(this.position(), 5, ParticleTypes.PORTAL);
        } else {
            // 悬停：围绕目标缓慢游移
            double ang = this.tickCount * 0.12 + this.slotIndex;
            Vec3 hover = tc.add(Math.cos(ang) * ATTACK_HOVER, Math.sin(ang * 0.7) * 0.4, Math.sin(ang) * ATTACK_HOVER);
            steerTo(hover, K_ORBIT);
        }

        // 面朝目标（刀尖指向目标）；roll 固定 0，挥砍视觉由渲染层按玩家挥刀进度驱动
        Vec3 toTarget = tc.subtract(this.position());
        double h = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        float yaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * Mth.RAD_TO_DEG) - 90.0f;
        float pitch = (float) (-Mth.atan2(toTarget.y, h) * Mth.RAD_TO_DEG);
        faceDir(yaw, pitch, 0.0f);

        // 伤害时机与玩家看齐：玩家挥刀时在命中帧触发（与视觉挥砍同节奏）；未挥刀则自主起挥，
        // 同样在自挥命中帧结算，保证“先挥砍、后出伤”的视觉一致
        float tNow = owner.getAttackAnim(0.0F);
        if (tNow > 0.0F) {
            if (this.prevSwingT < HIT_FRAME && tNow >= HIT_FRAME) {
                releaseSlash(owner, target, tc);
            }
            this.prevSwingT = tNow;
            this.attackCooldown = ATTACK_INTERVAL;
            this.selfSwingHit = 0; // 玩家接管节奏，中止自挥的伤害调度
        } else {
            this.prevSwingT = 0.0F;
            if (this.selfSwingHit > 0) {
                if (--this.selfSwingHit == 0) {
                    releaseSlash(owner, target, tc);
                }
            } else if (--this.attackCooldown <= 0) {
                this.attackCooldown = ATTACK_INTERVAL;
                this.entityData.set(SWING_SEQ, this.entityData.get(SWING_SEQ) + 1); // 通知客户端起挥
                this.selfSwingHit = Math.max(1, (int) (SELF_SWING_TICKS * HIT_FRAME)); // 命中帧再结算
            }
        }
    }

    // ===== RETURN：减速飞回环绕槽位；到位切 ORBIT =====
    private void tickReturn(LivingEntity owner) {
        Vec3 center = owner.getEyePosition().add(0, -0.2, 0);
        double ang = this.orbitPhase;
        Vec3 desired = center.add(
                Math.cos(ang) * this.orbitRadius,
                Math.sin(this.tickCount * 0.1 + this.slotIndex) * ORBIT_BOB,
                Math.sin(ang) * this.orbitRadius);
        this.orbitPhase += ORBIT_SPEED;

        steerTo(desired, K_RETURN);

        // 归途也保持竖直（刀尖指向归位点会近水平，卡相位时看起来就是平躺）；yaw 仍朝飞行方向
        Vec3 outward = desired.subtract(this.position());
        float yaw = (float) (Mth.atan2(outward.z, outward.x) * Mth.RAD_TO_DEG) - 90.0f;
        faceDir(yaw, ORBIT_PITCH, 0.0f);

        // 拖尾
        if (this.tickCount % 6 == 0) {
            spawnBurst(this.position(), 1, ParticleTypes.END_ROD);
        }

        if (this.position().distanceToSqr(desired) < ARRIVE_EPS * ARRIVE_EPS) {
            this.entityData.set(PHASE, (byte) PHASE_ORBIT);
        }
    }

    // ===== 一次“剑技”：随机朝向的剑气视觉特效 + 真实伤害（唯一伤害来源） =====
    private void releaseSlash(LivingEntity owner, Entity target, Vec3 tc) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        // 视觉：伤害设为 0，仅出特效；不干扰下方自控的真实伤害
        EntitySlashEffect fx = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, this.level());
        fx.setOwner(owner);
        fx.setShooter(owner);
        fx.setPos(tc.x, tc.y, tc.z);
        fx.setDamage(0);
        fx.setNoClip(true);
        fx.setMute(true);
        fx.setLifetime(6);
        fx.setBaseSize(1.1f);
        fx.setColor(PHANTOM_COLOR);
        fx.setRotationRoll(RNG.nextFloat() * 360.0f);
        fx.setRotationOffset(RNG.nextFloat() * 120.0f - 60.0f);
        sl.addFreshEntity(fx);

        // 伤害：玩家攻击力一半的真实伤害，归因玩家；女仆/宠物（持有者所属驯服生物）免疫
        if (target instanceof LivingEntity living && !MaidCompat.isOwnedBy(target, owner)) {
            float half = (float) (owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5);
            if (half > 0) {
                living.hurt(new DamageSource(
                        DamageTypeHolderUtils.getHolder(this.level(), "out_of_world"), this, owner), half);
            }
        }
        spawnBurst(tc, 3, ParticleTypes.END_ROD);
    }

    private void teleportNear(Entity target) {
        Vec3 tc = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double a = RNG.nextDouble() * Math.PI * 2.0;
        double r = 1.5 + RNG.nextDouble() * 0.5;
        Vec3 pos = tc.add(Math.cos(a) * r, (RNG.nextDouble() - 0.5) * 1.2, Math.sin(a) * r);
        this.setPos(pos.x, pos.y, pos.z);
        this.smoothVel = Vec3.ZERO;
        this.setDeltaMovement(Vec3.ZERO);
    }

    private Entity currentTarget() {
        int id = this.entityData.get(TARGET_ID);
        if (id == -1) {
            return null;
        }
        Entity e = this.level().getEntity(id);
        // 目标必须是存活生物，否则视为无目标（切回环绕），避免对物品等“摸鱼”空转
        return (e instanceof LivingEntity && e.isAlive()) ? e : null;
    }

    /** 位置控制：朝 desired 点做带加/减速的平滑跟随（比例速度 → 近处自然减速）。 */
    private void steerTo(Vec3 desired, double k) {
        Vec3 cur = this.position();
        Vec3 want = desired.subtract(cur).scale(k);
        this.smoothVel = this.smoothVel.lerp(want, SMOOTH);
        this.setDeltaMovement(this.smoothVel);
    }

    private void faceDir(float yaw, float pitch, float roll) {
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setRoll(roll);
    }

    private void spawnBurst(Vec3 pos, int count, SimpleParticleType type) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(type, pos.x, pos.y, pos.z, count, 0.25, 0.25, 0.25, 0.05);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 128.0 * 128.0;
    }

    // 幻影刃不参与父类的 NBT 模型/延迟字段持久化；仅保留必要的整体保存
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AbsLife", this.entityData.get(LIFE));
        tag.putString("AbsModel", this.entityData.get(MODEL_LOC));
        tag.putString("AbsTex", this.entityData.get(TEX_LOC));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AbsLife")) {
            this.entityData.set(LIFE, tag.getInt("AbsLife"));
        }
        this.entityData.set(MODEL_LOC, tag.getString("AbsModel"));
        this.entityData.set(TEX_LOC, tag.getString("AbsTex"));
    }
}
