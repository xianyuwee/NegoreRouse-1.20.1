package net.xianyu.prinegorerouse.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.xianyu.prinegorerouse.config.NRClientConfig;

/**
 * 时间素格子的位置编辑界面：一次编辑两个格子。
 * <ul>
 *   <li><b>HUD 格子</b>——拖拽时用的是屏幕绝对坐标（鼠标位移直接可算），但写回配置时会被 {@link AquaHudLayout}
 *       换算成“距屏幕右/下边缘的偏移”，所以换分辨率、全屏/窗口互换后仍贴着右下角；在空白背景上拖动，
 *       右键复位为“自动”（贴右下角）。</li>
 *   <li><b>背包虚拟格子</b>——坐标是相对背包 GUI 左上角的偏移（{@link AquaInvLayout}），
 *       因此画面中央画了一块原版的 176×166 背包面板作为参照，格子只能在“面板 + 外侧余量”范围内拖，
 *       可以拖到面板外侧，但不会拖到看不见的位置；右键复位为内置默认位置。</li>
 * </ul>
 * <p>鼠标按住哪个格子就拖哪个（背包格子优先判定，因为它叠在面板上）；都不在光标下时右键复位两者。
 * 关闭（Esc）时把两组坐标写回客户端配置（{@link NRClientConfig}）并落盘。渲染预览与游戏内实际显示
 * 共用同一套换算，保证所见即所得。</p>
 */
public class AquaHudEditScreen extends Screen {

    /** 格子边长，与 HUD / 背包渲染保持一致。 */
    private static final int SIZE = AquaHudLayout.SIZE;
    /** 抓取容差：允许在格子及其外围若干像素内按下即可拖动。 */
    private static final int GRAB_PADDING = 6;
    /** 面板上方留给标题与提示文案的高度，避免叠在一起。 */
    private static final int PANEL_TOP_PAD = 34;
    /** 背包面板贴图（与原版生存背包同一张）。 */
    private static final ResourceLocation INVENTORY_TEXTURE =
            new ResourceLocation("textures/gui/container/inventory.png");

    // ===== HUD 格子（拖拽/渲染用屏幕绝对坐标，落盘时换算成右/下边缘偏移） =====
    private int hudX;
    private int hudY;
    private boolean hudDragging;
    private double hudGrabX;
    private double hudGrabY;
    private boolean hudAuto;

    // ===== 背包虚拟格子（相对 GUI 左上角的偏移） =====
    private int invX;
    private int invY;
    private boolean invDragging;
    private double invGrabX;
    private double invGrabY;
    private boolean invAuto;

    /**
     * 是否已经按有效的窗口尺寸校准过初始位置。
     * 必须在 {@link #init()} 里做而不是在构造器里：构造阶段 {@code width/height} 还是 0，
     * 拿它去换算会把已保存的坐标夹成 (0,0)，再写回配置就把位置永久锁死在左上角。
     * 只校准一次，避免拖拽途中改窗口大小（也会触发 init）把未保存的位置抹掉。
     */
    private boolean calibrated;

    public AquaHudEditScreen() {
        super(Component.translatable("screen.prinegorerouse.aqua_hud_edit"));
        // 这里只取不依赖窗口尺寸的开关；坐标留到 init() 再算
        this.hudAuto = AquaHudLayout.isAuto();
        this.invAuto = AquaInvLayout.isAuto();
    }

    @Override
    protected void init() {
        super.init();
        // width/height 到这一步才有效
        if (!this.calibrated) {
            this.calibrated = true;
            this.hudX = AquaHudLayout.x(this.width);
            this.hudY = AquaHudLayout.y(this.height);
            this.invX = AquaInvLayout.x();
            this.invY = AquaInvLayout.y();
        }
        // 窗口尺寸变化后把已有位置拉回可视范围（不重读配置，保留未保存的拖拽结果）
        this.hudX = clamp(this.hudX, 0, this.width - SIZE);
        this.hudY = clamp(this.hudY, 0, this.height - SIZE);
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

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.prinegorerouse.aqua_hud_edit.tip"),
                this.width / 2, 20, 0xA0A0A0);

        int panelLeft = panelLeft();
        int panelTop = panelTop();

        // 预览：即便当前时间素为 0，也用一小段示例填充让玩家看清格子形态
        long amount = ClientAquaState.get();
        long preview = amount > 0L ? amount : 360000L;

        // 背包面板参照物：直接 blit 原版 GUI，槽位位置与游戏内完全一致
        graphics.blit(INVENTORY_TEXTURE, panelLeft, panelTop, 0, 0,
                AquaInvLayout.PANEL_W, AquaInvLayout.PANEL_H);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.prinegorerouse.aqua_hud_edit.panel"),
                panelLeft + AquaInvLayout.PANEL_W / 2, panelTop + AquaInvLayout.PANEL_H + 2, 0x808080);

        RenderSystem.disableDepthTest();
        // HUD 格子：画在背景上
        if (isOverHudSlot(mouseX, mouseY)) {
            drawGrabFrame(graphics, hudX, hudY);
        }
        AquaGaugeRenderer.draw(graphics, hudX, hudY, SIZE, preview);
        // 坐标按新的语义显示“距右/下边缘”的偏移，跟 toml 里的 hud_edge_x/y 一致
        drawLabel(graphics, "screen.prinegorerouse.aqua_hud_edit.hud",
                hudX, hudY, hudAuto,
                AquaHudLayout.toEdgeX(hudX, this.width) + " / " + AquaHudLayout.toEdgeY(hudY, this.height));

        // 背包格子：叠在面板上（后画，保证看得见）
        int isx = panelLeft + invX;
        int isy = panelTop + invY;
        if (isOverInvSlot(mouseX, mouseY)) {
            drawGrabFrame(graphics, isx, isy);
        }
        AquaGaugeRenderer.draw(graphics, isx, isy, SIZE, preview);
        drawLabel(graphics, "screen.prinegorerouse.aqua_hud_edit.inv",
                isx, isy, invAuto, invX + " / " + invY);
        RenderSystem.enableDepthTest();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 背包格子先判：它叠在面板上，与 HUD 格子重叠时以它为准
            if (isOverInvSlot(mouseX, mouseY)) {
                invDragging = true;
                invGrabX = mouseX - (panelLeft() + invX);
                invGrabY = mouseY - (panelTop() + invY);
                invAuto = false;
                return true;
            }
            if (isOverHudSlot(mouseX, mouseY)) {
                hudDragging = true;
                hudGrabX = mouseX - hudX;
                hudGrabY = mouseY - hudY;
                hudAuto = false;
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 1) {
            // 右键复位光标下的那个格子；都没对准则两者一起复位
            if (isOverInvSlot(mouseX, mouseY)) {
                resetInvAuto();
            } else if (isOverHudSlot(mouseX, mouseY)) {
                resetHudAuto();
            } else {
                resetInvAuto();
                resetHudAuto();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && invDragging) {
            // 相对面板左上角，允许拖出面板外侧，但不超出“面板 + 余量”
            invX = AquaInvLayout.clampX((int) Math.round(mouseX - invGrabX - panelLeft()));
            invY = AquaInvLayout.clampY((int) Math.round(mouseY - invGrabY - panelTop()));
            return true;
        }
        if (button == 0 && hudDragging) {
            hudX = clamp((int) Math.round(mouseX - hudGrabX), 0, this.width - SIZE);
            hudY = clamp((int) Math.round(mouseY - hudGrabY), 0, this.height - SIZE);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            invDragging = false;
            hudDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** 关闭界面时把两个格子的位置写入客户端配置并落盘。 */
    @Override
    public void removed() {
        super.removed();
        // HUD 存“距右/下边缘”的偏移，而不是绝对坐标，这样换分辨率/全屏互换时位置跟着角落走
        NRClientConfig.AQUA_HUD_EDGE_X.set(hudAuto ? -1 : AquaHudLayout.toEdgeX(hudX, this.width));
        NRClientConfig.AQUA_HUD_EDGE_Y.set(hudAuto ? -1 : AquaHudLayout.toEdgeY(hudY, this.height));
        // 背包格子的偏移允许为负，所以“是否用默认位置”用独立布尔记录，坐标本身始终写当前值
        NRClientConfig.AQUA_INV_AUTO.set(invAuto);
        NRClientConfig.AQUA_INV_X.set(invX);
        NRClientConfig.AQUA_INV_Y.set(invY);
        NRClientConfig.save();
    }

    /** 面板左上角的屏幕坐标：水平居中，垂直居中但给标题/提示留出空间。 */
    private int panelLeft() {
        return this.width / 2 - AquaInvLayout.PANEL_W / 2;
    }

    private int panelTop() {
        return Math.max(PANEL_TOP_PAD, this.height / 2 - AquaInvLayout.PANEL_H / 2);
    }

    private void resetHudAuto() {
        hudAuto = true;
        hudX = AquaHudLayout.autoX(this.width);
        hudY = AquaHudLayout.autoY(this.height);
    }

    private void resetInvAuto() {
        invAuto = true;
        invX = AquaInvLayout.DEFAULT_X;
        invY = AquaInvLayout.DEFAULT_Y;
    }

    /** 悬停高亮外框。 */
    private void drawGrabFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + SIZE + 1, y, 0x80FFFFFF);
        graphics.fill(x - 1, y + SIZE, x + SIZE + 1, y + SIZE + 1, 0x80FFFFFF);
        graphics.fill(x - 1, y - 1, x, y + SIZE + 1, 0x80FFFFFF);
        graphics.fill(x + SIZE, y - 1, x + SIZE + 1, y + SIZE + 1, 0x80FFFFFF);
    }

    /** 格子旁的名称 + 坐标提示；靠屏幕下缘时放到格子上方，避免超出屏幕。 */
    private void drawLabel(GuiGraphics graphics, String key, int x, int y, boolean auto, String coord) {
        Component text = Component.translatable(key)
                .append(" " + coord + (auto ? "  (auto)" : ""));
        int labelY = y > this.height - 12 ? y - 10 : y + SIZE + 2;
        graphics.drawString(this.font, text, x, labelY, 0x80FF80, false);
    }

    private boolean isOverHudSlot(double mx, double my) {
        return mx >= hudX - GRAB_PADDING && mx < hudX + SIZE + GRAB_PADDING
                && my >= hudY - GRAB_PADDING && my < hudY + SIZE + GRAB_PADDING;
    }

    private boolean isOverInvSlot(double mx, double my) {
        double x = panelLeft() + invX;
        double y = panelTop() + invY;
        return mx >= x - GRAB_PADDING && mx < x + SIZE + GRAB_PADDING
                && my >= y - GRAB_PADDING && my < y + SIZE + GRAB_PADDING;
    }

    private static int clamp(int v, int lo, int hi) {
        if (hi < lo) {
            return lo;
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
