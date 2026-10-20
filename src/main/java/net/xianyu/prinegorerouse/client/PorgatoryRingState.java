package net.xianyu.prinegorerouse.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.prinegorerouse;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 禁锢（Porgatory）"收容柱 + 封缄环"的客户端表现：叠层由服务端 {@code PorgatoryRingPacket} 同步过来。
 * <p>随层数变化在目标身上呈现一根<b>黑色半透明收容柱</b>与其<b>顶端封缄法环</b>：</p>
 * <ul>
 *   <li><b>高度随层数上升</b>：{@code 1 层}→环贴脚底（此时柱高≈0）、{@code 过半}→到身体中部、
 *       {@code 满层}→升到头顶；柱体自脚底向上生长到环所在高度。</li>
 *   <li><b>竖直缓动动画</b>：环/柱顶随层数变化时以指数平滑移动到新高（增层上升、掉层或消散回落至脚底）。</li>
 *   <li><b>封缄环</b>：脚底固定一枚 + 随层数升降的柱顶一枚。</li>
 *   <li><b>死亡消散</b>：目标死亡时逐层解除（每 {@code DEATH_DROP_TICKS} tick 掉一层）再淡出，避免直接消失。</li>
 *   <li><b>柱体脉动</b>：收容柱半径随时间轻微呼吸式缩放，幅度随层数（{@code fill}）增大。</li>
 *   <li><b>环的填充</b>：底层 {@code rings/porgatory_empty.png} 描边环常亮，叠加 {@code rings/porgatory_full.png}
 *       黑环，其不透明度随 {@code stacks/max} 递增——层数越高黑越满、满层即 {@code porgatory_full}。</li>
 *   <li><b>柱体浓淡</b>：黑色半透明圆柱（{@code POSITION_COLOR}，不写深度）不透明度亦随层数加深，强化"收容"感。</li>
 * </ul>
 * <p>环平铺于水平面（XZ）并缓慢自转、柱面借透视自然获得立体感；走 {@code AFTER_ENTITIES} 阶段（主帧缓冲、
 * 相机 modelview 参考系，与下方 translate(entity-cam) 数学匹配），开深度测试但不写深度，故被前景方块正常遮挡、
 * 又不反过来遮住实体。（注：{@code AFTER_LEVEL} 的 PoseStack 烘入了投影矩阵、无相机 modelview，会致错位；
 * {@code AFTER_WEATHER} 在 Fabulous 下画到离屏天气缓冲、深度与主场景不一致会被剔除，均不适用。）</p>
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PorgatoryRingState {

    private static final ResourceLocation EMPTY_RING = prinegorerouse.prefix("textures/rings/porgatory_empty.png");
    private static final ResourceLocation FULL_RING = prinegorerouse.prefix("textures/rings/porgatory_full.png");

    /** 整环/柱淡入淡出的满程步长（每 tick 朝目标不透明度移动的量）。 */
    private static final float ALPHA_STEP = 1.0F / 6.0F;
    /** 黑色填充逐层逼近的步长（每 tick 朝目标填充度移动的量）。 */
    private static final float FILL_STEP = 1.0F / 10.0F;
    /** 高度竖直缓动系数：每 tick 朝目标高度移动剩余量的此比例（越大越快）。 */
    private static final float HEIGHT_LERP = 0.22F;
    /** 法环自转速度（度 / tick）。 */
    private static final float SPIN_DEG_PER_TICK = 2.0F;
    /** 环相对柱顶的抬高，避免与柱顶 z-fighting。 */
    private static final double GROUND_LIFT = 0.03D;
    /** 收容柱侧面分段数。 */
    private static final int CYL_SEGMENTS = 20;
    /** 圆周（弧度），供柱体分段。 */
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    /** 柱体黑色不透明度上下限（按填充度插值；已调得更透明）。 */
    private static final float CYL_ALPHA_MIN = 0.08F;
    private static final float CYL_ALPHA_MAX = 0.28F;
    /** 柱体脉动：半径变化幅度上限（×fill）与角速度（弧度 / tick）。 */
    private static final float CYL_PULSE_AMP = 0.04F;
    private static final float CYL_PULSE_SPEED = 0.1F;
    /** 死亡“掉层”消散：每这么多 tick 解除一层。 */
    private static final int DEATH_DROP_TICKS = 7;

    /** 单个目标的收容柱/法环状态。 */
    private static final class Ring {
        int stacks;              // 服务端同步来的层数（<=0 表示进入淡出）
        int maxStacks = 1;       // 层数上限（来自同步包，避免与硬编码耦合）
        int shownStacks;         // 当前用于呈现的层数：存活时=stacks，死亡时逐层递减做消散动画
        boolean dying;           // 目标是否已死亡/移除（进入掉层消散）
        int dropTimer;           // 死亡消散的层间隔时计数
        float curAlpha;          // 当前主不透明度（0~1）
        float prevAlpha;
        float curFill;           // 当前黑色填充度（0~1）
        float prevFill;
        float entityHeight = 2.0F; // 实体总高（脚底→头顶），用于把环映射到相应高度
        float curHeight;         // 当前柱高/环高（脚底以上的方块数）
        float prevHeight;
        double prevX, prevY, prevZ;   // 上一 tick 快照位置（供渲染帧间插值）
        double curX, curY, curZ;
        boolean hasPos;
    }

    /** 服务端同步的叠层缓存：实体 id -> 状态。 */
    private static final Map<Integer, Ring> RINGS = new HashMap<>();

    private PorgatoryRingState() {
    }

    /** 由 {@code PorgatoryRingPacket} 在客户端线程调用：写入/更新某目标的层数。 */
    public static void update(int entityId, int stacks, int maxStacks) {
        if (stacks <= 0) {
            Ring r = RINGS.get(entityId);
            if (r != null) {
                r.stacks = 0; // 触发淡出与回落，实际移除在 tick 中完成
            }
            return;
        }
        Ring r = RINGS.computeIfAbsent(entityId, k -> new Ring());
        r.stacks = stacks;
        r.maxStacks = Math.max(1, maxStacks);
        if (!r.dying) {
            r.shownStacks = stacks; // 非死亡态直接同步显示层
        }
    }

    /** 每 tick：推进淡入淡出、填充与高度缓动、刷新位置快照；淡尽或实体消失则移除。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // 世界卸载 / 回到标题等：清空全部，避免残留。
        if (mc.level == null || mc.player == null) {
            RINGS.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Ring>> it = RINGS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Ring> en = it.next();
            Ring r = en.getValue();
            Entity e = mc.level.getEntity(en.getKey());
            // 死亡/被移除则进入“掉层”消散动画（逐层解除，避免直接消失太突兀）
            boolean dead = e == null || e.isRemoved()
                    || (e instanceof LivingEntity le && (le.isDeadOrDying() || le.getHealth() <= 0.0F));
            if (dead && !r.dying) {
                r.dying = true;
                r.dropTimer = 0;
            }
            if (r.dying) {
                // 每隔 DEATH_DROP_TICKS 掉一层，直到 0（模拟禁锢逐层解除）
                if (++r.dropTimer >= DEATH_DROP_TICKS) {
                    r.dropTimer = 0;
                    r.shownStacks = Math.max(0, r.shownStacks - 1);
                }
            } else {
                r.shownStacks = r.stacks;
            }
            int shown = r.shownStacks;
            // 位置与身高快照：prev=上一帧 cur，cur=实体当前值
            if (e != null && !e.isRemoved()) {
                r.prevX = r.curX;
                r.prevY = r.curY;
                r.prevZ = r.curZ;
                r.curX = e.getX();
                r.curY = e.getY();
                r.curZ = e.getZ();
                if (!r.hasPos) {
                    r.prevX = r.curX;
                    r.prevY = r.curY;
                    r.prevZ = r.curZ;
                    r.hasPos = true;
                }
                r.entityHeight = Math.max(0.5F, e.getBbHeight());
            }
            r.prevAlpha = r.curAlpha;
            r.prevFill = r.curFill;
            r.prevHeight = r.curHeight;

            float targetAlpha = shown > 0 ? 1.0F : 0.0F;
            float targetFill = shown > 0 ? (float) shown / r.maxStacks : 0.0F;
            r.curAlpha = approach(r.curAlpha, targetAlpha, ALPHA_STEP);
            r.curFill = approach(r.curFill, targetFill, FILL_STEP);

            // 高度目标：1 层=脚底(0)、满层=头顶(entityHeight)，随层数线性；掉层/消散回到 0（脚底）
            float level = shown > 0 && r.maxStacks > 1 ? (float) (shown - 1) / (r.maxStacks - 1) : 0.0F;
            float targetHeight = shown > 0 ? r.entityHeight * Mth.clamp(level, 0.0F, 1.0F) : 0.0F;
            r.curHeight += (targetHeight - r.curHeight) * HEIGHT_LERP;
            if (Math.abs(targetHeight - r.curHeight) < 0.01F) {
                r.curHeight = targetHeight;
            }

            // 掉层结束/到期且彻底淡出：移除，避免表膨胀
            if (shown <= 0 && r.curAlpha <= 0.0F && (r.dying || r.stacks <= 0)) {
                it.remove();
            }
        }
    }

    /** 在 {@code AFTER_ENTITIES} 阶段（主帧缓冲、相机参考系正确），为每个目标绘制收容柱与脚底/柱顶两枚环。 */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || RINGS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float pt = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ms = event.getPoseStack();
        float time = mc.level.getGameTime() + pt;
        float spin = (float) ((time * SPIN_DEG_PER_TICK) % 360.0D);

        // 透明特效状态：普通 alpha 混合、写深度关闭但开启深度测试——
        // 由地形/方块遮挡环与柱（不吃阻挡），但环/柱自身不写深度，故不会反过来遮住实体。
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        for (Map.Entry<Integer, Ring> en : RINGS.entrySet()) {
            Entity e = mc.level.getEntity(en.getKey());
            if (e == null) {
                continue;
            }
            Ring r = en.getValue();
            float alpha = Mth.clamp(Mth.lerp(pt, r.prevAlpha, r.curAlpha), 0.0F, 1.0F);
            if (alpha <= 0.01F || !r.hasPos) {
                continue;
            }
            float fill = Mth.clamp(Mth.lerp(pt, r.prevFill, r.curFill), 0.0F, 1.0F);
            float height = Mth.lerp(pt, r.prevHeight, r.curHeight);
            double x = Mth.lerp(pt, r.prevX, r.curX) - cam.x;
            double y = Mth.lerp(pt, r.prevY, r.curY) - cam.y;   // 脚底（世界相对）
            double z = Mth.lerp(pt, r.prevZ, r.curZ) - cam.z;
            float size = ringRadius(e);
            // 柱体随层数轻微脉动：幅度随 fill 增大、频率固定，按实体 id 错相
            float pulse = (float) Math.sin((time + en.getKey() * 0.7F) * CYL_PULSE_SPEED) * CYL_PULSE_AMP * fill;

            ms.pushPose();
            ms.translate(x, y, z);
            // 收容柱（脚底→柱顶，带脉动）
            drawCylinder(ms, size * (1.0F + pulse), height, alpha * Mth.lerp(fill, CYL_ALPHA_MIN, CYL_ALPHA_MAX));
            // 水平环：脚底固定 + 柱顶随层数升降
            drawRingAt(ms, size, spin, alpha, fill, 0.0F);
            drawRingAt(ms, size, spin, alpha, fill, height);
            ms.popPose();
        }

        // 复位渲染状态，交回后续渲染（深度测试保持开启，符合世界渲染默认）。
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** 依实体宽度给出柱/环半径（随体型略变）。 */
    private static float ringRadius(Entity e) {
        return 0.6F + e.getBbWidth() * 0.6F;
    }

    /** 黑色半透明收容柱侧面：以当前原点（脚底）向上延展 {@code height} 格。 */
    private static void drawCylinder(PoseStack ms, float radius, float height, float alpha) {
        if (height <= 0.03F || alpha <= 0.004F) {
            return;
        }
        Matrix4f m = ms.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < CYL_SEGMENTS; i++) {
            float a0 = (float) i / CYL_SEGMENTS * TWO_PI;
            float a1 = (float) (i + 1) / CYL_SEGMENTS * TWO_PI;
            float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;
            buffer.vertex(m, x0, 0.0F, z0).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            buffer.vertex(m, x1, 0.0F, z1).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            buffer.vertex(m, x1, height, z1).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            buffer.vertex(m, x0, height, z0).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
    }

    /** 在脚底以上 {@code y} 处水平绘制一枚环：empty 常亮 + full 黑按 fill 叠加。 */
    private static void drawRingAt(PoseStack ms, float size, float spinDeg, float alpha, float fill, float y) {
        ms.pushPose();
        ms.translate(0.0D, y + GROUND_LIFT, 0.0D);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0F));   // 由竖直面放平到水平 XZ
        ms.mulPose(Axis.ZP.rotationDegrees(spinDeg));   // 平面内缓慢自转
        Matrix4f m = ms.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        // 底层：空心描边环
        RenderSystem.setShaderTexture(0, EMPTY_RING);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        drawQuad(m, size);

        // 叠加：满黑环，按填充度调暗——层数越高黑越满，满层即呈现 full.png
        if (fill > 0.0F) {
            RenderSystem.setShaderTexture(0, FULL_RING);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * fill);
            drawQuad(m, size);
        }
        ms.popPose();
    }

    /** 以当前贴图绘制一张 size×size 的方形贴花（局部 XY 平面，放平后即水平面）。 */
    private static void drawQuad(Matrix4f m, float size) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(m, -size, -size, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(m, size, -size, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(m, size, size, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(m, -size, size, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    /** 把 {@code cur} 以不超过 {@code step} 的步长朝 {@code target} 逼近。 */
    private static float approach(float cur, float target, float step) {
        if (cur < target) {
            return Math.min(target, cur + step);
        } else if (cur > target) {
            return Math.max(target, cur - step);
        }
        return cur;
    }
}
