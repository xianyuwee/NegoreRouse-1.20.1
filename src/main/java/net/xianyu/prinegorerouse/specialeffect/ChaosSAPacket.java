package net.xianyu.prinegorerouse.specialeffect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：卡俄斯的 SA 操作。
 * <ul>
 *   <li>{@code select}：从存储的 SA 中点选一个装备为当前 SA。</li>
 *   <li>{@code toggle}：切换随机 SA 模式。</li>
 *   <li>{@code disable} / {@code enable}：禁用 / 重新启用某个已存储的 SA。</li>
 * </ul>
 * 只作用于当前主手的卡俄斯，合法性由 {@link ChaosHandler} 校验。
 */
public class ChaosSAPacket {
    private final String action;
    private final String saId;

    public ChaosSAPacket(String action, String saId) {
        this.action = action;
        this.saId = saId == null ? "" : saId;
    }

    public ChaosSAPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readUtf();
        this.saId = buffer.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(action);
        buf.writeUtf(saId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ItemStack chaos = player.getMainHandItem();
            if (!NRSpecialEffect.isChaosBlade(chaos)) {
                return;
            }
            if ("toggle".equals(action)) {
                ChaosHandler.toggleRandom(player, chaos);
                return;
            }
            // select / disable / enable 均需一个合法的 SA 标识，统一解析一次
            ResourceLocation rl = ResourceLocation.tryParse(saId);
            if (rl == null) {
                return;
            }
            switch (action) {
                case "select" -> ChaosHandler.selectSA(player, chaos, rl);
                case "disable" -> ChaosHandler.disableSA(player, chaos, rl);
                case "enable" -> ChaosHandler.enableSA(player, chaos, rl);
                default -> { }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
