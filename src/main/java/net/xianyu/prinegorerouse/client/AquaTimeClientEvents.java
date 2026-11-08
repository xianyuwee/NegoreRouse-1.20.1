package net.xianyu.prinegorerouse.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.NetworkPacketHandler;
import net.xianyu.prinegorerouse.aquatime.AquaAbsorbPacket;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.prinegorerouse;

/**
 * 时间素格子的客户端表现：背包界面内的虚拟格子、HUD 常驻格子、以及点击存物交互。
 * 全部只在客户端运行。
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AquaTimeClientEvents {

    /**
     * 虚拟格子位置：不再写死坐标，改读客户端配置（可由 {@link AquaHudEditScreen} 拖拽调）。
     * 具体换算与默认值见 {@link AquaInvLayout}：旧默认值 (152,54) 落在面板右缘的 buff 图标列上，
     * 现在默认取合成区正下方的空带 (134,62)。
     */
    private static final int SLOT_SIZE = AquaInvLayout.SIZE;

    private AquaTimeClientEvents() {
    }

    // 仅对生存背包（屏幕类恰好为 InventoryScreen）生效；创造背包是 CreativeInventoryScreen，自然被排除。
    private static boolean isSurvivalInventory(ScreenEvent event) {
        return event.getScreen().getClass() == InventoryScreen.class;
    }

    /**
     * 计算背包虚拟格子在屏幕上的绝对坐标。
     * 直接取 {@link AbstractContainerScreen#getGuiLeft()} / {@link AbstractContainerScreen#getGuiTop()}（原版真实 leftPos/topPos），
     * 这样打开配方书导致 GUI 左移时，格子会跟随 GUI 一起移动，不会与副手槽等错位重叠。
     */
    private static int slotScreenX(ScreenEvent event) {
        return ((AbstractContainerScreen<?>) event.getScreen()).getGuiLeft() + AquaInvLayout.x();
    }

    private static int slotScreenY(ScreenEvent event) {
        return ((AbstractContainerScreen<?>) event.getScreen()).getGuiTop() + AquaInvLayout.y();
    }

    /**
     * 数值文字的纵向位置：格子靠近面板顶部（或被拖到面板外上方）时放到格子下方，
     * 否则会顶出屏幕/GUI 上缘。与 HUD 那边的处理保持同一思路。
     */
    private static int textY(int guiTop, int slotTop) {
        return slotTop - guiTop < 12 ? slotTop + SLOT_SIZE + 2 : slotTop - 10;
    }

    /** 背包界面：绘制虚拟格子与数值。 */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!AquaTimeData.isEnabled() || !isSurvivalInventory(event)) {
            return;
        }
        int x = slotScreenX(event);
        int y = slotScreenY(event);
        long amount = ClientAquaState.get();
        GuiGraphics graphics = event.getGuiGraphics();
        AquaGaugeRenderer.draw(graphics, x, y, SLOT_SIZE, amount);
        graphics.drawString(Minecraft.getInstance().font,
                Component.literal(AquaGaugeRenderer.format(amount)), x, textY(
                        ((AbstractContainerScreen<?>) event.getScreen()).getGuiTop(), y), ChatFormatting.AQUA.getColor());
    }

    /** 背包界面：左键点击虚拟格子 -> 发送存物请求。 */
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!AquaTimeData.isEnabled() || !isSurvivalInventory(event) || event.getButton() != 0) {
            return;
        }
        int x = slotScreenX(event);
        int y = slotScreenY(event);
        double mx = event.getMouseX();
        double my = event.getMouseY();
        if (mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE) {
            NetworkPacketHandler.INSTANCE.sendToServer(new AquaAbsorbPacket());
            event.setCanceled(true);
        }
    }

    /** HUD：按客户端配置的位置常驻显示时间素格子（背包界面打开时不重复显示）。 */
    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!AquaTimeData.isEnabled() || !NRConfig.AQUA_SHOW_HUD.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) {
            return;
        }
        long amount = ClientAquaState.get();
        if (amount <= 0L) {
            return;
        }

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = AquaHudLayout.x(sw);
        int y = AquaHudLayout.y(sh);
        // 位于屏幕下半部分时把数值文字放在格子上方，避免超出屏幕
        int textY = y > sh / 2 ? y - 10 : y + AquaHudLayout.SIZE + 1;
        RenderSystem.disableDepthTest();
        GuiGraphics graphics = event.getGuiGraphics();
        AquaGaugeRenderer.draw(graphics, x, y, AquaHudLayout.SIZE, amount);
        graphics.drawString(mc.font, Component.literal(AquaGaugeRenderer.format(amount)),
                x, textY, ChatFormatting.AQUA.getColor());
        RenderSystem.enableDepthTest();
    }

    /** 退出世界 / 断线时清零客户端缓存。 */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAquaState.reset();
    }
}
