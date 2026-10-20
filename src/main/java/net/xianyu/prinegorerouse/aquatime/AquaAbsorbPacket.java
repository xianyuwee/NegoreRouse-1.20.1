package net.xianyu.prinegorerouse.aquatime;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：玩家在背包界面点击时间素格子，请求把当前鼠标光标拿着的物品存入并转化为时间素。
 * <p>只有服务端执行 handle，因此不引用任何客户端类。光标物品由服务端直接清空，
 * 客户端会在下一 tick 的 {@code broadcastChanges} 中收到光标变化而自动清除。</p>
 */
public class AquaAbsorbPacket {

    public AquaAbsorbPacket(FriendlyByteBuf buffer) {
    }

    public AquaAbsorbPacket() {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !AquaTimeData.isEnabled()) {
                return;
            }
            AbstractContainerMenu menu = player.containerMenu;
            if (menu == null) {
                return;
            }
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) {
                return;
            }
            // 白名单校验：非白名单物品一律拒绝存入
            if (!AquaTimeData.isWhitelisted(carried)) {
                player.displayClientMessage(Component.literal("§c该物品无法转化为时间素"), true);
                return;
            }
            int count = carried.getCount();
            long gain = (long) count * AquaTimeData.valuePerItem();
            // 清空光标（服务端）；下一 tick 自动同步给客户端
            menu.setCarried(ItemStack.EMPTY);
            AquaTimeData.add(player, gain);
        });
        ctx.get().setPacketHandled(true);
    }
}
