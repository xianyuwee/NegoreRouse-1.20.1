package net.xianyu.prinegorerouse.specialeffect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：在吞噬界面点击背包中某槽位的刀，喂给当前主手的卡俄斯吞噬。
 * <p>只在服务端执行，合法性（主手是卡俄斯、目标确为刀且未吞过）全部由 {@link ChaosHandler} 校验。</p>
 */
public class ChaosDevourPacket {
    private final int slot;

    public ChaosDevourPacket(int slot) {
        this.slot = slot;
    }

    public ChaosDevourPacket(FriendlyByteBuf buffer) {
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
            ItemStack chaos = player.getMainHandItem();
            if (!NRSpecialEffect.isChaosBlade(chaos)) {
                return;
            }
            Inventory inv = player.getInventory();
            if (slot < 0 || slot >= inv.getContainerSize()) {
                return;
            }
            ItemStack target = inv.getItem(slot);
            if (target.isEmpty() || NRSpecialEffect.isChaosBlade(target)) {
                return;
            }
            if (ChaosHandler.devour(player, chaos, target)) {
                inv.setItem(slot, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("se.prinegorerouse.hint.devoured",
                        target.getHoverName()), true);
            } else {
                player.displayClientMessage(Component.translatable("se.prinegorerouse.hint.devour_fail"), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
