package net.xianyu.prinegorerouse.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.xianyu.prinegorerouse.prinegorerouse;

/**
 * 本模组的粒子类型注册。粒子只做纯客户端视觉表现，不产生任何实体。
 */
public final class NrParticlesRegistry {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, prinegorerouse.MOD_ID);

    /** 回溯（back）的金色四芒星：点 → 四芒星 → 点 的闪烁动画，参数在粒子内部随机。 */
    public static final RegistryObject<SimpleParticleType> BACK_STAR =
            PARTICLES.register("back_star", () -> new SimpleParticleType(false));

    /** 炯恻（Fate）未激活时的闭合眼指示：常驻玩家头顶的闭眼贴花粒子。 */
    public static final RegistryObject<SimpleParticleType> FATE_EYE =
            PARTICLES.register("fate_eye", () -> new SimpleParticleType(false));

    /** 附魔剑命中的闪电贴花：替代真实 LightningBolt 实体的纯视觉锯齿雷击。 */
    public static final RegistryObject<SimpleParticleType> LIGHTNING_BOLT =
            PARTICLES.register("lightning_bolt", () -> new SimpleParticleType(false));

    /** 永恒瞳矇（Eternity）层数可视化：黑洞核心/环绕粒子，速度参数编码轨道向量与尺寸。 */
    public static final RegistryObject<SimpleParticleType> BLACK_HOLE =
            PARTICLES.register("black_hole", () -> new SimpleParticleType(false));

    private NrParticlesRegistry() {
    }
}
