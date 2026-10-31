package net.xianyu.prinegorerouse.specialeffect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.xianyu.prinegorerouse.client.NRClientPacketHandler;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步某目标的禁锢（Porgatory）叠层，用于在其脚下绘制禁锢法环。
 */
public class PorgatoryRingPacket {
    private final int entityId;
    private final int stacks;
    private final int maxStacks;

    public PorgatoryRingPacket(int entityId, int stacks, int maxStacks) {
        this.entityId = entityId;
        this.stacks = stacks;
        this.maxStacks = maxStacks;
    }

    public PorgatoryRingPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.stacks = buffer.readVarInt();
        this.maxStacks = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(stacks);
        buf.writeVarInt(maxStacks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NRClientPacketHandler.handlePorgatoryRing(this.entityId, this.stacks, this.maxStacks))
        );
        ctx.get().setPacketHandled(true);
    }
}
