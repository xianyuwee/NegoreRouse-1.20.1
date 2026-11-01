package net.xianyu.prinegorerouse.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.xianyu.prinegorerouse.client.renderer.EnergyBladeBEWLR;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ClientItemHelper {
    private ClientItemHelper() {
    }

    public static boolean isShiftKeyDown() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.options == null) {
            return false;
        }
        KeyMapping shift = mc.options.keyShift;
        if (shift == null) {
            return false;
        }
        InputConstants.Key key = shift.getKey();
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    public static IClientItemExtensions getEnergyBladeItemExtensions() {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new EnergyBladeBEWLR(
                            Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                            Minecraft.getInstance().getEntityModels());
                }
                return renderer;
            }
        };
    }
}
