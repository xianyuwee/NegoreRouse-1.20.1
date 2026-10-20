package net.xianyu.prinegorerouse.specialeffect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xianyu.prinegorerouse.client.PorgatoryRingState;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步某目标的禁锢（Porgatory）叠层，用于在其脚下绘制禁锢法环。
 * <p>禁锢的削弱状态只存于服务端静态表，客户端无从得知层数，故叠层变化时向“能看到该实体的客户端”
 * 广播本包。{@code stacks <= 0} 表示削弱已到期/移除，客户端据此让法环淡出并清除本地记录。</p>
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
        // 仅客户端执行：写入本地叠层缓存，交由渲染与插值处理。
        ctx.get().enqueueWork(() -> PorgatoryRingState.update(this.entityId, this.stacks, this.maxStacks));
        ctx.get().setPacketHandled(true);
    }
}
