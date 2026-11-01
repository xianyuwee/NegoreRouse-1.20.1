package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.entity.EntityEnchantedSword;
import net.xianyu.prinegorerouse.entity.EntityFateEye;
import net.xianyu.prinegorerouse.entity.EntityNRBlisteringSword;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fate（炯恻）SE。
 * <p>命中表现（{@link #onSlashBladeHit}）：<b>无论主/副手</b>，只要命中所用的刀<b>携带</b>炯恻，
 * 就<b>无条件</b>给玩家 LUCK/HUNGER/SATURATION、给目标 GLOWING；仅当该刀<b>未激活</b>时，才按现有概率
 * 生成一支幻影剑（激活后交由“眼”实体输出，不再放剑）。副手专属分支已弃置，主副手统一走同一条路径。</p>
 * <p>激活表现（{@link #onServerTick}）：对“手持（主/副）携带且已激活炯恻”的玩家，每 {@code EYE_INTERVAL}
 * tick 选取目标并在其上方 {@code 2} 格生成一只 {@link EntityFateEye}——目标<b>优先为玩家重锋锁定单位且
 * 无视距离</b>，无锁定时才在半径 {@code EYE_RADIUS} 格球形范围内选敌对生物（激光 + 追踪剑攻击由眼实体负责）。</p>
 */
@Mod.EventBusSubscriber
public class Fate extends NRSpecialEffect {
    public Fate() {
        super(30, false, false);
    }

    static Random random = new Random();

    // ===== 眼生成轮询参数 =====
    /** 每位激活玩家的眼生成间隔（tick，100 = 5 秒）。 */
    private static final int EYE_INTERVAL = 100;
    /** 以玩家为球心、搜索敌对目标的半径（格）。 */
    private static final double EYE_RADIUS = 5.0;
    /** 眼生成在目标质点上方的抬高（格）。 */
    private static final double EYE_ABOVE = 2.0;

    /** 服务端每玩家的眼生成冷却计数（tick）。 */
    private static final Map<UUID, Integer> COOLDOWNS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        Entity userEntity = event.getUser();

        if (!(userEntity instanceof Player player)) {
            return;
        }
        ResourceLocation fate = NrSpecialEffectsRegistry.Fate.getId();
        // 命中所用的刀携带炯恻即可触发（主/副手统一，去掉主手限定）
        if (!state.hasSpecialEffect(fate)) {
            return;
        }

        // buff 无条件给予（不再受概率门）
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 10));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 10, 1));
        // 女仆/宠物保护：不对主人自己的驯服生物施加标记
        if (!MaidCompat.isOwnedBy(event.getTarget(), player)) {
            event.getTarget().addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 1));
        }

        // 未激活才按概率放剑；激活时输出改由眼实体负责
        if (NRSpecialEffect.isAquaActive(event.getBlade(), fate)) {
            return;
        }
        double decimal = random.nextDouble();
        if (decimal - 0.8d <= 0.0000001D) {
            Level worldIn = player.level();
            // 旧写法先 new 一把 EntityEnchantedSword 并 addFreshEntity，又调 ss.startRiding(player, true)
            // 把它“骑”在玩家身上，仅用作传递 speed/damage/color/delay 的占位实体——结果就是一只真正的
            // 剑实体挂在发射者身上、跟着他移动（与父类插附无关，故 getRayTrace/setHitEntity 等关卡管不到）。
            // 这些参数全部可以就地算出，因此不再创建任何多余实体，只留 spawnSwords 生成的唯一一把剑。
            double damage = state.getBaseAttackModifier() * 0.2F;
            int color = 16766720;
            int delay = 5;
            Vec3 offset = player.getEyePosition();
            EntityEnchantedSword.spawnSwords(player, worldIn, offset, EntityNRBlisteringSword.SpawnMode.RANDOM, 1,
                    true, 0, 0, 0, 5, damage, color, true, delay);
            player.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.2F, 1.45F);
        }
    }

    /**
     * 服务端每 tick：对“手持（主/副）携带且激活炯恻”的玩家维护冷却，冷却到点时在半径 {@code EYE_RADIUS}
     * 球内选取敌对生物（优先玩家锁定单位），在其上方生成一只 {@link EntityFateEye}。找不到目标则本周期跳过、
     * 不累加冷却（保持就绪）。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        ResourceLocation fate = NrSpecialEffectsRegistry.Fate.getId();
        List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            if (player.isSpectator()) {
                continue;
            }
            if (activeFateBlade(player, fate) == null) {
                COOLDOWNS.remove(player.getUUID());
                continue;
            }
            int cd = COOLDOWNS.getOrDefault(player.getUUID(), 0);
            if (cd > 0) {
                COOLDOWNS.put(player.getUUID(), cd - 1);
                continue;
            }
            Entity target = chooseEyeTarget(player, fate);
            if (target == null) {
                continue; // 无目标：本周期跳过，保持就绪
            }
            spawnEye(player, target);
            COOLDOWNS.put(player.getUUID(), EYE_INTERVAL);
        }
    }

    /** 玩家离线时移除其冷却记录，避免 Map 内存泄漏 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        COOLDOWNS.remove(event.getEntity().getUUID());
    }

    /** 服务端停机/退出世界时清空冷却记录 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    public static void clear() {
        COOLDOWNS.clear();
    }

    /** 取手持（主手优先、否则副手）携带且已激活炯恻的刀；无则返回 {@code null}（判定仿 {@code Porgatory}）。 */
    private static ItemStack activeFateBlade(ServerPlayer player, ResourceLocation fate) {
        ItemStack main = player.getMainHandItem();
        if (NRSpecialEffect.carriesSE(main, fate) && NRSpecialEffect.isAquaActive(main, fate)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (NRSpecialEffect.carriesSE(off, fate) && NRSpecialEffect.isAquaActive(off, fate)) {
            return off;
        }
        return null;
    }

    /**
     * 选取眼锁定的目标：优先玩家<b>重锋锁定</b>单位——真正的锁定目标存在激活炯恻的刀状态里
     * （{@link ISlashBladeState#getTargetEntity(Level)}，由 LockOnManager 写入；注意
     * {@code TargetSelector.getTargettableEntitiesWithinAABB} 只枚举近战射程内候选、不含锁定状态，
     * 故不能拿它当“已锁定”用），<b>无视距离</b>直接采用；无锁定单位时退化为球内任意可锁定
     * 敌对生物（{@link Enemy} 且 {@link TargetSelector#lockon}）。多段生物归并到本体。无候选返回 {@code null}。
     */
    private static Entity chooseEyeTarget(ServerPlayer player, ResourceLocation fate) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        double r2 = EYE_RADIUS * EYE_RADIUS;

        // 1) 真正的重锋锁定单位（在读刀状态里，不看距离）
        Entity locked = lockedTargetOfBlade(player, level, fate);
        if (locked != null) {
            return mergeToMain(locked);
        }

        // 2) 球内任意敌对生物
        Entity best = null;
        double min = Double.MAX_VALUE;
        AABB box = player.getBoundingBox().inflate(EYE_RADIUS);
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e instanceof Enemy && e.isAlive() && !e.isSpectator()
                        && eye.distanceToSqr(e.position()) <= r2
                        && TargetSelector.lockon.test(player, e));
        for (LivingEntity e : enemies) {
            double d = eye.distanceToSqr(e.position());
            if (d < min) {
                min = d;
                best = e;
            }
        }
        return best == null ? null : mergeToMain(best);
    }

    /** 从携带且激活炯恻的刀（主手优先、否则副手）的刀状态里读重锋锁定单位；无/失效返回 {@code null}。 */
    private static Entity lockedTargetOfBlade(ServerPlayer player, Level level, ResourceLocation fate) {
        ItemStack blade = null;
        ItemStack main = player.getMainHandItem();
        if (NRSpecialEffect.carriesSE(main, fate) && NRSpecialEffect.isAquaActive(main, fate)) {
            blade = main;
        } else {
            ItemStack off = player.getOffhandItem();
            if (NRSpecialEffect.carriesSE(off, fate) && NRSpecialEffect.isAquaActive(off, fate)) {
                blade = off;
            }
        }
        if (blade == null) {
            return null;
        }
        // 注意：LazyOptional.map() 不允许 mapper 返回 null（内部 Optional.of(null) 会 NPE），
        // 而无锁定/目标已卸载时 getTargetEntity 恰好返回 null；改用 resolve() 拿到真 Optional 后再 map。
        Entity target = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .resolve()
                .map(st -> st.getTargetEntity(level))
                .orElse(null);
        if (target == null || !target.isAlive() || target == player || target.isSpectator()) {
            return null;
        }
        // 存档重载后刀 NBT 里可能残留上次会话的 targetEntityId，可能命中无关实体：
        // 要求仍是可锁定的活体生物才采用
        if (!(target instanceof LivingEntity lt) || !TargetSelector.lockon.test(player, lt)
                // 女仆/宠物保护：炯恻之眼不锁定主人自己的驯服生物
                || MaidCompat.isOwnedBy(lt, player)) {
            return null;
        }
        return target;
    }

    /** 在目标质点上方 {@code EYE_ABOVE} 格生成一只眼，绑定释放者与目标。 */
    private static void spawnEye(ServerPlayer player, Entity target) {
        Level level = player.level();
        EntityFateEye eye = new EntityFateEye(NrEntitiesRegistry.Fate_Eye, level);
        Vec3 spawnPos = target.position().add(0, target.getBbHeight() + EYE_ABOVE, 0);
        eye.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        eye.setup(player, target);
        level.addFreshEntity(eye);
    }

    /** 把多段生物（末影龙等 {@link PartEntity}）归并到本体。 */
    private static Entity mergeToMain(Entity e) {
        if (e instanceof PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent != null) {
                return parent;
            }
        }
        return e;
    }
}
