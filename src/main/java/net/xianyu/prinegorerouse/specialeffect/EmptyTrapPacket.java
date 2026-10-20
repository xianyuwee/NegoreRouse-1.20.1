package net.xianyu.prinegorerouse.specialeffect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.xianyu.prinegorerouse.specialeffect.NRSEs.Empty;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：在吞噬界面对带 Empty（虚无）特效的刀累计点击达到阈值，触发“虚无陷阱”惩罚。
 * <p>真正的属性扣除与向卡俄斯累加已吞时间素全部在服务端 {@link Empty#applyEmptyTrap} 完成，
 * 这里只上报被点击的槽位供服务端二次校验（该槽确为带 Empty 的刀、主手为卡俄斯）。</p>
 */
public class EmptyTrapPacket {
    private final int slot;

    public EmptyTrapPacket(int slot) {
        this.slot = slot;
    }

    public EmptyTrapPacket(FriendlyByteBuf buffer) {
        this.slot = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(slot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            Empty.applyEmptyTrap(player, slot);
        });
        ctx.get().setPacketHandled(true);
    }
}
