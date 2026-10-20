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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;
import net.xianyu.prinegorerouse.prinegorerouse;
import org.joml.Matrix4f;

/**
 * Phantom（幻影）的客户端渲染：在天空额外绘制一个对应 {@code dayTime + 13000} 的天体。
 * <p>当玩家<b>主手</b>持有“携带 Phantom 且已激活”的刀时，于 {@code AFTER_SKY} 渲染阶段，
 * 以原版风格（复用 {@code sun.png} / {@code moon_phases.png} 与原版顶点/旋转/混合状态）自绘一个天体，
 * 其角度等于“把世界时间推进 13000 tick 后”原版本该显示的那颗——于是白天多出一轮幻影月亮、
 * 夜晚多出一轮幻影太阳（太阳↔月亮对调）。因走 AFTER_SKY、地形尚未绘制，故能被山体/建筑正常遮挡。</p>
 * <p>已知取舍（与用户确认）：这是自绘而非注入原版 {@code renderSky}，光影包(Iris)可能不会像对待真
 * 日/月那样为它着色；且即便某 mod 隐藏了原版日/月，幻影天体仍会显示。仅在主世界类（{@code NORMAL}）
 * 天空维度生效，末地 / 地狱等无日月天空的维度自动跳过。</p>
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PhantomSkyRenderer {

    // 原版天体贴图（与 LevelRenderer 内的同名私有常量一致，故材质包替换 sun.png/moon_phases.png 时自动跟随）。
    private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation MOON_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");

    /** 幻影相对当前世界时间的偏移，折算为一个完整昼夜的比例：13000 / 24000。 */
    private static final float PHANTOM_TIME_OFFSET = 13000.0F / 24000.0F;

    /** 淡入 / 淡出各自的满程时长（tick）：约 40t 走完 无→完全 或 完全→无 的渐变。 */
    private static final int FADE_TICKS = 40;

    /** 上一 tick 边界与当前 tick 边界的线性淡入度（0=不可见，1=完全渲染）；渲染时按 partialTick 插值再做缓入缓出。 */
    private static float prevFade = 0.0F;
    private static float curFade = 0.0F;

    private PhantomSkyRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        // 仅在渲染原版日/月的天空维度出现；末地(END)、地狱等无日月天空自动跳过。
        if (level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL) {
            return;
        }
        // 用 tick 间插值 + 缓入缓出得到当前不透明度；淡出进行中（手持条件已不满足但 fade>0）仍继续绘制。
        float pt = event.getPartialTick();
        float linear = Mth.clamp(Mth.lerp(pt, prevFade, curFade), 0.0F, 1.0F);
        if (linear <= 0.0F) {
            return;
        }
        drawPhantomCelestial(event.getPoseStack(), level, pt, easeInOut(linear));
    }

    /** 每 tick 把淡入度朝目标（手持并激活=1 / 否则=0）推进 1/FADE_TICKS，并记录上一 tick 值供渲染插值。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        prevFade = curFade;
        Minecraft mc = Minecraft.getInstance();
        // 世界卸载 / 无玩家时直接归零，避免下次进入世界残留半程淡入度。
        if (mc.level == null || mc.player == null) {
            curFade = 0.0F;
            return;
        }
        float step = 1.0F / FADE_TICKS;
        float target = activeInMainhand(mc) ? 1.0F : 0.0F;
        if (curFade < target) {
            curFade = Math.min(target, curFade + step);
        } else if (curFade > target) {
            curFade = Math.max(target, curFade - step);
        }
    }

    /** 缓入缓出（smoothstep）：两端慢、中间快，把线性淡入度映射为不透明度。 */
    private static float easeInOut(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    /** 主手是否持有“携带 Phantom 且已激活”的刀（{@code carriesSE} 走同步能力、{@code isAquaActive} 读同步 NBT，客户端可判）。 */
    private static boolean activeInMainhand(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        ResourceLocation id = NrSpecialEffectsRegistry.Phantom.getId();
        ItemStack main = mc.player.getMainHandItem();
        return NRSpecialEffect.carriesSE(main, id) && NRSpecialEffect.isAquaActive(main, id);
    }

    /**
     * 复刻原版 {@code LevelRenderer#renderSky} 的日/月绘制块，仅把旋转角换成 {@code +13000} 对应角度。
     * <p>AFTER_SKY 拿到的 PoseStack 与原版 renderSky 入口一致：已含相机朝向旋转、不含相机平移，
     * 故天体画在以原点为球心的无穷远球面上即可，无需额外抵消位移。太阳与月亮照旧各画一块，
     * 由几何上下半自然决定当下可见的是哪一颗。</p>
     *
     * @param alpha 缓入缓出后的不透明度（0~1），用于驱动淡入 / 淡出渐变。
     */
    private static void drawPhantomCelestial(PoseStack ms, ClientLevel level, float partialTick, float alpha) {
        float phantomTimeOfDay = (level.getTimeOfDay(partialTick) + PHANTOM_TIME_OFFSET) % 1.0F;
        if (phantomTimeOfDay < 0.0F) {
            phantomTimeOfDay += 1.0F;
        }

        // 天空段已恢复 depthMask(true)，此处按原版日/月块重新进入“不写深度 + 加法混合”。
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        // 加法混合下 alpha 作为源的强度因子：1=完全渲染、0=不可见，据此实现淡入淡出。
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        ms.pushPose();
        ms.mulPose(Axis.YP.rotationDegrees(-90.0F));
        ms.mulPose(Axis.XP.rotationDegrees(phantomTimeOfDay * 360.0F));
        Matrix4f matrix = ms.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        // 太阳：局部 +Y，边长 30，UV 满幅。
        RenderSystem.setShaderTexture(0, SUN_LOCATION);
        float sunSize = 30.0F;
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, -sunSize, 100.0F, -sunSize).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(matrix, sunSize, 100.0F, -sunSize).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, sunSize, 100.0F, sunSize).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, -sunSize, 100.0F, sunSize).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        // 月亮：局部 -Y，边长 20，按当前月相取贴图中的对应格。
        RenderSystem.setShaderTexture(0, MOON_LOCATION);
        int phase = level.getMoonPhase();
        int col = phase % 4;
        int row = phase / 4 % 2;
        float minU = col / 4.0F;
        float maxU = (col + 1) / 4.0F;
        float minV = row / 2.0F;
        float maxV = (row + 1) / 2.0F;
        float moonSize = 20.0F;
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, -moonSize, -100.0F, moonSize).uv(maxU, maxV).endVertex();
        buffer.vertex(matrix, moonSize, -100.0F, moonSize).uv(minU, maxV).endVertex();
        buffer.vertex(matrix, moonSize, -100.0F, -moonSize).uv(minU, minV).endVertex();
        buffer.vertex(matrix, -moonSize, -100.0F, -moonSize).uv(maxU, minV).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        ms.popPose();

        // 复位渲染状态，交回后续的雾 / 地形绘制。
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }
}
