package net.xianyu.prinegorerouse.aquatime;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.NetworkPacketHandler;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.specialeffect.ChaosHandler;
import net.xianyu.prinegorerouse.specialeffect.NRSEs.Back;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

import java.util.List;

/**
 * 时间素（AquaTime）数据读写工具。
 * <p>数值保存在玩家的 {@code getPersistentData()}（Forge 的 "ForgeData" 标签）里，
 * 会随 player.dat 自动持久化，无需自行写盘；但 ForgeData 不会自动同步到客户端，
 * 因此客户端展示所需的数值通过 {@link AquaSyncPacket} 主动下发。</p>
 */
public final class AquaTimeData {
    /** 持久化标签键名。 */
    public static final String KEY = "prinegorerouse_aquatime";
    /** 该玩家是否已触发 Empty 陷阱（此后吞噬界面不再显示带 Empty 的刀）；持久于玩家 ForgeData。 */
    public static final String EMPTY_BAN_KEY = "prinegorerouse_empty_banned";

    private AquaTimeData() {
    }

    /** 读取玩家当前时间素（服务端权威值）。 */
    public static long get(Player player) {
        return player.getPersistentData().getLong(KEY);
    }

    /** 直接设置时间素并同步到客户端（仅服务端调用）。 */
    public static void set(Player player, long value) {
        player.getPersistentData().putLong(KEY, value);
        sync(player);
    }

    /** 增加时间素并同步（仅服务端调用）。默认启用卡俄斯反向吞噬与回溯反转。 */
    public static void add(Player player, long delta) {
        add(player, delta, true, true);
    }

    /**
     * 增加时间素并同步（仅服务端调用）。
     *
     * @param chaosAware 为 true 且 delta&gt;0 时，若玩家主手持激活的卡俄斯则改为“反向吞噬”
     *                   （扣等量时间素并记录）；若时间素不足则仍按原样增长。扣费/返还等内部
     *                   增减应传 false，避免被立即吞掉或递归。
     */
    public static void add(Player player, long delta, boolean chaosAware) {
        add(player, delta, chaosAware, true);
    }

    /**
     * 增加时间素并同步（仅服务端调用）。
     *
     * @param chaosAware 见 {@link #add(Player, long, boolean)}。
     * @param backAware  为 true 且 delta&lt;0 时，若玩家手持已激活的“回溯”刀且有层数，这笔消耗会被
     *                   <b>反转为等量增加</b>（降一层、回复刀耐久），并覆盖卡俄斯的扣减（吞噬量仍累计）；
     *                   回溯自身的持续消耗应传 false，否则“消耗→增加”会自循环。
     */
    public static void add(Player player, long delta, boolean chaosAware, boolean backAware) {
        if (delta == 0) {
            return;
        }
        if (backAware && delta < 0 && player instanceof ServerPlayer sp && Back.consumeLayer(sp)) {
            // 回溯：消耗反转为等量增加；卡俄斯仍计入吞噬时间素，但不再扣玩家的时间素
            if (chaosAware) {
                ChaosHandler.countDevouredOnly(sp, -delta);
            }
            player.getPersistentData().putLong(KEY, get(player) - delta);
            sync(player);
            return;
        }
        if (chaosAware && delta > 0 && player instanceof ServerPlayer sp) {
            // 回溯覆盖卡俄斯的反向吞噬：主手持卡俄斯且有层数时，这笔“本会被吞掉”的增长
            // 反转为正常增加，卡俄斯吞噬量仍照常累计（阶段成长/随机增益不变），并降一层。
            if (backAware && NRSpecialEffect.isChaosBlade(sp.getMainHandItem()) && Back.consumeLayer(sp)) {
                ChaosHandler.countDevouredOnly(sp, delta);
                player.getPersistentData().putLong(KEY, get(player) + delta);
                sync(player);
                return;
            }
            if (ChaosHandler.eatAquaIfHolding(sp, delta)) {
                return;
            }
        }
        long v = get(player) + delta;
        if (v < 0) {
            v = 0;
        }
        player.getPersistentData().putLong(KEY, v);
        sync(player);
    }

    /** 向玩家客户端下发当前时间素与 Empty 禁显示标记；只对该玩家本人有意义。 */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer sp) {
            NetworkPacketHandler.sendToClientPlayer(new AquaSyncPacket(get(sp), isEmptyBanned(sp)), sp);
        }
    }

    /** 该玩家是否已因多次点击 Empty 陷阱而被永久屏蔽 Empty 刀（仅服务端读取持久数据）。 */
    public static boolean isEmptyBanned(Player player) {
        return player != null && player.getPersistentData().getBoolean(EMPTY_BAN_KEY);
    }

    /** 标记该玩家已触发 Empty 陷阱（仅服务端调用，后续 sync 会下发到客户端）。 */
    public static void setEmptyBanned(Player player) {
        player.getPersistentData().putBoolean(EMPTY_BAN_KEY, true);
    }

    /**
     * 统计“背包内的刀数量”：
     * <ul>
     *   <li>{@code inv.items}（快捷栏 0-8 + 主库存 9-35）——主手当前拿着的刀就是选中的快捷栏格，已包含在内；</li>
     *   <li>{@code inv.offhand}——副手。</li>
     * </ul>
     * 这样主手与副手的刀都会算入倍率。
     */
    public static int countBlades(Player player) {
        int count = 0;
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            if (isBlade(stack)) {
                count++;
            }
            // 卡俄斯吞噬掉的刀也算作“背包中存在”，触发额外叠加
            count += ChaosHandler.devouredCount(stack);
        }
        for (ItemStack stack : inv.offhand) {
            if (isBlade(stack)) {
                count++;
            }
            count += ChaosHandler.devouredCount(stack);
        }
        return count;
    }

    /** 是否为拔刀剑：优先按类判定，再用 BLADESTATE 能力兜底。 */
    private static boolean isBlade(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ItemSlashBlade) {
            return true;
        }
        return stack.getCapability(ItemSlashBlade.BLADESTATE).isPresent();
    }

    /** 白名单判定：物品注册 id 是否落在 config 的 whitelist 中。 */
    public static boolean isWhitelisted(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return false;
        }
        String id = key.toString();
        List<? extends String> whitelist = NRConfig.AQUA_WHITELIST.get();
        return whitelist != null && whitelist.contains(id);
    }

    /** 单个物品的转化价值（config 配置）。 */
    public static long valuePerItem() {
        return NRConfig.AQUA_PER_ITEM.get().longValue();
    }

    /** 系统总开关。 */
    public static boolean isEnabled() {
        return NRConfig.AQUA_ENABLE.get();
    }
}
