package net.xianyu.prinegorerouse;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkPacketHandler {
    public static SimpleChannel INSTANCE;
    public static final String PROTOCOL_VERSION = "1.0";
    private static int ID = 0;

    public static int nextID() {
        return ID++;
    }

    public static void registerMessage() {
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(prinegorerouse.MOD_ID, "network"),
                () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

        INSTANCE.messageBuilder(PowerSwitchPacket.class, nextID(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PowerSwitchPacket::toBytes).decoder(PowerSwitchPacket::new)
                .consumerNetworkThread(PowerSwitchPacket::handle).add();

        // 服务端 -> 客户端：拔刀剑动态配方重载后，通知客户端强制刷新 JEI 配方索引。
        INSTANCE.messageBuilder(BladeRecipeReloadPacket.class, nextID(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BladeRecipeReloadPacket::toBytes).decoder(BladeRecipeReloadPacket::new)
                .consumerNetworkThread(BladeRecipeReloadPacket::handle).add();

        // 服务端 -> 客户端：同步玩家的时间素数量（供 HUD / 背包格子流体计量表展示）。
        INSTANCE.messageBuilder(net.xianyu.prinegorerouse.aquatime.AquaSyncPacket.class, nextID(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(net.xianyu.prinegorerouse.aquatime.AquaSyncPacket::toBytes)
                .decoder(net.xianyu.prinegorerouse.aquatime.AquaSyncPacket::new)
                .consumerNetworkThread(net.xianyu.prinegorerouse.aquatime.AquaSyncPacket::handle).add();

        // 客户端 -> 服务端：点击时间素格子，存入光标物品并转化为时间素。
        INSTANCE.messageBuilder(net.xianyu.prinegorerouse.aquatime.AquaAbsorbPacket.class, nextID(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(net.xianyu.prinegorerouse.aquatime.AquaAbsorbPacket::toBytes)
                .decoder(net.xianyu.prinegorerouse.aquatime.AquaAbsorbPacket::new)
                .consumerNetworkThread(net.xianyu.prinegorerouse.aquatime.AquaAbsorbPacket::handle).add();

        // 客户端 -> 服务端：在激活界面点击“激活 / 解除”某特效。
        INSTANCE.messageBuilder(net.xianyu.prinegorerouse.specialeffect.SEAquaPacket.class, nextID(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(net.xianyu.prinegorerouse.specialeffect.SEAquaPacket::toBytes)
                .decoder(net.xianyu.prinegorerouse.specialeffect.SEAquaPacket::new)
                .consumerNetworkThread(net.xianyu.prinegorerouse.specialeffect.SEAquaPacket::handle).add();

        // 客户端 -> 服务端：卡俄斯吞噬背包中某槽位的刀。
        INSTANCE.messageBuilder(net.xianyu.prinegorerouse.specialeffect.ChaosDevourPacket.class, nextID(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(net.xianyu.prinegorerouse.specialeffect.ChaosDevourPacket::toBytes)
                .decoder(net.xianyu.prinegorerouse.specialeffect.ChaosDevourPacket::new)
                .consumerNetworkThread(net.xianyu.prinegorerouse.specialeffect.ChaosDevourPacket::handle).add();

        // 客户端 -> 服务端：卡俄斯的 SA 装备 / 随机模式切换。
        INSTANCE.messageBuilder(net.xianyu.prinegorerouse.specialeffect.ChaosSAPacket.class, nextID(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(net.xianyu.prinegorerouse.specialeffect.ChaosSAPacket::toBytes)
                .decoder(net.xianyu.prinegorerouse.specialeffect.ChaosSAPacket::new)
                .consumerNetworkThread(net.xianyu.prinegorerouse.specialeffect.ChaosSAPacket::handle).add();

        // 客户端 -> 服务端：对带 Empty 的刀多次点击触发“虚无陷阱”惩罚。
        INSTANCE.messageBuilder(net.xianyu.prinegorerouse.specialeffect.EmptyTrapPacket.class, nextID(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(net.xianyu.prinegorerouse.specialeffect.EmptyTrapPacket::toBytes)
                .decoder(net.xianyu.prinegorerouse.specialeffect.EmptyTrapPacket::new)
                .consumerNetworkThread(net.xianyu.prinegorerouse.specialeffect.EmptyTrapPacket::handle).add();

        // 服务端 -> 客户端：同步某目标的禁锢（Porgatory）叠层，用于脚下法环渲染。
        INSTANCE.messageBuilder(net.xianyu.prinegorerouse.specialeffect.PorgatoryRingPacket.class, nextID(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(net.xianyu.prinegorerouse.specialeffect.PorgatoryRingPacket::toBytes)
                .decoder(net.xianyu.prinegorerouse.specialeffect.PorgatoryRingPacket::new)
                .consumerNetworkThread(net.xianyu.prinegorerouse.specialeffect.PorgatoryRingPacket::handle).add();
    }

    public static void sendToClientPlayer(Object message, Player player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), message);
    }

    /** 服务端 -> 所有正在追踪该实体的客户端（含实体自身为玩家时）：用于实体附着型特效的同步。 */
    public static void sendToTrackingEntity(Object message, Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }
}
