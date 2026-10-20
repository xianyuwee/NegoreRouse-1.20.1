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
 * 永恒瞳矇（Eternity）层数可视化的<b>黑洞粒子</b>：复用 {@code LightningBoltParticle} 的
 * “自带独立贴图 + 自定义加法 {@link ParticleRenderType}”范式（不走粒子图集）。
 * <p>服务端 {@code sendParticles} 的参数编码约定：
 * <ul>
 *   <li>粒子位置 = 轨道上的点（环绕粒子）或黑洞中心（核心粒子）；</li>
 *   <li>xs/zs = 该点相对中心的水平偏移向量（核心粒子传 0,0 → 不环绕）；</li>
 *   <li>ys = 贴花尺寸（米）。</li>
 * </ul>
 * 粒子由 (位置 − 偏移) 反推中心，沿圆轨道匀速扫掠；短寿命 + 高频补发形成连续吸积盘观感。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BlackHoleParticle extends SingleQuadParticle {

    /** 黑洞贴图（透明中心 + 紫蓝发光吸积环）。 */
    public static final ResourceLocation TEXTURE =
            prinegorerouse.prefix("textures/particle/blackhole.png");

    private static boolean textureRegistered = false;

    /** 加法混合发光渲染类型：暗部不增亮、亮环叠加发光。 */
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
            return "NR_BLACKHOLE_GLOW";
        }
    };

    // ===== 微调旋钮 =====
    private static final int BASE_LIFETIME = 12;    // 基础寿命（tick），高频补发形成连续环
    private static final float OMEGA_BASE = 0.55F;  // 角速度基数（rad/tick）
    private static final float OMEGA_R_DROP = 0.30F; // 半径每 +1 格的角速度衰减
    private static final float Y_BOB = 0.03F;       // 轨道竖直微幅

    private double centerX;
    private double centerY;
    private double centerZ;
    private double radiusXZ;   // 轨道水平半径（0=核心粒子不环绕）
    private double yOffset;    // 相对中心的竖直轨道高度
    private float angle;       // 当前极角
    private float omega;       // 角速度 rad/tick

    /** 把黑洞贴图作为独立纹理注册进 {@link TextureManager}（不进粒子图集）。 */
    public static void ensureTexture(TextureManager manager) {
        if (textureRegistered) {
            return;
        }
        AbstractTexture texture = new SimpleTexture(TEXTURE);
        texture.setFilter(true, false);
        manager.register(TEXTURE, texture);
        textureRegistered = true;
    }

    public BlackHoleParticle(ClientLevel level, double x, double y, double z,
                             double xs, double ys, double zs) {
        super(level, x, y, z);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        // 解码服务端参数：xs/zs=中心→本点的水平偏移，ys=贴花尺寸
        double ox = xs;
        double oz = zs;
        this.radiusXZ = Math.sqrt(ox * ox + oz * oz);
        this.centerX = x - ox;
        this.centerY = y;
        this.centerZ = z - oz;
        this.yOffset = 0.0D;
        this.angle = (float) Math.atan2(oz, ox);
        this.omega = radiusXZ > 0.02D ? (float) (OMEGA_BASE - OMEGA_R_DROP * Math.min(radiusXZ, 1.5D)) : 0.0F;
        this.quadSize = (float) Math.max(0.06D, ys);
        this.setLifetime(BASE_LIFETIME + this.random.nextInt(4));
        this.setColor(1.0F, 1.0F, 1.0F); // 颜色交给贴图本身
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.radiusXZ > 0.02D) {
            // 匀速圆周扫掠：位置完全由轨道参数决定，不依赖物理积分
            this.angle += this.omega;
            this.x = this.centerX + this.radiusXZ * Mth.cos(this.angle);
            this.z = this.centerZ + this.radiusXZ * Mth.sin(this.angle);
            this.y = this.centerY + Mth.sin(this.age * 0.6F) * Y_BOB;
        } else {
            // 核心粒子：原地轻微脉动
            this.x = this.centerX + (this.random.nextFloat() - 0.5F) * 0.02D;
            this.z = this.centerZ + (this.random.nextFloat() - 0.5F) * 0.02D;
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float t = (this.age + partialTicks) / (float) this.lifetime;
        // 淡入淡出 + 高频闪烁：吸积物质不稳定感
        float envelope = Mth.sin(Mth.clamp(t, 0.0F, 1.0F) * (float) Math.PI);
        float flicker = 0.8F + 0.2F * Mth.sin((this.age + partialTicks) * 2.1F + this.angle * 7.0F);
        this.alpha = Mth.clamp(envelope * flicker, 0.0F, 1.0F);
        // 环绕粒子随寿命轻微向内坠落（半径收缩），强化“吸入”感
        if (this.radiusXZ > 0.10D) {
            this.radiusXZ *= 0.985D;
        }
        super.render(buffer, camera, partialTicks);
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
