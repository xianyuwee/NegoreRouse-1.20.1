package net.xianyu.prinegorerouse.client;

import net.minecraft.client.Minecraft;
import net.xianyu.prinegorerouse.config.NRClientConfig;

/**
 * 时间素 HUD 格子的布局：把 CLIENT 配置换算成实际的左上角屏幕坐标。
 * <p>位置按<b>到屏幕右/下边缘的偏移</b>存（{@code hud_edge_x}/{@code hud_edge_y}），不再存绝对坐标：
 * 换分辨率、全屏/窗口互换、改 GUI 缩放时格子都跟着右下角走，玩家不必每次重调。{@code -1} 表示“自动”，
 * 即用内置的 {@value #AUTO_MARGIN} 像素边距。渲染与拖拽编辑界面共用这里的换算，保证所见即所得。</p>
 * <p>老版本的配置是绝对坐标 {@code hud_x}/{@code hud_y}，由 {@link #ensureMigrated()} 在每个存档进程里一次性
 * 换算成边缘偏移；其中 {@code 0/0} 是旧代码在 Screen 构造器里用 width=0 换算出来的污染值，会被归回“自动”，
 * 免得迁移完把格子贴死在左上角。</p>
 */
public final class AquaHudLayout {
    /** 格子边长（缩放像素）。 */
    public static final int SIZE = 18;
    /** 自动模式距离屏幕边缘的边距。 */
    private static final int AUTO_MARGIN = 4;

    /** 本进程内是否已经跑过迁移检查（避免每帧读配置 + 重复写盘）。 */
    private static boolean migrateChecked;

    private AquaHudLayout() {
    }

    /** 生效的右边缘偏移：配置为 -1（自动）时取内置边距。 */
    public static int edgeX() {
        int v = NRClientConfig.AQUA_HUD_EDGE_X.get();
        return v < 0 ? AUTO_MARGIN : v;
    }

    /** 生效的下边缘偏移。 */
    public static int edgeY() {
        int v = NRClientConfig.AQUA_HUD_EDGE_Y.get();
        return v < 0 ? AUTO_MARGIN : v;
    }

    /** 格子左上角的屏幕 X：由右边缘偏移反推，并夹进屏幕内。 */
    public static int x(int scaledWidth) {
        ensureMigrated();
        return clamp(scaledWidth - SIZE - edgeX(), 0, scaledWidth - SIZE);
    }

    /** 格子左上角的屏幕 Y：由下边缘偏移反推，并夹进屏幕内。 */
    public static int y(int scaledHeight) {
        ensureMigrated();
        return clamp(scaledHeight - SIZE - edgeY(), 0, scaledHeight - SIZE);
    }

    /** 左上角屏幕坐标 -> 右边缘偏移，供编辑界面写回配置（与 {@link #x(int)} 互逆）。 */
    public static int toEdgeX(int screenX, int scaledWidth) {
        return clamp(scaledWidth - SIZE - screenX, 0, scaledWidth);
    }

    /** 左上角屏幕坐标 -> 下边缘偏移。 */
    public static int toEdgeY(int screenY, int scaledHeight) {
        return clamp(scaledHeight - SIZE - screenY, 0, scaledHeight);
    }

    /** 自动模式（贴右下角）下的左上角坐标，供编辑界面复位预览使用（不读配置）。 */
    public static int autoX(int scaledWidth) {
        return scaledWidth - SIZE - AUTO_MARGIN;
    }

    public static int autoY(int scaledHeight) {
        return scaledHeight - SIZE - AUTO_MARGIN;
    }

    /**
     * 是否两轴都还在“自动”（未手动拖拽）状态。
     * <p>用 {@code &&} 而不是 {@code ||}：编辑界面关闭时若是 auto 就把两轴一起写回 -1，
     * 用或的话，只要有一轴是 -1 就会把另一轴刚迁移好的偏移也抹掉（迁移产物允许是混合态）。</p>
     */
    public static boolean isAuto() {
        ensureMigrated();
        return NRClientConfig.AQUA_HUD_EDGE_X.get() < 0 && NRClientConfig.AQUA_HUD_EDGE_Y.get() < 0;
    }

    /**
     * 一次性把遗留的绝对坐标迁移成边缘偏移。
     * <p>换算必须等窗口尺寸就绪：启动早期与 Screen 构造阶段 {@code getGuiScaledWidth()} 可能是 0，
     * 那时换算会得到垃圾值，所以直接跳过、下次再读布局时重试。真正做成一次靠 {@code hud_migrated}
     * 配置标记（写盘），{@link #migrateChecked} 只是本进程的节流。</p>
     */
    private static void ensureMigrated() {
        if (migrateChecked) {
            return;
        }
        // 配置还没从磁盘读进来时不能读写，否则会把默认值当成“旧值”迁移掉
        if (!NRClientConfig.CLIENT_CONFIG.isLoaded()) {
            return;
        }
        if (NRClientConfig.AQUA_HUD_MIGRATED.get()) {
            migrateChecked = true;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        if (sw <= SIZE || sh <= SIZE) {
            // 尺寸还没准备好，保持 migrateChecked = false，下一帧再试
            return;
        }
        migrateChecked = true;

        int legacyX = NRClientConfig.AQUA_HUD_X.get();
        int legacyY = NRClientConfig.AQUA_HUD_Y.get();
        if (legacyX == 0 && legacyY == 0) {
            // 旧版在 Screen 构造器里用 width=0 做 clamp(配置, 0, -18)，恒等于 0，又把 0 写回了配置。
            // 这不是玩家想要的位置，所以两轴同时为 0 时按“自动”处理。
            NRClientConfig.AQUA_HUD_EDGE_X.set(-1);
            NRClientConfig.AQUA_HUD_EDGE_Y.set(-1);
        } else {
            // 只能按“当前”窗口尺寸换算：绝对坐标本身没记录它是多大屏幕上调的
            if (legacyX >= 0) {
                NRClientConfig.AQUA_HUD_EDGE_X.set(toEdgeX(legacyX, sw));
            }
            if (legacyY >= 0) {
                NRClientConfig.AQUA_HUD_EDGE_Y.set(toEdgeY(legacyY, sh));
            }
        }
        NRClientConfig.AQUA_HUD_X.set(-1);
        NRClientConfig.AQUA_HUD_Y.set(-1);
        NRClientConfig.AQUA_HUD_MIGRATED.set(true);
        NRClientConfig.save();
    }

    private static int clamp(int v, int lo, int hi) {
        if (hi < lo) {
            return lo;
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
