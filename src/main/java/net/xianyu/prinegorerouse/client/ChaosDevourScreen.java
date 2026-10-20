package net.xianyu.prinegorerouse.client;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.xianyu.prinegorerouse.NetworkPacketHandler;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.ChaosDevourPacket;
import net.xianyu.prinegorerouse.specialeffect.ChaosHandler;
import net.xianyu.prinegorerouse.specialeffect.EmptyTrapPacket;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 卡俄斯吞噬界面：把背包中的其它拔刀剑列为可点按钮，点击即喂给当前主手的卡俄斯吞噬。
 * <p>已吞噬过的同类刀按钮置灰不可点。顶部展示累计吞噬时间素（翻倍显示）与当前阶段。
 * 真正吞噬与属性累加在服务端 {@link ChaosHandler} 完成，界面只发送槽位意图。</p>
 * <p>列表支持滚轮滚动；并在 {@link #tick()} 中检测背包/已吞集合变化后自动重建，使被吞噬的条目随之消失。</p>
 */
public class ChaosDevourScreen extends Screen {

    /** 与按钮一一对应的可吞噬槽位。 */
    private final List<Integer> slots = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();

    /** Empty 陷阱：槽位 -> 已被点击次数（重建列表时保留，达到阈值即触发惩罚）。 */
    private final Map<Integer, Integer> emptyClicks = new HashMap<>();
    /** 触发惩罚所需的累计点击次数。 */
    private static final int EMPTY_MAX = 5;

    private static final int ROW_WIDTH = 240;
    private static final int ROW_HEIGHT = 20;
    private static final int CONTENT_TOP = 58;
    private static final int BOTTOM_PAD = 12;

    private double scrollOffset = 0;
    private int maxScroll = 0;
    /** 上次构建列表时的状态签名；变化即重建。 */
    private int builtSignature = Integer.MIN_VALUE;

    public ChaosDevourScreen() {
        super(Component.translatable("screen.prinegorerouse.chaos.devour_title"));
    }

    private ItemStack held() {
        return this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getMainHandItem() : ItemStack.EMPTY;
    }

    @Override
    protected void init() {
        super.init();
        slots.clear();
        buttons.clear();

        ItemStack chaos = held();
        if (!NRSpecialEffect.isChaosBlade(chaos)) {
            this.builtSignature = Integer.MIN_VALUE;
            return;
        }
        Inventory inv = this.minecraft.player.getInventory();
        int startX = this.width / 2 - ROW_WIDTH / 2;
        int y = CONTENT_TOP;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) {
                continue;
            }
            if (NRSpecialEffect.isChaosBlade(stack)) {
                continue; // 卡俄斯本身不可被吞噬
            }
            // 带 Empty（虚无）的彩蛋刀：免疫吞噬，在界面中作为“这里明明什么都没有”的陷阱条目
            if (NRSpecialEffect.carriesSE(stack, NrSpecialEffectsRegistry.Empty.getId())) {
                if (ClientAquaState.isEmptyBanned()) {
                    continue; // 已触发陷阱后不再显示
                }
                slots.add(slot);
                EmptyTrapButton trap = new EmptyTrapButton(slot, startX, y, Component.literal(""));
                buttons.add(trap);
                this.addRenderableWidget(trap);
                y += ROW_HEIGHT;
                continue;
            }
            slots.add(slot);
            boolean devoured = ChaosHandler.isDevoured(chaos, stack);
            final int captured = slot;
            Button button = Button.builder(stack.getHoverName(), b ->
                    NetworkPacketHandler.INSTANCE.sendToServer(new ChaosDevourPacket(captured)))
                    .bounds(startX, y, ROW_WIDTH, 18).build();
            button.active = !devoured;
            buttons.add(button);
            this.addRenderableWidget(button);
            y += ROW_HEIGHT;
        }

        int contentBottom = CONTENT_TOP + slots.size() * ROW_HEIGHT;
        this.maxScroll = Math.max(0, contentBottom - (this.height - BOTTOM_PAD));
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
        this.builtSignature = signature();
    }

    @Override
    public void tick() {
        super.tick();
        // 背包或已吞集合变化时重建列表（被吞噬的条目因此消失 / 新同类条目出现）
        if (signature() != this.builtSignature) {
            this.rebuildWidgets();
        }
    }

    /** 轻量状态签名：主手物品 + 已吞数量 + 每个刀槽（槽号/物品/数量）。 */
    private int signature() {
        ItemStack chaos = held();
        if (!NRSpecialEffect.isChaosBlade(chaos)) {
            return Integer.MIN_VALUE;
        }
        int h = net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(chaos.getItem());
        h = 31 * h + ChaosHandler.devouredCount(chaos);
        Inventory inv = this.minecraft.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)
                    || NRSpecialEffect.isChaosBlade(stack)) {
                continue;
            }
            h = 31 * h + slot;
            h = 31 * h + net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem());
            h = 31 * h + stack.getCount();
        }
        return h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.maxScroll > 0) {
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset - amount * 18.0, this.maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);

        ItemStack chaos = held();
        if (!NRSpecialEffect.isChaosBlade(chaos)) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("screen.prinegorerouse.chaos.need_blade"),
                    this.width / 2, this.height / 2, 0xFF8080);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        long shown = ChaosHandler.devouredAqua(chaos) * 2L;
        int stage = ChaosHandler.stage(chaos);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.prinegorerouse.chaos.aqua", shown),
                this.width / 2, 30, 0x80FFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.prinegorerouse.chaos.stage", stage, NRConfig.chaosMaxStage()),
                this.width / 2, 42, 0xFFE0A0);

        if (slots.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("screen.prinegorerouse.chaos.devour_empty"),
                    this.width / 2, this.height / 2, 0xA0A0A0);
        }

        int off = (int) this.scrollOffset;
        int startX = this.width / 2 - ROW_WIDTH / 2;
        graphics.enableScissor(0, CONTENT_TOP - 2, this.width, this.height - BOTTOM_PAD);
        for (int i = 0; i < buttons.size(); i++) {
            this.buttons.get(i).setPosition(startX, CONTENT_TOP + i * ROW_HEIGHT + off * -1);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();

        // Empty 陷阱：悬停时提示“这里明明什么都没有”，随点击次数越来越红
        for (Button b : this.buttons) {
            if (b instanceof EmptyTrapButton trap && mouseOver(trap, mouseX, mouseY)) {
                int c = this.emptyClicks.getOrDefault(trap.slot, 0);
                Component hint = Component.translatable("screen.prinegorerouse.chaos.empty_hint")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(emptyColor(c))));
                graphics.renderTooltip(this.font, hint, mouseX, mouseY);
            }
        }

        if (this.maxScroll > 0) {
            int trackTop = CONTENT_TOP;
            int trackBottom = this.height - BOTTOM_PAD;
            int trackH = trackBottom - trackTop;
            int x = this.width - 8;
            graphics.fill(x, trackTop, x + 4, trackBottom, 0x40FFFFFF);
            int thumbH = Math.max(16, (int) ((double) trackH * trackH / (trackH + this.maxScroll)));
            int thumbTop = trackTop + (int) ((double) this.scrollOffset / this.maxScroll * (trackH - thumbH));
            graphics.fill(x, thumbTop, x + 4, thumbTop + thumbH, 0xC0FFFFFF);
        }
    }

    /** 点击 Empty 陷阱条目：累计次数，达到阈值则发送惩罚包、本地立即屏蔽并关闭界面。 */
    private void onEmptyClick(int slot) {
        int c = this.emptyClicks.merge(slot, 1, Integer::sum);
        if (c >= EMPTY_MAX) {
            NetworkPacketHandler.INSTANCE.sendToServer(new EmptyTrapPacket(slot));
            ClientAquaState.markEmptyBanned();
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }
    }

    private static boolean mouseOver(Button b, int mx, int my) {
        return mx >= b.getX() && mx < b.getX() + b.getWidth() && my >= b.getY() && my < b.getY() + b.getHeight();
    }

    /** 随点击次数由白渐红（clicks 从 0 到 EMPTY_MAX-1）。 */
    private static int emptyColor(int clicks) {
        double t = Math.min(1.0, (double) clicks / (double) (EMPTY_MAX - 1));
        int channel = (int) Math.round(255.0 * (1.0 - t));
        return (0xFF << 24) | (0xFF << 16) | (channel << 8) | channel;
    }

    /**
     * 带 Empty 的刀对应的“陷阱”按钮：条目本身不显示刀名（看起来“什么都没有”），
     * 悬停提示与点满逻辑由外部 {@link ChaosDevourScreen} 负责。
     */
    private final class EmptyTrapButton extends Button {
        final int slot;

        EmptyTrapButton(int slot, int x, int y, Component msg) {
            super(x, y, ROW_WIDTH, 18, msg, b -> onEmptyClick(slot), DEFAULT_NARRATION);
            this.slot = slot;
        }
    }
}
