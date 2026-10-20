package net.xianyu.prinegorerouse.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xianyu.prinegorerouse.config.NRClientConfig;

/**
 * 时间素 HUD 格子的位置编辑界面。
 * <p>玩家可用鼠标拖动格子到任意位置，右键复位为“自动”（贴右下角），关闭（Esc）时把坐标写回
 * 客户端配置（{@link NRClientConfig}）并落盘。渲染预览与 HUD 实际显示共用 {@link AquaHudLayout}
 * 的换算，保证所见即所得。</p>
 */
public class AquaHudEditScreen extends Screen {

    /** 格子边长，与 HUD 渲染保持一致。 */
    private static final int SIZE = AquaHudLayout.SIZE;
    /** 抓取容差：允许在格子及其外围若干像素内按下即可拖动。 */
    private static final int GRAB_PADDING = 6;

    /** 当前预览的格子左上角坐标（缩放像素）。 */
    private int slotX;
    private int slotY;
    /** 是否正在拖动，以及按下时鼠标相对格子左上角的偏移。 */
    private boolean dragging;
    private double grabX;
    private double grabY;
    /** 是否处于“自动”状态（用于提示文案与复位判定）。 */
    private boolean autoMode;

    public AquaHudEditScreen() {
        super(Component.translatable("screen.prinegorerouse.aqua_hud_edit"));
        this.slotX = AquaHudLayout.x(this.width);
        this.slotY = AquaHudLayout.y(this.height);
        this.autoMode = AquaHudLayout.isAuto();
    }

    @Override
    protected void init() {
        super.init();
        // 若构造时 width/height 尚未就绪，在 init 阶段再校准一次
        if (this.autoMode) {
            this.slotX = AquaHudLayout.x(this.width);
            this.slotY = AquaHudLayout.y(this.height);
        }
    }

    /** 编辑时不暂停游戏，方便观察实际 HUD 效果。 */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 半透明暗化背景（不渲染控件列表，避免按钮抢焦点）
        graphics.fill(0, 0, this.width, this.height, 0x90000000);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.prinegorerouse.aqua_hud_edit.tip"),
                this.width / 2, 34, 0xA0A0A0);

        // 预览：即便当前时间素为 0，也用一小段示例填充让玩家看清格子形态
        long amount = ClientAquaState.get();
        long preview = amount > 0L ? amount : 360000L;

        RenderSystem.disableDepthTest();
        // 悬停高亮外框
        if (isOverSlot(mouseX, mouseY)) {
            graphics.fill(slotX - 1, slotY - 1, slotX + SIZE + 1, slotY - 1, 0x80FFFFFF);
            graphics.fill(slotX - 1, slotY + SIZE, slotX + SIZE + 1, slotY + SIZE + 1, 0x80FFFFFF);
            graphics.fill(slotX - 1, slotY - 1, slotX, slotY + SIZE + 1, 0x80FFFFFF);
            graphics.fill(slotX + SIZE, slotY - 1, slotX + SIZE + 1, slotY + SIZE + 1, 0x80FFFFFF);
        }
        AquaGaugeRenderer.draw(graphics, slotX, slotY, SIZE, preview);
        RenderSystem.enableDepthTest();

        // 坐标提示：拖动时放在格子下方，靠底则放上方，避免超出屏幕
        String coordText = "x=" + slotX + " y=" + slotY + (autoMode ? "  (auto)" : "");
        int coordY = slotY > this.height / 2 ? slotY - 10 : slotY + SIZE + 2;
        graphics.drawString(this.font, Component.literal(coordText), slotX, coordY, 0x80FF80);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverSlot(mouseX, mouseY)) {
            dragging = true;
            grabX = mouseX - slotX;
            grabY = mouseY - slotY;
            autoMode = false;
            return true;
        }
        if (button == 1) {
            // 右键复位为“自动”
            resetAuto();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            slotX = clamp((int) Math.round(mouseX - grabX), 0, this.width - SIZE);
            slotY = clamp((int) Math.round(mouseY - grabY), 0, this.height - SIZE);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** 关闭界面时保存当前位置到客户端配置并落盘。 */
    @Override
    public void removed() {
        super.removed();
        if (autoMode) {
            NRClientConfig.AQUA_HUD_X.set(-1);
            NRClientConfig.AQUA_HUD_Y.set(-1);
        } else {
            NRClientConfig.AQUA_HUD_X.set(slotX);
            NRClientConfig.AQUA_HUD_Y.set(slotY);
        }
        NRClientConfig.save();
    }

    private void resetAuto() {
        autoMode = true;
        slotX = AquaHudLayout.autoX(this.width);
        slotY = AquaHudLayout.autoY(this.height);
    }

    private boolean isOverSlot(double mx, double my) {
        return mx >= slotX - GRAB_PADDING && mx < slotX + SIZE + GRAB_PADDING
                && my >= slotY - GRAB_PADDING && my < slotY + SIZE + GRAB_PADDING;
    }

    private static int clamp(int v, int lo, int hi) {
        if (hi < lo) {
            return lo;
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
