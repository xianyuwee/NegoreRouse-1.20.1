package net.xianyu.prinegorerouse.client.renderer.bar;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.item.BladeEnergyHelper;
import net.xianyu.prinegorerouse.prinegorerouse;

@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, value = Dist.CLIENT)
public class BladeEnergyBarRenderer {
    private static final int BAR_WIDTH = 14;
    private static final int BAR_HEIGHT = 2;
    private static final int BAR_X_OFFSET = 1;
    private static final int BAR_Y_OFFSET = 14; // 物品图标底部偏移
    private static final int BAR_BG_COLOR = 0xCC404040;
    private static final int BAR_FILL_COLOR = 0xCC00FF00;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();

        if (mc.screen == null) {
            // 无GUI：渲染快捷栏
            renderHotbar(mc, guiGraphics);
        } else if (mc.screen instanceof AbstractContainerScreen<?> screen) {
            // 有容器界面：渲染所有槽位
            renderContainerSlots(screen, guiGraphics);
        }
    }

    private static void renderHotbar(Minecraft mc, GuiGraphics guiGraphics) {
        Player player = mc.player;
        if (player == null) return;

        // 获取快捷栏槽位（0-8）
        int hotbarSlotCount = 9;
        int windowWidth = mc.getWindow().getGuiScaledWidth();
        int windowHeight = mc.getWindow().getGuiScaledHeight();

        // 快捷栏起始位置（类似原版，通常为 (windowWidth/2 - 91, windowHeight - 22)）
        int hotbarX = windowWidth / 2 - 91;
        int hotbarY = windowHeight - 22;

        for (int i = 0; i < hotbarSlotCount; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!BladeEnergyHelper.isAritemisSBlade(stack)) continue;

            int slotX = hotbarX + i * 20; // 每个槽位间隔20像素
            int slotY = hotbarY;
            int current = BladeEnergyHelper.getCurrentEnergy(stack);
            int max = BladeEnergyHelper.getMaxEnergy(stack);
            if (max <= 0) continue;

            // 绘制能量条背景和填充
            drawEnergyBar(guiGraphics, slotX, slotY, current, max);
            drawEnergyNumber(guiGraphics, slotX, slotY, current, max);
        }
    }

    private static void renderContainerSlots(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics) {
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (!BladeEnergyHelper.isAritemisSBlade(stack)) continue;

            int slotX = screen.getGuiLeft() + slot.x;
            int slotY = screen.getGuiTop() + slot.y;
            int current = BladeEnergyHelper.getCurrentEnergy(stack);
            int max = BladeEnergyHelper.getMaxEnergy(stack);
            if (max <= 0) continue;

            drawEnergyBar(guiGraphics, slotX, slotY, current, max);
            drawEnergyNumber(guiGraphics, slotX, slotY, current, max);
        }
    }

    private static void drawEnergyBar(GuiGraphics guiGraphics, int slotX, int slotY, int current, int max) {
        // 背景
        guiGraphics.fill(slotX + BAR_X_OFFSET, slotY + BAR_Y_OFFSET,
                slotX + BAR_X_OFFSET + BAR_WIDTH, slotY + BAR_Y_OFFSET + BAR_HEIGHT,
                BAR_BG_COLOR);
        // 填充
        float ratio = (float) current / max;
        int fillWidth = Math.max(1, (int) (BAR_WIDTH * ratio));
        guiGraphics.fill(slotX + BAR_X_OFFSET, slotY + BAR_Y_OFFSET,
                slotX + BAR_X_OFFSET + fillWidth, slotY + BAR_Y_OFFSET + BAR_HEIGHT,
                BAR_FILL_COLOR);
    }

    private static void drawEnergyNumber(GuiGraphics guiGraphics, int slotX, int slotY, int current, int max) {
        Minecraft mc = Minecraft.getInstance();
        String text = BladeEnergyHelper.formatEnergy(current) + "/" + BladeEnergyHelper.formatEnergy(max);
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(slotX + 2, slotY + BAR_Y_OFFSET - 4, 0);
        pose.scale(0.5f, 0.5f, 1f);
        guiGraphics.drawString(mc.font, text, 0, 0, 0x55FF55, false);
        pose.popPose();
    }


}