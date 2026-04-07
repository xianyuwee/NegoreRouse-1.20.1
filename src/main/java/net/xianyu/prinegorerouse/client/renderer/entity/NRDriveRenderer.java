package net.xianyu.prinegorerouse.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.client.renderer.util.MSAutoCloser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xianyu.prinegorerouse.entity.EntityNRDrive;
import net.xianyu.prinegorerouse.prinegorerouse;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class NRDriveRenderer<T extends EntityNRDrive> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = prinegorerouse.prefix("model/util/ss.png");
    private static final ResourceLocation MODEL = prinegorerouse.prefix( "model/util/drive_5ye.obj");

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.getTextureLoc();
    }

    public NRDriveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int packedLightIn) {
        // ====================== 渲染调试代码 START ======================
        // 仅客户端打印，区分渲染帧
//        String debugMsg = String.format(
//                "[NRDrive渲染调试] ID:%d | 插值Yaw:%.2f | 插值Pitch:%.2f | 同步InitialYaw:%.2f | InitialPitch:%.2f | 实体原生Yaw:%.2f | 原生Pitch:%.2f | Roll:%.2f | pos_x:%.2f | pos_y:%.2f | pos_z:%.2f |",
//                entity.getId(),
//                Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()), // 渲染插值角度
//                Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot()),
//                entity.getInitialYaw(), // 同步过来的初始旋转（核心）
//                entity.getInitialPitch(),
//                entity.getYRot(), // 实体原生旋转
//                entity.getXRot(),
//                entity.getRotationRoll(),
//                entity.getX(),
//                entity.getY(),
//                entity.getZ()
//        );
//        // 输出到客户端日志/控制台
//        SlashBlade.LOGGER.info(debugMsg);
        // ====================== 渲染调试代码 END ======================

        try (MSAutoCloser msac = MSAutoCloser.pushMatrix(matrixStack)) {
            float lifetime = entity.getLifetime();
            double deathTime = lifetime;
            double baseAlpha = (Math.min(deathTime, Math.max(0, (lifetime - (entity.tickCount)))) / deathTime);
            baseAlpha = Math.max(0, -Math.pow(baseAlpha - 1, 4.0) + 0.75);

            // ====================== 【纯固定旋转】无插值、无原生旋转，永久不抖 ======================
            matrixStack.mulPose(Axis.YP.rotationDegrees(entity.getInitialYaw() - 90.0F));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(entity.getInitialPitch()));
            matrixStack.mulPose(Axis.XP.rotationDegrees(entity.getRotationRoll()));

            float scale = entity.getBaseSize();
            matrixStack.scale(scale, scale, scale);
            matrixStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            int color = entity.getColor() & 0xFFFFFF;
            int alpha = ((0xFF & (int) (0xFF * baseAlpha)) << 24);
            WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);

            BladeRenderState.setCol(color | alpha);
            BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, "drive_5ye", TEXTURE, matrixStack, bufferSource, packedLightIn);
        }
    }
}
