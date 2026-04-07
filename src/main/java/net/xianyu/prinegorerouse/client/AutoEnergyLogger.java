package net.xianyu.prinegorerouse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.item.BladeEnergyHelper;
import net.xianyu.prinegorerouse.prinegorerouse;

@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, value = Dist.CLIENT)
public class AutoEnergyLogger {
    private static int tickCounter = 0;
    private static final int INTERVAL_TICKS = 100; // 5秒

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter < INTERVAL_TICKS) return;
        tickCounter = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        if (BladeEnergyHelper.isAritemisSBlade(mainHand)) {
            int current = BladeEnergyHelper.getCurrentEnergy(mainHand);
            int max = BladeEnergyHelper.getMaxEnergy(mainHand);
            prinegorerouse.LOGGER.info("[AutoEnergy] {} | FE: {}/{}",
                    mainHand.getDisplayName().getString(), current, max);
        } else {
            // 可选：输出非目标物品的信息，便于调试
            prinegorerouse.LOGGER.info("[AutoEnergy] Not Aritemis blade: {}", mainHand.getDisplayName().getString());
        }
    }
}