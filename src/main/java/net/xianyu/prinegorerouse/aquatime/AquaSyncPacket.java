package net.xianyu.prinegorerouse.aquatime;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.xianyu.prinegorerouse.client.NRClientPacketHandler;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步玩家当前时间素数量，供客户端 HUD / 背包格子的流体计量表展示。
 */
public class AquaSyncPacket {
    private final long amount;
    /** 该玩家是否已触发 Empty 陷阱（吞噬界面不再显示 Empty 刀）。 */
    private final boolean emptyBanned;

    public AquaSyncPacket(long amount) {
        this(amount, false);
    }

    public AquaSyncPacket(long amount, boolean emptyBanned) {
        this.amount = amount;
        this.emptyBanned = emptyBanned;
    }

    public AquaSyncPacket(FriendlyByteBuf buffer) {
        this.amount = buffer.readLong();
        this.emptyBanned = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(this.amount);
        buf.writeBoolean(this.emptyBanned);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NRClientPacketHandler.handleAquaSync(this.amount, this.emptyBanned))
        );
        ctx.get().setPacketHandled(true);
    }
}
