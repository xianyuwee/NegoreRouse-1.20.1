package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.data.ChaosDamageSavedData;
import net.xianyu.prinegorerouse.data.builtin.NrBladeBuiltInRegistry;
import net.xianyu.prinegorerouse.item.ItemFEChaosBlade;
import net.xianyu.prinegorerouse.item.ModItems;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 卡俄斯“持有规则”事件处理，合并了原先的两个 FORGE 订阅类，统一为一个 {@link TickEvent.PlayerTickEvent} 监听器
 * （减少每 tick 的事件分发次数），并收敛重复的背包遍历样板。包含两块互相独立的职责：
 *
 * <p><b>一、自发转化</b>（原 ChaosConvertHandler）：玩家进入世界后<b>新获得</b>的前三把拔刀剑（任意 mod 的
 * {@link ItemSlashBlade}），分别按 {@link NRConfig#chaosChance(int)} 的概率（默认 30%/50%/70%）<b>替换为一把全新的卡俄斯</b>。</p>
 * <ul>
 *   <li>只计算一把：一旦转化成功即视为机会用尽（后续不再判定）。</li>
 *   <li>每个玩家单存档限一次：转化标志存于 {@code getPersistentData()}，随世界存档持久化，死亡经 Clone 保留。</li>
 *   <li>“前三把”机会每次进入世界重新计：进世界时背包已有的刀一律标记为“已见”，不计入新获得。</li>
 *   <li>创造模式不触发转化，仅把新刀标记为“已见”。</li>
 * </ul>
 *
 * <p><b>二、唯一持有</b>（原 ChaosStackLimitHandler）：每个玩家背包（含副手）内同时只能存在一把卡俄斯；
 * 合成、拿取、指令给予等任何让第二把卡俄斯进入背包的操作，多余那把会被<b>直接掉落</b>。</p>
 * <ul>
 *   <li>拾取拦截：{@link EntityItemPickupEvent} 中，若已持有卡俄斯则取消拾取，地上的卡俄斯留在原地。</li>
 *   <li>兜底扫描：服务端定时扫描背包，保留第一把卡俄斯、把其余的移除并掉落，覆盖所有非拾取来源。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChaosBladeHandler {

    // ===== 转化相关常量 =====

    /** 打在刀栈根 tag 上的“已判定”标记：使一把刀只会被机会窗口计入一次。 */
    private static final String CHECKED_TAG = "ChaosAcquiredChecked";
    /** 玩家持久化数据键：本存档是否已完成过一次转化（单存档限一次）。 */
    private static final String CONVERTED_KEY = "prinegorerouse_chaos_converted";

    private static final Random RANDOM = new Random();

    /** 每位玩家本次进入世界已消耗的判定次数（0..3），运行期内存态，进入/重生时重置、登出时清理。 */
    private static final Map<UUID, Integer> USED_THIS_SESSION = new ConcurrentHashMap<>();

    private ChaosBladeHandler() {
    }

    // ===== 转化：生命周期 =====

    /** 进入世界（登录）：重置本次机会窗口，并把当前背包已有的刀标记为已见（不算“新获得”）。 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        beginSession(event.getEntity());
    }

    /** 同存档内重生（死亡后）：同样重置窗口并标记已有刀。 */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        beginSession(event.getEntity());
    }

    /** 登出：清理内存窗口计数，避免 Map 泄漏。 */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        USED_THIS_SESSION.remove(event.getEntity().getUUID());
    }

    /** 死亡 Clone：把“本存档是否已转化”标志从旧玩家带到新玩家（ForgeData 不会自动拷贝）。 */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(CONVERTED_KEY)) {
            event.getEntity().getPersistentData().putBoolean(CONVERTED_KEY, true);
        }
    }

    private static void beginSession(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        USED_THIS_SESSION.put(player.getUUID(), 0);
        // 标记背包中已有的刀为已见，使其不占用“新获得前三把”的机会
        markAllBladesSeen(player.getInventory());
    }

    // ===== 统一 tick 扫描（唯一持有 + 自发转化各用各的节奏） =====

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) {
            return;
        }
        if (sp.isSpectator()) {
            return;
        }
        int t = sp.tickCount;
        // 唯一持有：每 5 tick 兜底扫描，兼顾“直接掉落”的即时感与开销
        if (t % 5 == 0) {
            enforceUnique(sp);
        }
        // 自发转化：每 10 tick 一次即可，降低开销
        if (t % 10 == 0 && NRConfig.chaosConvertEnabled() && !isConverted(sp)) {
            scanConvert(sp);
        }
    }

    // ===== 唯一持有 =====

    /** 已持有一把卡俄斯时，阻止再从地上捡起第二把（被取消的掉落物留在原地，即“直接掉落”效果）。 */
    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (NRSpecialEffect.isChaosBlade(event.getItem().getItem()) && hasChaos(sp.getInventory())) {
            event.setCanceled(true);
        }
    }

    /** 保留背包中第一把卡俄斯，把其余的移除并掉落。 */
    private static void enforceUnique(ServerPlayer sp) {
        Inventory inv = sp.getInventory();
        boolean kept = false;
        List<ItemStack> extras = null; // 惰性分配：绝大多数玩家 0/1 把卡俄斯，无需建列表

        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (!NRSpecialEffect.isChaosBlade(stack)) {
                continue;
            }
            if (kept) {
                if (extras == null) {
                    extras = new ArrayList<>(2);
                }
                extras.add(stack.copy());
                inv.items.set(i, ItemStack.EMPTY);
            } else {
                kept = true;
            }
        }
        for (int i = 0; i < inv.offhand.size(); i++) {
            ItemStack stack = inv.offhand.get(i);
            if (!NRSpecialEffect.isChaosBlade(stack)) {
                continue;
            }
            if (kept) {
                if (extras == null) {
                    extras = new ArrayList<>(2);
                }
                extras.add(stack.copy());
                inv.offhand.set(i, ItemStack.EMPTY);
            } else {
                kept = true;
            }
        }

        if (extras == null) {
            return;
        }
        for (ItemStack extra : extras) {
            sp.drop(extra, false);
        }
        // 同步背包到客户端与打开中的容器界面
        sp.inventoryMenu.broadcastChanges();
    }

    private static boolean hasChaos(Inventory inv) {
        for (int i = 0; i < inv.items.size(); i++) {
            if (NRSpecialEffect.isChaosBlade(inv.items.get(i))) {
                return true;
            }
        }
        for (int i = 0; i < inv.offhand.size(); i++) {
            if (NRSpecialEffect.isChaosBlade(inv.offhand.get(i))) {
                return true;
            }
        }
        return false;
    }

    // ===== 自发转化：扫描 =====

    private static void scanConvert(ServerPlayer player) {
        Inventory inv = player.getInventory();
        // 创造模式不触发转化：仅把背包中的新刀标记为已见，避免其占用切换难度后的机会窗口
        if (player.getAbilities().instabuild) {
            markAllBladesSeen(inv);
            return;
        }
        UUID uuid = player.getUUID();
        int used = USED_THIS_SESSION.getOrDefault(uuid, 0);

        // 主背包 0-35
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (!isFreshBlade(stack)) {
                continue;
            }
            stack.getOrCreateTag().putBoolean(CHECKED_TAG, true);
            if (used < 3 && roll(++used)) {
                USED_THIS_SESSION.put(uuid, used);
                inv.items.set(i, chaosStack(player));
                succeed(player);
                return;
            }
        }
        // 副手
        for (int i = 0; i < inv.offhand.size(); i++) {
            ItemStack stack = inv.offhand.get(i);
            if (!isFreshBlade(stack)) {
                continue;
            }
            stack.getOrCreateTag().putBoolean(CHECKED_TAG, true);
            if (used < 3 && roll(++used)) {
                USED_THIS_SESSION.put(uuid, used);
                inv.offhand.set(i, chaosStack(player));
                succeed(player);
                return;
            }
        }
        USED_THIS_SESSION.put(uuid, used);
    }

    // ===== 转化：工具 =====

    /** 把背包（含副手）中所有拔刀剑标记为“已见”。 */
    private static void markAllBladesSeen(Inventory inv) {
        for (int i = 0; i < inv.items.size(); i++) {
            markIfBlade(inv.items.get(i));
        }
        for (int i = 0; i < inv.offhand.size(); i++) {
            markIfBlade(inv.offhand.get(i));
        }
    }

    /** 是否为一把“本模组尚未判定过、且非卡俄斯”的拔刀剑。 */
    private static boolean isFreshBlade(ItemStack stack) {
        return stack.getItem() instanceof ItemSlashBlade
                && !NRSpecialEffect.isChaosBlade(stack)
                && !stack.getOrCreateTag().getBoolean(CHECKED_TAG);
    }

    /** 第 attempt（1..3）把的转化判定。 */
    private static boolean roll(int attempt) {
        int chance = NRConfig.chaosChance(attempt);
        return chance > 0 && RANDOM.nextInt(100) < chance;
    }

    private static void markIfBlade(ItemStack stack) {
        if (stack.getItem() instanceof ItemSlashBlade) {
            stack.getOrCreateTag().putBoolean(CHECKED_TAG, true);
        }
    }

    /** 通过卡俄斯的 SlashBladeDefinition 生成一把“完整”的卡俄斯（模型/耐久/SE/附魔齐全），
     *  并把面板攻击力烘焙为本存档“生存转化”专属的 (1,10) 随机值（跟随存档、不重随）。 */
    private static ItemStack chaosStack(ServerPlayer player) {
        ItemStack blade = null;
        var registry = SlashBlade.getSlashBladeDefinitionRegistry(player.serverLevel());
        if (registry.containsKey(NrBladeBuiltInRegistry.CHAOS_BLADE.location())) {
            SlashBladeDefinition def = registry.get(NrBladeBuiltInRegistry.CHAOS_BLADE);
            if (def != null) {
                blade = def.getBlade();
            }
        }
        // 兜底：定义不可用时退回原始物品（至少保证物品存在）
        if (blade == null || blade.isEmpty()) {
            blade = new ItemStack(ModItems.CHAOS_BLADE.get());
        }
        // 烘焙生存转化的随机攻击力 (1,10)，两位小数，跟随存档固定
        ItemFEChaosBlade.bakeBaseAttack(blade, ChaosDamageSavedData.get(player.serverLevel()).converted());
        return blade;
    }

    private static boolean isConverted(Player player) {
        return player.getPersistentData().getBoolean(CONVERTED_KEY);
    }

    private static void succeed(ServerPlayer player) {
        player.getPersistentData().putBoolean(CONVERTED_KEY, true);
        USED_THIS_SESSION.remove(player.getUUID());
        // 反馈：粒子 + 音效 + 提示
        player.serverLevel().sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0D, player.getZ(), 32,
                0.5D, 0.5D, 0.5D, 0.5D);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.0F);
        player.displayClientMessage(Component.translatable("se.prinegorerouse.chaos.converted"), false);
    }
}
