package net.xianyu.prinegorerouse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.client.particle.FateEyeParticle;
import net.xianyu.prinegorerouse.registry.NrParticlesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

/**
 * 炯恻（Fate）<b>未激活指示</b>的客户端表现：本地玩家手持（主/副）一把“携带炯恻但<b>未激活</b>”的刀时，
 * 周期性在其头顶生成一枚闭合眼粒子（{@link FateEyeParticle}）。
 * <p>纯客户端、无需发包：{@code carriesSE} 走同步能力、{@code isAquaActive} 读同步 NBT，客户端可直接判定
 * （判定仿 {@code PhantomSkyRenderer#activeInMainhand}）。激活后本指示自动停止——改由服务端生成“眼”实体输出。</p>
 */
@Mod.EventBusSubscriber(modid = "prinegorerouse", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FateEyeIndicator {

    /** 补发周期（tick）：小于粒子寿命，令前后两枚淡入淡出重叠、呈现常驻。 */
    private static final int RESPAWN_INTERVAL = 10;
    /** 头顶相对眼位的抬高（格）。 */
    private static final double HEAD_LIFT = 0.6;

    private FateEyeIndicator() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.level == null || player == null || player.isSpectator()) {
            return;
        }
        if (player.tickCount % RESPAWN_INTERVAL != 0) {
            return;
        }
        if (!showClosedEye(player)) {
            return;
        }
        Vec3 pos = player.getEyePosition().add(0.0, HEAD_LIFT, 0.0);
        mc.level.addParticle(NrParticlesRegistry.FATE_EYE.get(), false,
                pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    /** 手持（主/副）是否“携带炯恻但未激活”：携带任一、且没有任何一把已激活，则为真。 */
    private static boolean showClosedEye(Player player) {
        ResourceLocation fate = NrSpecialEffectsRegistry.Fate.getId();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean carryMain = NRSpecialEffect.carriesSE(main, fate);
        boolean carryOff = NRSpecialEffect.carriesSE(off, fate);
        if (!carryMain && !carryOff) {
            return false;
        }
        boolean active = (carryMain && NRSpecialEffect.isAquaActive(main, fate))
                || (carryOff && NRSpecialEffect.isAquaActive(off, fate));
        return !active;
    }
}
