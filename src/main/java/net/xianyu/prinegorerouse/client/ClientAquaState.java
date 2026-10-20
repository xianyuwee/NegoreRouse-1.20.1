package net.xianyu.prinegorerouse.client;

/**
 * 客户端本地缓存的时间素数量，由 {@code AquaSyncPacket} 写入，仅供 HUD / 背包格子渲染读取。
 */
public final class ClientAquaState {
    private static long amount = 0L;
    /** 该玩家是否已触发 Empty 陷阱（此后吞噬界面不再显示带 Empty 的刀）。由 {@code AquaSyncPacket} 同步。 */
    private static boolean emptyBanned = false;

    private ClientAquaState() {
    }

    public static void set(long value) {
        amount = value;
    }

    public static long get() {
        return amount;
    }

    /** 一次同步写入时间素与 Empty 禁显示标记。 */
    public static void set(long value, boolean banned) {
        amount = value;
        emptyBanned = banned;
    }

    public static boolean isEmptyBanned() {
        return emptyBanned;
    }

    /** 本地即时置位（发送惩罚包后马上生效，避免等待服务端回包）。 */
    public static void markEmptyBanned() {
        emptyBanned = true;
    }

    /** 断开连接 / 换世界时清零，避免残留上个存档的显示值。 */
    public static void reset() {
        amount = 0L;
        emptyBanned = false;
    }
}
