package net.xianyu.prinegorerouse.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.client.particle.BackStarParticle;
import net.xianyu.prinegorerouse.client.particle.BlackHoleParticle;
import net.xianyu.prinegorerouse.client.particle.FateEyeParticle;
import net.xianyu.prinegorerouse.client.particle.LightningBoltParticle;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrParticlesRegistry;

/**
 * 粒子 provider 注册（MOD 总线，仅客户端）。
 * <p>回溯星形使用自带贴图的特殊 provider，不需要 particle json。</p>
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NrClientParticles {

    private NrClientParticles() {
    }

    @SubscribeEvent
    public static void onRegisterProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(NrParticlesRegistry.BACK_STAR.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new BackStarParticle(level, x, y, z));
        event.registerSpecial(NrParticlesRegistry.FATE_EYE.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new FateEyeParticle(level, x, y, z));
        event.registerSpecial(NrParticlesRegistry.LIGHTNING_BOLT.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new LightningBoltParticle(level, x, y, z));
        // 黑洞粒子需要完整速度参数（轨道偏移+尺寸编码），原样透传
        event.registerSpecial(NrParticlesRegistry.BLACK_HOLE.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new BlackHoleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed));
        // 星形贴图走独立纹理，在初始化阶段就注册好，避免渲染循环里同步加载贴图
        BackStarParticle.ensureTexture(Minecraft.getInstance().getTextureManager());
        FateEyeParticle.ensureTexture(Minecraft.getInstance().getTextureManager());
        LightningBoltParticle.ensureTexture(Minecraft.getInstance().getTextureManager());
        BlackHoleParticle.ensureTexture(Minecraft.getInstance().getTextureManager());
    }
}
