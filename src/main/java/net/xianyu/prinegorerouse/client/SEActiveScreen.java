package net.xianyu.prinegorerouse.client;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.xianyu.prinegorerouse.NetworkPacketHandler;
import net.xianyu.prinegorerouse.config.AquaSEMode;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.specialeffect.ChaosHandler;
import net.xianyu.prinegorerouse.specialeffect.ChaosSAPacket;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;
import net.xianyu.prinegorerouse.specialeffect.SEAquaPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * 特效时间素激活界面。
 * <p>手持拔刀剑时打开：列出当前刀上属于本模组的所有特效，显示消耗模式与时间素成本、激活状态与“激活 / 解除”按钮。</p>
 * <p>当主手为卡俄斯时额外提供：CHAOS 本体锁定为“不可关闭”、获得的 SE 采用 CHAOS 公式费用、
 * SA 列表（点选装备 / 随机模式切换）与打开吞噬界面的入口。</p>
 * <p>内容可能超出屏幕，故支持鼠标滚轮滚动：所有内容按基准坐标布局，绘制与控件位置统一减去 {@link #scrollOffset}，
 * 并用 scissor 裁剪到可视区。</p>
 */
public class SEActiveScreen extends Screen {

    private final List<ResourceLocation> effects = new ArrayList<>();
    private final List<Button> seButtons = new ArrayList<>();
    private final List<ResourceLocation> saKeys = new ArrayList<>();
    private final List<Button> saButtons = new ArrayList<>();
    private final List<Button> saToggleButtons = new ArrayList<>();

    private Button devourButton;
    private Button randomButton;

    private boolean chaos = false;
    // 基准（未滚动）布局坐标
    private int seStartY;
    private int afterSEBase;
    private int saStartY;

    private double scrollOffset = 0;
    private int maxScroll = 0;

    private static final int CONTENT_TOP = 46;
    private static final int CONTENT_BOTTOM_PAD = 10;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_WIDTH = 240;

    public SEActiveScreen() {
        super(Component.translatable("screen.prinegorerouse.se_active"));
    }

    private ItemStack held() {
        return this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getMainHandItem() : ItemStack.EMPTY;
    }

    @Override
    protected void init() {
        super.init();
        effects.clear();
        seButtons.clear();
        saKeys.clear();
        saButtons.clear();
        saToggleButtons.clear();

        ItemStack blade = held();
        chaos = NRSpecialEffect.isChaosBlade(blade);
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            for (ResourceLocation se : state.getSpecialEffects()) {
                // 去重：历史上被重复吞噬写入的同一 SE 只显示一行
                if (prinegorerouse.MOD_ID.equals(se.getNamespace()) && !effects.contains(se)) {
                    effects.add(se);
                }
            }
        });

        int startX = this.width / 2 - ROW_WIDTH / 2;
        seStartY = CONTENT_TOP;
        for (int i = 0; i < effects.size(); i++) {
            ResourceLocation se = effects.get(i);
            boolean locked = (NRSpecialEffect.isChaosSE(se) && chaos) || NRSpecialEffect.isEmptySE(se);
            Button button = Button.builder(buttonLabel(se), b ->
                    NetworkPacketHandler.INSTANCE.sendToServer(new SEAquaPacket(se.toString(), !isActive(se))))
                    .bounds(startX + ROW_WIDTH - 74, seStartY + i * ROW_HEIGHT + 2, 72, 18).build();
            button.active = !locked;
            seButtons.add(button);
            this.addRenderableWidget(button);
        }

        afterSEBase = seStartY + effects.size() * ROW_HEIGHT + 8;
        int contentBottom = afterSEBase;

        if (chaos) {
            devourButton = Button.builder(
                    Component.translatable("screen.prinegorerouse.chaos.devour"),
                    b -> Minecraft.getInstance().setScreen(new ChaosDevourScreen()))
                    .bounds(startX, afterSEBase, 110, 18).build();
            this.addRenderableWidget(devourButton);

            randomButton = Button.builder(
                    Component.translatable(ChaosHandler.isRandomMode(blade)
                            ? "screen.prinegorerouse.chaos.random.on"
                            : "screen.prinegorerouse.chaos.random.off"),
                    b -> NetworkPacketHandler.INSTANCE.sendToServer(new ChaosSAPacket("toggle", "")))
                    .bounds(startX + 116, afterSEBase, 124, 18).build();
            this.addRenderableWidget(randomButton);

            saStartY = afterSEBase + 40;
            ListTag sa = ChaosHandler.saList(blade);
            for (int i = 0; i < sa.size(); i++) {
                ResourceLocation rl = ResourceLocation.tryParse(sa.getString(i));
                if (rl != null) {
                    saKeys.add(rl);
                }
            }
            for (int i = 0; i < saKeys.size(); i++) {
                ResourceLocation key = saKeys.get(i);
                Button equip = Button.builder(
                        Component.translatable("screen.prinegorerouse.chaos.equip"),
                        b -> NetworkPacketHandler.INSTANCE.sendToServer(new ChaosSAPacket("select", key.toString())))
                        .bounds(startX + ROW_WIDTH - 60, saStartY + i * 16 + 2, 58, 14).build();
                saButtons.add(equip);
                this.addRenderableWidget(equip);

                Button toggle = Button.builder(
                        Component.translatable("screen.prinegorerouse.chaos.sa_disable"),
                        b -> NetworkPacketHandler.INSTANCE.sendToServer(new ChaosSAPacket(
                                ChaosHandler.isSaDisabled(held(), key) ? "enable" : "disable", key.toString())))
                        .bounds(startX + ROW_WIDTH - 120, saStartY + i * 16 + 2, 56, 14).build();
                saToggleButtons.add(toggle);
                this.addRenderableWidget(toggle);
            }
            contentBottom = saStartY + saKeys.size() * 16 + 4;
        } else {
            saStartY = afterSEBase;
        }

        int viewportBottom = this.height - CONTENT_BOTTOM_PAD;
        this.maxScroll = Math.max(0, contentBottom - viewportBottom);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
    }

    private Component buttonLabel(ResourceLocation se) {
        if ((NRSpecialEffect.isChaosSE(se) && chaos) || NRSpecialEffect.isEmptySE(se)) {
            return Component.translatable("screen.prinegorerouse.chaos.locked");
        }
        return Component.translatable(isActive(se) ? "screen.prinegorerouse.se_active.deactivate"
                : "screen.prinegorerouse.se_active.activate");
    }

    /** 按单把刀判定：读取当前主手刀 NBT 的激活标志。 */
    private boolean isActive(ResourceLocation se) {
        return NRSpecialEffect.isAquaActive(held(), se);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.maxScroll > 0) {
            this.scrollOffset = clamp(this.scrollOffset - amount * 18.0, 0, this.maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);

        long pool = ClientAquaState.get();
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.prinegorerouse.se_active.pool", pool),
                this.width / 2, 30, 0x80FF80);

        if (effects.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("screen.prinegorerouse.se_active.empty"),
                    this.width / 2, this.height / 2, 0xA0A0A0);
        }

        int off = (int) this.scrollOffset;
        int startX = this.width / 2 - ROW_WIDTH / 2;

        // 裁剪内容区，使滚动的文字与控件不越界到头部
        graphics.enableScissor(0, CONTENT_TOP - 4, this.width, this.height - CONTENT_BOTTOM_PAD);

        for (int i = 0; i < effects.size(); i++) {
            ResourceLocation se = effects.get(i);
            int rowY = seStartY + i * ROW_HEIGHT + 6 - off;
            boolean active = isActive(se);

            graphics.drawString(this.font, displayName(se, active), startX, rowY, 0xFFFFFF);

            String detail;
            if (NRSpecialEffect.isEmptySE(se)) {
                detail = "永久激活";
            } else if (chaos && !NRSpecialEffect.isChaosSE(se)) {
                if (active) {
                    // 已激活：显示解除将返还的实际支付额，而非“解锁”费用
                    detail = "返还 " + NRSpecialEffect.paidOf(held(), se);
                } else {
                    long cost = ChaosHandler.chaosSECost(held(), se);
                    detail = "解锁 " + cost;
                }
            } else {
                AquaSEMode mode = NRConfig.seMode(se.toString());
                long cost = NRConfig.seCost(se.toString());
                detail = modeLabel(mode) + " " + cost;
            }
            graphics.drawString(this.font, Component.literal(detail), startX, rowY + 11,
                    active ? 0xB0FFB0 : 0x999999);

            Button b = seButtons.get(i);
            b.setMessage(buttonLabel(se));
            b.setPosition(startX + ROW_WIDTH - 74, seStartY + i * ROW_HEIGHT + 2 - off);
        }

        if (chaos) {
            this.devourButton.setPosition(startX, afterSEBase - off);
            this.randomButton.setPosition(startX + 116, afterSEBase - off);
            // 每帧按当前主手刀刷新随机 SA 按钮文案：服务端同步到达后自动纠正
            this.randomButton.setMessage(Component.translatable(ChaosHandler.isRandomMode(held())
                    ? "screen.prinegorerouse.chaos.random.on"
                    : "screen.prinegorerouse.chaos.random.off"));

            graphics.drawString(this.font, Component.translatable("screen.prinegorerouse.chaos.sa_title"),
                    startX, saStartY - 12 - off, 0xFFE0A0);
            ResourceLocation current = currentSA();
            for (int i = 0; i < saKeys.size(); i++) {
                ResourceLocation key = saKeys.get(i);
                boolean disabled = ChaosHandler.isSaDisabled(held(), key);
                boolean equipped = current != null && current.equals(key);
                Component label = Component.translatable("slash_art." + key.getNamespace() + "." + key.getPath());
                if (label.getString().equals("slash_art." + key.getNamespace() + "." + key.getPath())) {
                    // 无翻译时退回显示原始 id
                    label = Component.literal(key.toString());
                }
                if (disabled) {
                    label = label.copy().withStyle(ChatFormatting.STRIKETHROUGH);
                }
                int color = disabled ? 0x808080 : (equipped ? 0x70FF70 : 0xCCCCCC);
                graphics.drawString(this.font, label, startX, saStartY + i * 16 + 4 - off, color);

                Button equip = this.saButtons.get(i);
                equip.setPosition(startX + ROW_WIDTH - 60, saStartY + i * 16 + 2 - off);
                equip.active = !disabled; // 禁用的 SA 无法装备

                Button toggle = this.saToggleButtons.get(i);
                toggle.setPosition(startX + ROW_WIDTH - 120, saStartY + i * 16 + 2 - off);
                toggle.setMessage(Component.translatable(disabled
                        ? "screen.prinegorerouse.chaos.sa_enable"
                        : "screen.prinegorerouse.chaos.sa_disable"));
                // 当前装备的 SA 不允许禁用
                toggle.active = disabled || !equipped;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();

        // 滚动条
        if (this.maxScroll > 0) {
            int trackTop = CONTENT_TOP;
            int trackBottom = this.height - CONTENT_BOTTOM_PAD;
            int trackH = trackBottom - trackTop;
            int x = this.width - 8;
            graphics.fill(x, trackTop, x + 4, trackBottom, 0x40FFFFFF);
            int thumbH = Math.max(16, (int) ((double) trackH * trackH / (trackH + this.maxScroll)));
            int thumbTop = trackTop + (int) ((double) this.scrollOffset / this.maxScroll * (trackH - thumbH));
            graphics.fill(x, thumbTop, x + 4, thumbTop + thumbH, 0xC0FFFFFF);
        }
    }

    private ResourceLocation currentSA() {
        return held().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getSlashArtsKey).orElse(null);
    }

    private static String modeLabel(AquaSEMode mode) {
        switch (mode) {
            case PER_SECOND:
                return "/s";
            case PER_HITS:
                return "/hits";
            case PER_SA:
                return "/SA";
            case ONE_TIME:
            default:
                return "x1";
        }
    }

    /** 名称：激活显示 lang 颜色，未激活灰色（去掉 § 代码）。 */
    private static Component displayName(ResourceLocation se, boolean active) {
        Component desc = SpecialEffect.getDescription(se);
        if (active) {
            return desc;
        }
        return Component.literal(desc.getString().replaceAll("(?i)§.", ""))
                .withStyle(ChatFormatting.GRAY);
    }
}
