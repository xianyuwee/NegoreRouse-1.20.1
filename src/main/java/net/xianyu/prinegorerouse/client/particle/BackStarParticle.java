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
import net.xianyu.prinegorerouse.specialeffect.NRSEs.Back;

/**
 * 回溯（back）的金色四芒星粒子：一个完整的“点 → 四芒星 → 点”闪烁动画。
 * <p>纯客户端装饰，不做实体。贴图是一张自带柔光的白色四芒星，金色靠粒子颜色相乘得到；
 * 纹理直接单独绑定（不进粒子图集），因此无需 particle json 与图集拼接。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BackStarParticle extends SingleQuadParticle {

    /** 星形贴图（四芒星 + 高光核），白色便于按粒子色染成金色。 */
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(prinegorerouse.MOD_ID, "textures/particle/back_star.png");

    private static boolean textureRegistered = false;

    /** 加法混合的发光渲染类型：星芒叠加后中心更亮，且不写深度。 */
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
            return "NR_BACK_STAR_GLOW";
        }
    };

    // ===== 微调旋钮 =====
    private static final float BASE_SCALE = 0.16F;   // 两端“点”的尺寸占比
    private static final float MIN_ALPHA = 0.25F;    // 两端“点”的亮度

    /**
     * 把星形贴图作为独立纹理注册进 {@link TextureManager}（不进粒子图集，所以无需 particle json / 图集拼接）。
     * <p>客户端初始化时调一次即可；渲染 {@code begin} 里再保底调一次，防资源重载后丢注册。</p>
     */
    public static void ensureTexture(TextureManager manager) {
        if (textureRegistered) {
            return;
        }
        AbstractTexture texture = new SimpleTexture(TEXTURE);
        texture.setFilter(true, false); // 线性过滤，让柔光不被像素化
        manager.register(TEXTURE, texture);
        textureRegistered = true;
    }

    /** 完整展开时星形的半边长（米）。 */
    private final float maxSize;
    /** 每 tick 的自旋角速度（弧度），让每次闪烁的星芒朝向略有不同。 */
    private final float rollSpeed;

    public BackStarParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.setLifetime(Back.STAR_LIFETIME);
        this.maxSize = 0.17F + this.random.nextFloat() * 0.11F;
        this.roll = (this.random.nextFloat() - 0.5F) * 0.6F;
        this.rollSpeed = (this.random.nextFloat() - 0.5F) * 0.05F;
        this.setColor(1.0F, 0.78F, 0.32F); // 金色
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        this.roll += this.rollSpeed;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        this.alpha = MIN_ALPHA + (1.0F - MIN_ALPHA) * pulse(progress(partialTicks));
        super.render(buffer, camera, partialTicks);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        // 始终保留一点尺寸做“点”，峰值时展开成完整四芒星
        return this.maxSize * (BASE_SCALE + (1.0F - BASE_SCALE) * pulse(progress(partialTicks)));
    }

    /** 0 → 1 → 0 的单脉冲：两端是“点”，中段是“星”。 */
    private float pulse(float t) {
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
        return LightTexture.FULL_BRIGHT; // 自发光，不受环境亮度影响
    }

    @Override
    public ParticleRenderType getRenderType() {
        return GLOW;
    }
}
