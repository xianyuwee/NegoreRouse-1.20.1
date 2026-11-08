package net.xianyu.prinegorerouse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.entity.EntityNRBlisteringSword;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 幻影剑拖尾渲染：在所有实体渲染完之后（半透明方块之前）绘制飘带。
 *
 * <p>所有幻影剑统一使用 {@code EntityNRBlisteringSword} 里的拖尾采样（十二式是
 * {@code shouldRecordTrail() = isZenith()}，其余为「磁暴样式」）；拖尾颜色跟随
 * 剑自身的颜色码（{@code 0xRRGGBB}），因此十二式是淡金色、磁暴/清耀/炯恻的剑各是自己的颜色。</p>
 *
 * <p>透明度按采样点<b>采样时刻</b>（挂钟毫秒）计算，渲染时只画
 * {@link EntityNRBlisteringSword#TRAIL_LIFE_MILLIS} 内的采样点：剑停下后拖尾会自然老化、
 * 渐隐消失，不会定格在原地（采样/老化逻辑见 {@code EntityNRBlisteringSword#updateTrail()}）。
 * 用挂钟时间而不是 tick 数，是为了让淡出不受"实体是否还在 tick"影响 —— 幻影剑命中地面后被
 * 父类判定 inGround / 被插附到实体上时会停止 tick，任何基于 tick 的老化都会冻住。</p>
 */
@Mod.EventBusSubscriber(modid = "prinegorerouse", value = Dist.CLIENT)
public class NRClientRenderEvents {

    /** 飘带半宽（格）。 */
    private static final float TRAIL_HALF_WIDTH = 0.12f;
    /** 颜色码过暗时的亮度下限（避免拖尾看不见）。 */
    private static final float TRAIL_MIN_BRIGHTNESS = 0.55f;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 在所有实体渲染完毕后（半透明方块之前）绘制拖尾
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        Matrix4f matrix = poseStack.last().pose();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        // 使用 lightning 渲染管线，它支持 POSITION_COLOR，自带自发光效果且无需纹理
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof EntityNRBlisteringSword sword)) continue;
            if (!sword.itFired() || sword.isRemoved() || !sword.hasTrail()) continue;

            // 颜色取自剑自身的颜色码（0xRRGGBB）；太暗时按比例提亮
            int code = sword.getColor();
            float r = ((code >> 16) & 0xFF) / 255.0f;
            float g = ((code >> 8) & 0xFF) / 255.0f;
            float b = (code & 0xFF) / 255.0f;
            float max = Math.max(r, Math.max(g, b));
            if (max < TRAIL_MIN_BRIGHTNESS) {
                float k = TRAIL_MIN_BRIGHTNESS / Math.max(max, 0.05f);
                r = Math.min(1.0f, r * k);
                g = Math.min(1.0f, g * k);
                b = Math.min(1.0f, b * k);
            }

            drawTrail(consumer, matrix, sword, camPos, r, g, b);
        }
        // 结束批处理，立即渲染
        buffer.endBatch(RenderType.lightning());
    }

    /**
     * 绘制一条飘带拖尾。
     *
     * @param sword 提供采样点与每个点的采样时刻（index 0 = 最新）
     * @param r/g/b 基础颜色（越老越透明）
     */
    private static void drawTrail(VertexConsumer consumer, Matrix4f matrix, EntityNRBlisteringSword sword,
                                  Vec3 camPos, float r, float g, float b) {
        List<Vec3> trail = sword.getTrailPositions();
        if (trail.size() < 2) return;
        long lifeMs = EntityNRBlisteringSword.TRAIL_LIFE_MILLIS;
        // 以挂钟时间算透明度：即使这把剑已经不再 tick（命中地面后被父类冻结），拖尾也会自己淡完
        long now = Util.getMillis();

        for (int i = 0; i < trail.size() - 1; i++) {
            Vec3 p1 = trail.get(i);
            Vec3 p2 = trail.get(i + 1);

            // 坐标需要减去相机位置，因为此时 PoseStack 的原点就是相机
            Vec3 v1 = p1.subtract(camPos);
            Vec3 v2 = p2.subtract(camPos);

            // 采样点几乎重合时跳过：normalize 会得到 NaN，产生无效顶点
            Vec3 seg = v1.subtract(v2);
            if (seg.lengthSqr() < 1.0E-8 || v1.lengthSqr() < 1.0E-8) continue;

            // 计算飘带宽度方向：切线 叉乘 视线
            Vec3 dir = seg.normalize();
            Vec3 toCam = v1.scale(-1).normalize();
            Vec3 side = dir.cross(toCam);
            if (side.lengthSqr() < 1.0E-8) continue;
            side = side.normalize().scale(TRAIL_HALF_WIDTH);

            // 按实际经过时间渐隐：新点最亮，到寿命时完全透明
            float alpha1 = 1.0f - (float) (now - sword.getTrailTime(i)) / (float) lifeMs;
            float alpha2 = 1.0f - (float) (now - sword.getTrailTime(i + 1)) / (float) lifeMs;
            if (alpha1 <= 0.0f && alpha2 <= 0.0f) continue;
            alpha1 = Mth.clamp(alpha1, 0.0f, 1.0f);
            alpha2 = Mth.clamp(alpha2, 0.0f, 1.0f);

            // 绘制四边形
            consumer.vertex(matrix, (float) (v1.x + side.x), (float) (v1.y + side.y), (float) (v1.z + side.z)).color(r, g, b, alpha1).endVertex();
            consumer.vertex(matrix, (float) (v1.x - side.x), (float) (v1.y - side.y), (float) (v1.z - side.z)).color(r, g, b, alpha1).endVertex();
            consumer.vertex(matrix, (float) (v2.x - side.x), (float) (v2.y - side.y), (float) (v2.z - side.z)).color(r, g, b, alpha2).endVertex();
            consumer.vertex(matrix, (float) (v2.x + side.x), (float) (v2.y + side.y), (float) (v2.z + side.z)).color(r, g, b, alpha2).endVertex();
        }
    }
}
