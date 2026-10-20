package net.xianyu.prinegorerouse.client;

import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.specialeffect.ChaosHandler;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

import java.util.List;

/**
 * 幻影刃模型预热。
 * <p>绝对神力的幻影刃用被吞刀（或卡俄斯本体）的 OBJ 建模渲染，{@code AbsolutePhantomRenderer}
 * 每帧调 {@link BladeModelManager#getModel(ResourceLocation)}；某模型<b>首次</b>遇到时会在渲染帧里
 * 同步解析 OBJ 并上传，落到战斗中就是“挨打那一下偶发卡一下”。这里在客户端手持卡俄斯时，提前把它
 * NBT 里“被吞模型池”的每个模型都解析一次、常驻缓存，把解析开销从战斗帧挪到手持/加载期。</p>
 * <p>{@code getModel} 内部按 RL 缓存 {@code WavefrontObject}，重复调用只是查表，成本极低；再叠加周期
 * 节流，正常游玩几乎无感。</p>
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PhantomModelPrewarmer {

    /** 复查周期（tick）：预热后命中缓存即空转，无需每帧扫描。 */
    private static final int PERIOD = 20;

    private static int cooldown = 0;

    private PhantomModelPrewarmer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            cooldown = 0;
            return;
        }
        if (cooldown-- > 0) {
            return;
        }
        cooldown = PERIOD;
        prewarmHand(mc.player.getMainHandItem());
        prewarmHand(mc.player.getOffhandItem());
    }

    private static void prewarmHand(ItemStack stack) {
        if (stack.isEmpty() || !NRSpecialEffect.isChaosBlade(stack)) {
            return;
        }
        List<String> models = ChaosHandler.poolModelStrings(stack);
        if (models.isEmpty()) {
            return;
        }
        BladeModelManager manager = BladeModelManager.getInstance();
        for (String s : models) {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) {
                manager.getModel(rl); // 首遇即解析并常驻缓存；已缓存则直接命中
            }
        }
    }
}
