package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.config.AquaSEMode;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

/**
 * 特效时间素“持续消耗（会耗尽）”型的服务端维护（按单把刀判定）：
 * <ul>
 *   <li>{@link AquaSEMode#PER_SECOND}：每 20 tick（1 秒）从时间素池扣一次成本，不足即自动解除。</li>
 *   <li>{@link AquaSEMode#PER_HITS}：每命中设定次数扣一次成本。</li>
 *   <li>{@link AquaSEMode#PER_SA}：每次释放 SA 扣一次成本。</li>
 * </ul>
 * {@link AquaSEMode#ONE_TIME} 无持续消耗，激活时已一次性扣除，不在此处理。
 * 创造模式玩家免消耗（不会被扣除，也不会因池空而解除）。全部逻辑仅在服务端运行。
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AquaSEHandler {

    private static final String HITS_KEY = "prinegorerouse_se_hits";

    private AquaSEHandler() {
    }

    @SubscribeEvent
    public static void onUpdate(SlashBladeEvent.UpdateEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.isSelected()) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        ISlashBladeState state = event.getSlashBladeState();
        ItemStack blade = event.getBlade();
        for (ResourceLocation se : state.getSpecialEffects()) {
            if (isDrainActive(player, blade, se, AquaSEMode.PER_SECOND)) {
                drain(player, blade, se);
            }
        }
    }

    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity user = event.getUser();
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        ISlashBladeState state = event.getSlashBladeState();
        ItemStack blade = event.getBlade();
        for (ResourceLocation se : state.getSpecialEffects()) {
            if (!isDrainActive(player, blade, se, AquaSEMode.PER_HITS)) {
                continue;
            }
            int need = Math.max(1, NRConfig.seHits(se.toString()));
            int hits = incrementCounter(player.getPersistentData(), HITS_KEY, se);
            if (hits >= need) {
                resetCounter(player.getPersistentData(), HITS_KEY, se);
                drain(player, blade, se);
            }
        }
    }

    @SubscribeEvent
    public static void onDoSlash(SlashBladeEvent.DoSlashEvent event) {
        LivingEntity user = event.getUser();
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        ISlashBladeState state = event.getSlashBladeState();
        ItemStack blade = event.getBlade();
        for (ResourceLocation se : state.getSpecialEffects()) {
            if (isDrainActive(player, blade, se, AquaSEMode.PER_SA)) {
                drain(player, blade, se);
            }
        }
    }

    /** 该特效是否为“本模组 + 该消耗模式 + 这把刀上已激活”。 */
    private static boolean isDrainActive(ServerPlayer player, ItemStack blade, ResourceLocation se, AquaSEMode expected) {
        if (!prinegorerouse.MOD_ID.equals(se.getNamespace())) {
            return false;
        }
        if (NRConfig.seMode(se.toString()) != expected) {
            return false;
        }
        return NRSpecialEffect.isAquaActive(blade, se);
    }

    /** 扣除一次成本；创造模式免扣；池不足则自动解除该刀上的激活（不返还）。 */
    private static void drain(ServerPlayer player, ItemStack blade, ResourceLocation se) {
        if (NRSpecialEffect.isFreeActivate(player)) {
            return;
        }
        long cost = NRConfig.seCost(se.toString());
        if (cost <= 0L) {
            return;
        }
        if (AquaTimeData.get(player) >= cost) {
            // 回溯自身的持续消耗不参与反转，否则“消耗→增加”会与自己无限循环
            boolean backAware = !se.equals(NrSpecialEffectsRegistry.Back.getId());
            AquaTimeData.add(player, -cost, true, backAware);
        } else {
            NRSpecialEffect.clearActive(blade, se);
        }
    }

    private static int incrementCounter(CompoundTag data, String rootKey, ResourceLocation se) {
        CompoundTag root = data.getCompound(rootKey);
        int v = root.getInt(se.toString()) + 1;
        root.putInt(se.toString(), v);
        data.put(rootKey, root);
        return v;
    }

    private static void resetCounter(CompoundTag data, String rootKey, ResourceLocation se) {
        CompoundTag root = data.getCompound(rootKey);
        root.remove(se.toString());
        data.put(rootKey, root);
    }
}
