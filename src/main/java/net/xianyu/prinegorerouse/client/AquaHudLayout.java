package net.xianyu.prinegorerouse.client;

import net.xianyu.prinegorerouse.config.NRClientConfig;

/**
 * 时间素 HUD 格子的布局：把 CLIENT 配置里的 hud_x/hud_y 换算成实际的左上角屏幕坐标。
 * <p>-1 表示“自动”，贴右下角（留 4px 边距）；否则按绝对坐标并夹在屏幕范围内。
 * 渲染与拖拽编辑界面共用这里的换算，保证所见即所得。</p>
 */
public final class AquaHudLayout {
    /** 格子边长（缩放像素）。 */
    public static final int SIZE = 18;
    /** 自动模式距离屏幕边缘的边距。 */
    private static final int AUTO_MARGIN = 4;

    private AquaHudLayout() {
    }

    public static int x(int scaledWidth) {
        int v = NRClientConfig.AQUA_HUD_X.get();
        if (v < 0) {
            return scaledWidth - SIZE - AUTO_MARGIN;
        }
        return clamp(v, 0, scaledWidth - SIZE);
    }

    public static int y(int scaledHeight) {
        int v = NRClientConfig.AQUA_HUD_Y.get();
        if (v < 0) {
            return scaledHeight - SIZE - AUTO_MARGIN;
        }
        return clamp(v, 0, scaledHeight - SIZE);
    }

    /** 自动模式（贴右下角）下的左上角坐标，供编辑界面复位预览使用（不读配置）。 */
    public static int autoX(int scaledWidth) {
        return scaledWidth - SIZE - AUTO_MARGIN;
    }

    public static int autoY(int scaledHeight) {
        return scaledHeight - SIZE - AUTO_MARGIN;
    }

    /** 是否处于自动（未手动拖拽）状态。 */
    public static boolean isAuto() {
        return NRClientConfig.AQUA_HUD_X.get() < 0 || NRClientConfig.AQUA_HUD_Y.get() < 0;
    }

    private static int clamp(int v, int lo, int hi) {
        if (hi < lo) {
            return lo;
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
