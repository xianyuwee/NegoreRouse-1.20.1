package net.xianyu.prinegorerouse.aquatime;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.prinegorerouse;

/**
 * 时间素系统的服务端事件处理：自动填充、登录/重生同步、死亡保留。
 * 全部逻辑只在服务端运行，不做任何客户端类引用。
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AquaTimeHandler {

    /** 累计 tick 计数，用于每 1200 tick（1 分钟）触发一次填充。 */
    private static long tickCounter = 0L;

    private AquaTimeHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !AquaTimeData.isEnabled()) {
            return;
        }
        tickCounter++;
        // 每 1200 tick（游戏内 1 分钟）执行一次
        if (tickCounter % 1200L != 0L) {
            return;
        }
        int basePerMinute = NRConfig.AQUA_FILL_PER_MINUTE.get();
        if (basePerMinute <= 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            int blades = AquaTimeData.countBlades(player);
            if (blades > 0) {
                AquaTimeData.add(player, (long) basePerMinute * blades);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (AquaTimeData.isEnabled()) {
            AquaTimeData.sync(event.getEntity());
        }
    }

    /** 死亡 / 末地返回都会触发 Clone，把旧玩家的时间素原样带到新玩家。 */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        long value = event.getOriginal().getPersistentData().getLong(AquaTimeData.KEY);
        event.getEntity().getPersistentData().putLong(AquaTimeData.KEY, value);
    }
}
