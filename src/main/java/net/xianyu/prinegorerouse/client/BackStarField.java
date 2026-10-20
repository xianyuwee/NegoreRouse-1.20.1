package net.xianyu.prinegorerouse.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.client.back.StarAnchorStore;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrParticlesRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSEs.Back;
import org.joml.Vector3f;

import java.util.Arrays;

/**
 * 回溯（back）星星闪烁的客户端调度器：不产生实体，只按层数在刀镡附近投放粒子。
 * <p>每个“槽位”是一盏独立的星灯：亮一次 {@link Back#STAR_LIFETIME} tick，熄灭后歇一小段时间，
 * 再换一个新的随机位置重新点亮。同时点亮的槽位数 = min(回溯层数, {@link Back#MAX_RENDER})，
 * 因此最多渲染 10 个，而层数本身可以累积到 20。</p>
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BackStarField {

    // ===== 微调旋钮 =====
    private static final int RESTART_MIN = 40;  // 熄灭后最少等待（tick）再换位置重亮
    private static final int RESTART_RANGE = 70; // 额外的随机等待区间（tick）
    private static final double GUARD_FORWARD = 0.55; // 刀镡沿视线的前向距离
    private static final double GUARD_SIDE = 0.30;   // 刀镡横向偏移（主手斜向左、副手镜像向右）
    private static final double GUARD_UP = -0.4;     // 刀镡相对视线的上移（拔刀剑刀身斜向左上方）
    private static final double JITTER = 0.22;       // 随机位置半径

    /** 正在闪烁的剩余 tick；0 表示该槽位当前空闲。 */
    private static final int[] LIT = new int[Back.MAX_RENDER];
    /** 空闲槽位重新点亮的倒计时。 */
    private static final int[] COOLDOWN = new int[Back.MAX_RENDER];
    /** 首轮是否已把各槽位的倒计时错开（避免所有星同时闪）。 */
    private static boolean staggered = false;

    private BackStarField() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }
        int active = Math.min(Back.layers(mc.player), Back.MAX_RENDER);
        if (active <= 0) {
            reset();
            return;
        }
        RandomSource random = mc.level.random;
        if (!staggered) {
            for (int i = 0; i < Back.MAX_RENDER; i++) {
                COOLDOWN[i] = i * 5 + random.nextInt(10);
            }
            staggered = true;
        }
        for (int i = 0; i < Back.MAX_RENDER; i++) {
            if (i >= active) {
                LIT[i] = 0;
                COOLDOWN[i] = 0;
                continue;
            }
            if (LIT[i] > 0) {
                if (--LIT[i] == 0) {
                    COOLDOWN[i] = RESTART_MIN + random.nextInt(RESTART_RANGE);
                }
                continue;
            }
            if (COOLDOWN[i] > 0) {
                COOLDOWN[i]--;
                continue;
            }
            spawnOne(mc, random);
            LIT[i] = Back.STAR_LIFETIME;
        }
    }

    /** 在刀镡附近取一个随机点投放一颗星。 */
    private static void spawnOne(Minecraft mc, RandomSource random) {
        Player player = mc.player;
        Vec3 base;
        Vec3 look;
        if (mc.options.getCameraType().isFirstPerson()) {
            // 第一人称下手持刀是跟着相机画的，用相机基向量才贴得近
            Camera camera = mc.gameRenderer.getMainCamera();
            Vector3f dir = camera.getLookVector();
            base = camera.getPosition();
            look = new Vec3(dir.x(), dir.y(), dir.z());
        } else {
            base = player.getEyePosition(1.0F);
            look = player.getLookAngle();
        }
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        // 优先按当前刀的模型刀镡点算视线偏移（本 mod 有分组的刀）；未命中则回退固定偏移。
        double forward;
        double sideBase; // 主手约定：负=向刀身一侧（左），副手再镜像
        double up;
        double[] off = StarAnchorStore.offsetFor(Back.activeBlade(player), mc.level.registryAccess());
        if (off != null) {
            forward = off[0];
            sideBase = off[1];
            up = off[2];
        } else {
            forward = GUARD_FORWARD;
            sideBase = -GUARD_SIDE;
            up = GUARD_UP;
        }
        double side = Back.isActiveInOffhand(player) ? -sideBase : sideBase;
        Vec3 guard = base.add(look.scale(forward)).add(right.scale(side)).add(0.0, up, 0.0);
        Vec3 offset = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5)
                .scale(JITTER * 2.0);
        Vec3 pos = guard.add(offset);
        mc.level.addParticle(NrParticlesRegistry.BACK_STAR.get(), pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    private static void reset() {
        Arrays.fill(LIT, 0);
        Arrays.fill(COOLDOWN, 0);
        staggered = false;
    }
}
