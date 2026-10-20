package net.xianyu.prinegorerouse.client.renderer.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xianyu.prinegorerouse.entity.EntityFateEye;
import net.xianyu.prinegorerouse.prinegorerouse;
import org.joml.Matrix4f;

/**
 * 炯恻之眼渲染器：一张朝向相机的“眼球”方形贴花 + 一条从眼射向锁定目标的激光光束。
 * <p>眼球按同步的开合进度 {@code OPEN} 在 {@code fate_eye_closed.png} 与 {@code fate_eye_open.png} 间做
 * crossfade（先画闭合、再按 {@code OPEN} 叠画睁开），实现闭→睁→闭动画；{@code OPEN} 高于阈值且目标存在时
 * 用 {@code fate_beam.png} 画一条沿“眼→目标”方向拉伸、横向平铺的光束 quad。全程不写深度、加法/普通混合、
 * 借 {@code POSITION_TEX} 无光照顶点格式自发光。</p>
 */
@OnlyIn(Dist.CLIENT)
public class FateEyeRenderer extends EntityRenderer<EntityFateEye> {

    private static final ResourceLocation OPEN_TEX =
            prinegorerouse.prefix("textures/entity/fate_eye_open.png");
    private static final ResourceLocation CLOSED_TEX =
            prinegorerouse.prefix("textures/entity/fate_eye_closed.png");
    private static final ResourceLocation BEAM_TEX =
            prinegorerouse.prefix("textures/entity/fate_beam.png");

    // ===== 微调旋钮 =====
    private static final float EYE_HALF = 0.5F;        // 眼球贴花半边长
    private static final float BEAM_THRESHOLD = 0.5F;  // 低于此开度不画光束
    private static final float BEAM_HALF_WIDTH = 0.4F; // 光束半宽（加粗）
    private static final float BEAM_TILE_LEN = 1.5F;    // 光束贴图沿长度平铺的每格 UV=1

    public FateEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFateEye entity) {
        return OPEN_TEX;
    }

    @Override
    public void render(EntityFateEye entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float open = entity.getOpen(partialTick);
        Entity target = entity.getTargetEntity();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // 光束：开度足够且目标存活时绘制（画在眼球之前，令眼球盖住出射点）
        if (open > BEAM_THRESHOLD && target != null) {
            drawBeam(poseStack, entity, target, partialTick, open);
        }

        // 眼球：billboard 朝相机的方形贴花
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        Matrix4f m = poseStack.last().pose();

        RenderSystem.setShaderTexture(0, CLOSED_TEX);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawQuad(m, EYE_HALF);

        if (open > 0.004F) {
            RenderSystem.setShaderTexture(0, OPEN_TEX);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, open);
            drawQuad(m, EYE_HALF);
        }
        poseStack.popPose();

        // 复位渲染状态，交回后续实体渲染
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /** 绘制从眼到目标的一块拉伸光束 quad（在实体局部空间，原点即眼位；仅平移、无旋转，故局部轴=世界轴）。 */
    private void drawBeam(PoseStack poseStack, EntityFateEye eye, Entity target, float partialTick, float open) {
        double ex = Mth.lerp(partialTick, eye.xo, eye.getX());
        double ey = Mth.lerp(partialTick, eye.yo, eye.getY());
        double ez = Mth.lerp(partialTick, eye.zo, eye.getZ());
        double tx = Mth.lerp(partialTick, target.xo, target.getX());
        double ty = Mth.lerp(partialTick, target.yo, target.getY()) + target.getBbHeight() * 0.5;
        double tz = Mth.lerp(partialTick, target.zo, target.getZ());

        Vec3 rel = new Vec3(tx - ex, ty - ey, tz - ez);
        double len = rel.length();
        if (len < 1.0E-3) {
            return;
        }
        Vec3 dir = rel.scale(1.0 / len);
        // 取与光束方向不平行的“上”向量构造横向宽度
        Vec3 up = Math.abs(dir.y) > 0.98 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up).normalize().scale(BEAM_HALF_WIDTH);

        float uTiles = (float) (len / BEAM_TILE_LEN);
        float alpha = Mth.clamp((open - BEAM_THRESHOLD) / (1.0F - BEAM_THRESHOLD), 0.0F, 1.0F);

        Matrix4f m = poseStack.last().pose();
        float sx = (float) side.x, sy = (float) side.y, sz = (float) side.z;
        float rx = (float) rel.x, ry = (float) rel.y, rz = (float) rel.z;

        RenderSystem.setShaderTexture(0, BEAM_TEX);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(m, sx, sy, sz).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(m, rx + sx, ry + sy, rz + sz).uv(uTiles, 0.0F).endVertex();
        buffer.vertex(m, rx - sx, ry - sy, rz - sz).uv(uTiles, 1.0F).endVertex();
        buffer.vertex(m, -sx, -sy, -sz).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    /** 在当前贴图下绘制一张以原点为中心、半边长 {@code half} 的方形贴花（局部 XY 平面，已朝向相机）。 */
    private void drawQuad(Matrix4f m, float half) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(m, -half, -half, 0.0F).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(m, half, -half, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(m, half, half, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(m, -half, half, 0.0F).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }
}
