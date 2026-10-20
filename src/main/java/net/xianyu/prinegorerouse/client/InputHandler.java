package net.xianyu.prinegorerouse.client;

import com.mojang.blaze3d.platform.InputConstants;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.NetworkPacketHandler;
import net.xianyu.prinegorerouse.PowerSwitchPacket;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class InputHandler {
    public static final KeyMapping KEY_CHARGE = new KeyMapping("key.prinegorerouse.charge_switch",
            KeyConflictContext.IN_GAME, KeyModifier.SHIFT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
            "key.category.prinegorerouse");

    // 打开时间素 HUD 格子的位置拖拽编辑界面（默认绑定数字小键盘 8）。
    public static final KeyMapping KEY_EDIT_HUD = new KeyMapping("key.prinegorerouse.edit_hud",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_8,
            "key.category.prinegorerouse");

    // 打开特效时间素激活界面（需手持拔刀剑）（默认绑定数字小键盘 9）。
    public static final KeyMapping KEY_SE_MANAGE = new KeyMapping("key.prinegorerouse.se_manage",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_9,
            "key.category.prinegorerouse");

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent()
    public static void onPlayerPostTick(InputEvent.Key event) {
        @SuppressWarnings("resource")
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null)
            return;

        // 打开 HUD 位置编辑界面（与是否持刀无关）
        while (KEY_EDIT_HUD.consumeClick()) {
            Minecraft.getInstance().setScreen(new AquaHudEditScreen());
        }

        if (player.getMainHandItem().isEmpty() || !player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).isPresent())
            return;

        // 手持拔刀剑时按下打开特效激活界面
        while (KEY_SE_MANAGE.consumeClick()) {
            Minecraft.getInstance().setScreen(new SEActiveScreen());
        }

        if(InputHandler.KEY_CHARGE.isDown()) {
            NetworkPacketHandler.INSTANCE.sendToServer(new PowerSwitchPacket("triggered"));
        }
    }
}
