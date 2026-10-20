package net.xianyu.prinegorerouse.client;

import net.minecraft.client.gui.GuiGraphics;
import net.xianyu.prinegorerouse.config.NRConfig;

/**
 * 时间素格子的流体计量表绘制工具（纯静态颜色，不需要流体贴图资源）。
 * <p>满度按 {@code amount/(amount+半程点)} 渐近：半程点处恰为 50%，存储无限时逼近 100%。
 * 波形动画速度随存储量增大而变慢（“越存储显示速度越慢”）。</p>
 */
public final class AquaGaugeRenderer {

    /** 时间素流体颜色（半透明蓝）。 */
    private static final int FLUID_COLOR = 0xC038B0F0;
    /** 格子边框颜色。 */
    private static final int FRAME_COLOR = 0xFF6E6E6E;
    /** 空格子底色。 */
    private static final int BACKGROUND_COLOR = 0x60101620;

    private AquaGaugeRenderer() {
    }

    /**
     * 在 {@code (x, y)} 处绘制一个 {@code size x size} 的时间素格子。
     *
     * @param amount 当前时间素数量
     */
    public static void draw(GuiGraphics graphics, int x, int y, int size, long amount) {
        // 底色
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, BACKGROUND_COLOR);
        // 1px 边框（上下左右四条）
        graphics.fill(x, y, x + size, y + 1, FRAME_COLOR);
        graphics.fill(x, y + size - 1, x + size, y + size, FRAME_COLOR);
        graphics.fill(x, y, x + 1, y + size, FRAME_COLOR);
        graphics.fill(x + size - 1, y, x + size, y + size, FRAME_COLOR);

        if (amount <= 0L) {
            return;
        }

        long half = NRConfig.AQUA_HALF_POINT.get();
        if (half < 1L) {
            half = 1L;
        }
        double fraction = (double) amount / (double) (amount + half);
        int inner = size - 2;
        double fluid = fraction * inner;

        // 动画速度：存储越多越慢，最低不小于 0.15
        double speed = 1.6 / (1.0 + (double) amount / (double) half);
        if (speed < 0.15D) {
            speed = 0.15D;
        }
        double t = System.currentTimeMillis() / 1000.0 * speed;

        // 逐列绘制带正弦波面的流体
        for (int i = 0; i < inner; i++) {
            int colX = x + 1 + i;
            double wave = Math.sin(t * 2.0 + i * 0.35) * 0.8;
            int h = (int) Math.round(fluid + wave);
            if (h < 0) {
                h = 0;
            }
            if (h > inner) {
                h = inner;
            }
            int top = y + size - 1 - h;
            graphics.fill(colX, top, colX + 1, y + size - 1, FLUID_COLOR);
        }
    }

    /** 把长整型时间素格式化成便于阅读的短字符串。 */
    public static String format(long amount) {
        if (amount < 1000L) {
            return Long.toString(amount);
        } else if (amount < 1_000_000L) {
            return String.format("%.1fK", amount / 1000.0);
        } else if (amount < 1_000_000_000L) {
            return String.format("%.2fM", amount / 1_000_000.0);
        }
        return String.format("%.2fG", amount / 1_000_000_000.0);
    }
}
