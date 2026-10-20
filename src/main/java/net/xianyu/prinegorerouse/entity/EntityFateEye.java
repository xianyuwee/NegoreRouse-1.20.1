package net.xianyu.prinegorerouse.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.PlayMessages;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

/**
 * 炯恻（Fate）激活后由服务端生成的“眼”实体：一个<b>固定位置、不可被伤害、无重力</b>的标记型实体。
 * <p>行为状态机（全部服务端驱动，仅 {@code OPEN} 开合进度同步给客户端渲染）：</p>
 * <ul>
 *   <li><b>OPENING</b>：{@code OPEN} 由 0 缓入到 1（约 {@code OPEN_ANIM} tick）——闭眼→睁眼动画。</li>
 *   <li><b>HOLD</b>：{@code OPEN=1}，最多停留 {@code MAX_OPEN_TICKS}；期间<b>锁定生成时那只生物</b>不换目标，
 *       双路同时输出——每 {@code LASER_INTERVAL} tick 一次激光伤害（{@code 面板 × LASER_RATIO}，归因释放者），
 *       每 {@code SWORD_INTERVAL} tick 生成一支追踪剑（{@code SWORD_DELAY} tick 后发射，硬锁定该目标）。</li>
 *   <li><b>CLOSING</b>：目标死亡 / 距离超过 {@code MAX_DIST} / HOLD 计时到 → {@code OPEN} 缓出到 0（{@code CLOSE_ANIM} tick）
 *       后 {@code discard()}——睁眼→闭眼+缓出消失。</li>
 * </ul>
 * <p>位置在生成时一次性固定，此后不再移动；渲染（眼球贴图 crossfade + 激光光束）见 {@code FateEyeRenderer}。</p>
 */
public class EntityFateEye extends Entity {

    // ===== 同步字段 =====
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(EntityFateEye.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(EntityFateEye.class, EntityDataSerializers.INT);
    /** 开合进度 0~1（服务端逐 tick 写入，驱动客户端闭↔睁 crossfade 与光束显隐）。 */
    private static final EntityDataAccessor<Float> OPEN =
            SynchedEntityData.defineId(EntityFateEye.class, EntityDataSerializers.FLOAT);

    // ===== 微调旋钮 =====
    private static final int MAX_OPEN_TICKS = 31;   // HOLD 期最长停留
    private static final int LASER_INTERVAL = 12;   // 激光伤害间隔（降低频率）
    private static final int SWORD_INTERVAL = 6;    // 追踪剑生成间隔（降低频率）
    private static final int SWORD_DELAY = 0;       // 追踪剑发射延迟（0=生成即发：父类延迟阶段会把旋转取反并同步，出现可见翻转）
    private static final int OPEN_ANIM = 6;         // 睁眼动画 tick
    private static final int CLOSE_ANIM = 6;        // 闭眼缓出 tick
    private static final double MAX_DIST = 10.0;    // 目标过远判定
    private static final double SWORD_RING_RADIUS = 1.0; // 追踪剑环绕生成的环半径（格）
    private static final float LASER_RATIO = 0.4F;  // 激光伤害 = 面板 × 此比例
    private static final int SWORD_COLOR = 16766720; // 追踪剑颜色（与 Fate 主手分支一致）

    private static final int PHASE_OPENING = 0;
    private static final int PHASE_HOLD = 1;
    private static final int PHASE_CLOSING = 2;

    // ===== 服务端本地字段 =====
    private int phase = PHASE_OPENING;
    private int animTicks = 0;   // 当前相位内累计 tick（OPENING/HOLD/CLOSING 各自复用）
    private int laserTimer = 0;
    private int swordTimer = 0;

    public EntityFateEye(EntityType<? extends Entity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        // 贴地/贴空悬浮：不参与碰撞推动，位置生成后固定
        this.setDeltaMovement(Vec3.ZERO);
    }

    public static EntityFateEye createInstance(PlayMessages.SpawnEntity packet, Level level) {
        return new EntityFateEye(NrEntitiesRegistry.Fate_Eye, level);
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
    protected void defineSynchedData() {
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(OPEN, 0.0F);
    }

    // ===== 生成参数写入（服务端） =====
    public void setup(LivingEntity owner, Entity target) {
        this.entityData.set(OWNER_ID, owner == null ? -1 : owner.getId());
        this.entityData.set(TARGET_ID, target == null ? -1 : target.getId());
    }

    private LivingEntity ownerLiving() {
        Entity e = this.level().getEntity(this.entityData.get(OWNER_ID));
        return e instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    /** 供渲染与逻辑：当前锁定目标（存活生物），否则 {@code null}。 */
    public Entity getTargetEntity() {
        int id = this.entityData.get(TARGET_ID);
        if (id == -1) {
            return null;
        }
        Entity e = this.level().getEntity(id);
        if (e instanceof PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent != null) {
                e = parent;
            }
        }
        return e != null && e.isAlive() ? e : null;
    }

    /** 供渲染：含 partialTick 插值的开合进度 0~1。 */
    public float getOpen(float partialTick) {
        return Mth.clamp(this.entityData.get(OPEN), 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide()) {
            return; // 客户端仅靠同步的 OPEN 渲染，无逻辑
        }
        switch (this.phase) {
            case PHASE_OPENING -> tickOpening();
            case PHASE_HOLD -> tickHold();
            default -> tickClosing();
        }
    }

    private void tickOpening() {
        this.animTicks++;
        this.entityData.set(OPEN, smoothstep((float) this.animTicks / OPEN_ANIM));
        if (this.animTicks >= OPEN_ANIM) {
            this.entityData.set(OPEN, 1.0F);
            this.phase = PHASE_HOLD;
            this.animTicks = 0;
            this.laserTimer = 0;
            this.swordTimer = 0;
        }
    }

    private void tickHold() {
        this.animTicks++;
        this.entityData.set(OPEN, 1.0F);

        Entity target = getTargetEntity();
        // 目标丢失 / 过远 / HOLD 超时 → 提前进入闭眼缓出
        if (target == null || this.position().distanceTo(targetCenter(target)) > MAX_DIST
                || this.animTicks >= MAX_OPEN_TICKS) {
            this.phase = PHASE_CLOSING;
            this.animTicks = 0;
            return;
        }

        LivingEntity owner = ownerLiving();
        if (owner == null) {
            this.phase = PHASE_CLOSING;
            this.animTicks = 0;
            return;
        }

        // 激光：每 LASER_INTERVAL tick 结算一次面板比例伤害，归因释放者
        if (++this.laserTimer >= LASER_INTERVAL) {
            this.laserTimer = 0;
            applyLaser(owner, target);
        }
        // 追踪剑：每 SWORD_INTERVAL tick 生成一支，硬锁定当前目标、延迟 SWORD_DELAY tick 发射
        if (++this.swordTimer >= SWORD_INTERVAL) {
            this.swordTimer = 0;
            spawnTrackingSword(owner, target);
        }
    }

    private void tickClosing() {
        this.animTicks++;
        // 缓出：OPEN 从 1 平滑降到 0（平方缓出，末端更柔）
        float t = Mth.clamp((float) this.animTicks / CLOSE_ANIM, 0.0F, 1.0F);
        this.entityData.set(OPEN, 1.0F - t * t);
        if (this.animTicks >= CLOSE_ANIM) {
            this.discard();
        }
    }

    // ===== 攻击：激光（比例面板伤害，归因释放者） =====
    private void applyLaser(LivingEntity owner, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        // 女仆/宠物保护：不对释放者所属的驯服生物（Touhou Little Maid 等）发射激光
        if (MaidCompat.isOwnedBy(target, owner)) {
            return;
        }
        float dmg = (float) (owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * LASER_RATIO);
        if (dmg <= 0.0F) {
            return;
        }
        living.hurt(attackSource(owner), dmg);
    }

    // ===== 攻击：追踪剑（伤害语义与 Fate 主手分支一致：剑命中即结算释放者全额面板） =====
    private void spawnTrackingSword(LivingEntity owner, Entity target) {
        Level level = this.level();
        EntityEnchantedSword sword = new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, level);
        sword.setOwner(owner);
        sword.setCenterPosition(this.position());
        sword.spawnMode = EntityNRBlisteringSword.SpawnMode.RANDOM;
        sword.setChange(false);
        sword.setDamage((float) (owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * LASER_RATIO));
        sword.setColor(SWORD_COLOR);
        sword.setNoClip(true);
        sword.setSpeed(10.0F);
        sword.setDelayTicks(SWORD_DELAY);
        sword.setLockTarget(target);
        // 在“以眼为中心的水平环”上随机取点生成（而非眼实体内部），附带少量竖直抖动
        double ang = this.random.nextDouble() * Math.PI * 2.0;
        Vec3 spawnPos = this.position().add(
                Math.cos(ang) * SWORD_RING_RADIUS,
                (this.random.nextDouble() - 0.5) * 0.6,
                Math.sin(ang) * SWORD_RING_RADIUS);
        sword.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        // 初始发射方向指向锁定目标（而非玩家目视方向）；父类在 diyYaw/diyPitch 非 0 时按此方向发射，
        // 之后由父类统一追踪链（本剑的硬锁定为最高优先）逐 tick 修正；目标死亡则按当时方向直线飞行。
        // 两者同时为 0 会被父类当作“未指定方向”而退回“跟随玩家视角”，故下面加极小兜底。
        Vec3 dir = targetCenter(target).subtract(spawnPos);
        if (dir.lengthSqr() > 1.0E-6) {
            dir = dir.normalize();
            float yaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (Math.asin(Mth.clamp(-dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);
            if (Math.abs(yaw) < 0.5F && Math.abs(pitch) < 0.5F) {
                yaw = 0.5F;
            }
            sword.setYAW(yaw);
            sword.setPITCH(pitch);
            // 出生朝向改用与飞行追踪同款的“速度朝向”约定（atan2(v.x,v.z)，见父类 updateRotationFromVelocity），
            // 使 出生→发射→追踪 全程同一套旋转约定；父类在 noClip 下会拦截 setYRot/setXRot，
            // 故临时关 noClip 写入（否则出生包为默认 +Z，发射时才猛地转向）。
            double hh = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            float faceYaw = (float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
            float facePitch = (float) (Mth.atan2(dir.y, hh) * Mth.RAD_TO_DEG);
            sword.setNoClip(false);
            sword.setYRot(faceYaw);
            sword.setXRot(facePitch);
            sword.setNoClip(true);
        }
        level.addFreshEntity(sword);
    }

    /** 归因释放者的伤害来源：玩家→PLAYER_ATTACK，生物→MOB_ATTACK，其余→INDIRECT_MAGIC。 */
    private DamageSource attackSource(LivingEntity owner) {
        Level level = this.level();
        Holder<DamageType> holder;
        if (owner instanceof Player) {
            holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_ATTACK);
        } else {
            holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK);
        }
        // direct=眼实体，causing=释放者（击杀归属玩家）
        return new DamageSource(holder, this, owner);
    }

    private Vec3 targetCenter(Entity target) {
        return target.position().add(0, target.getBbHeight() * 0.5, 0);
    }

    /** smoothstep 缓入（两端慢、中间快），用于睁眼动画的开合进度曲线。 */
    private static float smoothstep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    // ===== 标记型实体：不被伤害、不参与碰撞推动 =====
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 128.0 * 128.0;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, 0);
    }

    // 短寿命战斗实体（<2s）：状态靠 entityData 同步，无需额外持久化
    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }
}
