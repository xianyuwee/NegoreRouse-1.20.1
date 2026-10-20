package net.xianyu.prinegorerouse.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xianyu.prinegorerouse.prinegorerouse;

/**
 * 炯恻（Fate）<b>未激活</b>时的闭合眼指示粒子：常驻玩家头顶的一枚闭眼贴花，短寿命、轻微上浮、淡入淡出。
 * <p>纯客户端装饰，复用 {@code BackStarParticle} 的“自带独立贴图 + 自定义 {@link ParticleRenderType}”范式
 * （不走粒子图集、无需 particle json），贴图换成 {@code fate_eye_closed.png}；自发光不受环境亮度影响。</p>
 */
@OnlyIn(Dist.CLIENT)
public class FateEyeParticle extends SingleQuadParticle {

    /** 闭合眼贴图（与实体渲染、指示共用同一张闭眼图）。 */
    public static final ResourceLocation TEXTURE =
            prinegorerouse.prefix("textures/entity/fate_eye_closed.png");

    private static boolean textureRegistered = false;

    /** 加法混合的发光渲染类型：叠加更亮且不写深度。 */
    public static final ParticleRenderType GLOW = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager manager) {
            ensureTexture(manager);
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, manager.getTexture(TEXTURE).getId());
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

        @Override
        public String toString() {
            return "NR_FATE_EYE_GLOW";
        }
    };

    // ===== 微调旋钮 =====
    public static final int LIFETIME = 16;        // 寿命（tick），由指示器周期性补发以保持常驻
    private static final float QUAD_SIZE = 0.34F; // 贴花半边长（米）
    private static final float MAX_ALPHA = 0.9F;  // 峰值不透明度

    /** 把闭眼贴图作为独立纹理注册进 {@link TextureManager}（不进粒子图集）。 */
    public static void ensureTexture(TextureManager manager) {
        if (textureRegistered) {
            return;
        }
        AbstractTexture texture = new SimpleTexture(TEXTURE);
        texture.setFilter(true, false); // 线性过滤，柔化贴花边缘
        manager.register(TEXTURE, texture);
        textureRegistered = true;
    }

    public FateEyeParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        // 轻微上浮，仅作呼吸般的悬浮感
        this.setParticleSpeed(
                (this.random.nextFloat() - 0.5F) * 0.004F, 0.006F, (this.random.nextFloat() - 0.5F) * 0.004F);
        this.setLifetime(LIFETIME);
        this.quadSize = QUAD_SIZE;
        this.setColor(1.0F, 1.0F, 1.0F); // 白色，颜色交给贴图本身
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        // 不施加物理：贴花悬浮于生成点，靠指示器周期性补发维持常驻
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        this.alpha = MAX_ALPHA * fade(progress(partialTicks));
        super.render(buffer, camera, partialTicks);
    }

    /** 淡入淡出（两端 0、中段 1）：sin 半波，指示器补发时前后一枚自然衔接。 */
    private float fade(float t) {
        return Mth.sin((float) Math.PI * Mth.clamp(t, 0.0F, 1.0F));
    }

    private float progress(float partialTicks) {
        return (this.age + partialTicks) / (float) this.lifetime;
    }

    @Override
    protected float getU0() {
        return 0.0F;
    }

    @Override
    protected float getU1() {
        return 1.0F;
    }

    @Override
    protected float getV0() {
        return 0.0F;
    }

    @Override
    protected float getV1() {
        return 1.0F;
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return GLOW;
    }
}
