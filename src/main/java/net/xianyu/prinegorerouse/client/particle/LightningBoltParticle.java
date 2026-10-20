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
 * 附魔剑命中的<b>闪电贴花</b>粒子：以复用 {@code FateEyeParticle}/{@code BackStarParticle} 的
 * “自带独立贴图 + 自定义加法 {@link ParticleRenderType}”范式（不走粒子图集、无需 particle json），
 * 在命中点绘制一道竖直的锯齿雷光，短寿命、先亮后淡出、轻微抖动，替代真实 {@code LightningBolt} 实体
 * 以规避掉帧与雷声过大问题。纯客户端装饰，不产生任何伤害或火焰。
 */
@OnlyIn(Dist.CLIENT)
public class LightningBoltParticle extends SingleQuadParticle {

    /** 闪电贴图（白芯+蓝辉+紫外晕的竖直锯齿雷光）。 */
    public static final ResourceLocation TEXTURE =
            prinegorerouse.prefix("textures/particle/lightning_bolt.png");

    private static boolean textureRegistered = false;

    /** 加法混合发光渲染类型：叠加更亮、不写深度、自发光。 */
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
            return "NR_LIGHTNING_GLOW";
        }
    };

    // ===== 微调旋钮 =====
    private static final int LIFETIME = 9;          // 寿命（tick）：雷光一闪即逝
    private static final float QUAD_SIZE = 1.15F;   // 贴花半边长（米）
    private static final float MAX_ALPHA = 1.0F;    // 峰值不透明度
    private static final float XZ_DRIFT = 0.05F;    // 水平微抖动幅度

    /** 把闪电贴图作为独立纹理注册进 {@link TextureManager}（不进粒子图集）。 */
    public static void ensureTexture(TextureManager manager) {
        if (textureRegistered) {
            return;
        }
        AbstractTexture texture = new SimpleTexture(TEXTURE);
        texture.setFilter(true, false); // 线性过滤，柔化雷光边缘
        manager.register(TEXTURE, texture);
        textureRegistered = true;
    }

    public LightningBoltParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        // 仅水平微抖，竖直保持：像雷光在原位闪烁
        this.setParticleSpeed(
                (this.random.nextFloat() - 0.5F) * XZ_DRIFT, 0.0D,
                (this.random.nextFloat() - 0.5F) * XZ_DRIFT);
        this.setLifetime(LIFETIME);
        this.quadSize = QUAD_SIZE;
        this.setColor(1.0F, 1.0F, 1.0F); // 白色，颜色交给贴图本身
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        // 不施加物理：雷光悬浮于命中点
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        this.alpha = MAX_ALPHA * flash(progress(partialTicks));
        super.render(buffer, camera, partialTicks);
    }

    /** 爆闪曲线：起手瞬间拉满、随后带一次高频抖动地衰减，模拟雷光的“噼啪”闪烁。 */
    private float flash(float t) {
        if (t <= 0.0F) {
            return 1.0F;
        }
        float decay = 1.0F - t;                 // 线性回落
        float jitter = 0.75F + 0.25F * Mth.sin(t * 26.0F); // 高频明暗抖动
        return Mth.clamp(decay * jitter, 0.0F, 1.0F);
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
