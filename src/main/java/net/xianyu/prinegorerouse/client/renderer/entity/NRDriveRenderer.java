package net.xianyu.prinegorerouse.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
        try (MSAutoCloser msac = MSAutoCloser.pushMatrix(matrixStack)) {
            float lifetime = entity.getLifetime();
            double deathTime = lifetime;
            double baseAlpha = (Math.min(deathTime, Math.max(0, (lifetime - (entity.tickCount))))
                    / deathTime);
            baseAlpha = Math.max(0, -Math.pow(baseAlpha - 1, 4.0) + 0.75);

            matrixStack.mulPose(
                    Axis.YP.rotationDegrees(Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot())));
            matrixStack.mulPose(Axis.XP.rotationDegrees(entity.getRotationRoll()));

            float scale = entity.getBaseSize();
            matrixStack.scale(scale, scale, scale);
            matrixStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            int color = entity.getColor() & 0xFFFFFF;
            int alpha = ((0xFF & (int) (0xFF * baseAlpha)) << 24);
            WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);

            BladeRenderState.setCol(color | alpha);
            BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, "drive_5ye", TEXTURE, matrixStack, bufferSource,
                    packedLightIn);
        }
    }
}
