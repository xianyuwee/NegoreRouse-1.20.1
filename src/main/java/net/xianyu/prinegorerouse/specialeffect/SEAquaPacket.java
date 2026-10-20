package net.xianyu.prinegorerouse.specialeffect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：玩家在特效激活界面点击“激活 / 解除”某特效。
 * <p>只在服务端执行 handle，因此不引用任何客户端类。合法性（背包内确有携带该特效的刀、时间素是否足够）
 * 全部交由服务端 {@link NRSpecialEffect#tryActivate} 校验，客户端无法伪造。</p>
 */
public class SEAquaPacket {
    private final String seId;
    private final boolean activate;

    public SEAquaPacket(String seId, boolean activate) {
        this.seId = seId;
        this.activate = activate;
    }

    public SEAquaPacket(FriendlyByteBuf buffer) {
        this.seId = buffer.readUtf();
        this.activate = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(seId);
        buf.writeBoolean(activate);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ResourceLocation id = ResourceLocation.tryParse(seId);
            // 仅允许操作本模组的特效
            if (id == null || !prinegorerouse.MOD_ID.equals(id.getNamespace())) {
                return;
            }
            // 按单把刀判定：作用于当前主手所持的刀
            ItemStack blade = player.getMainHandItem();
            if (!NRSpecialEffect.carriesSE(blade, id)) {
                player.displayClientMessage(Component.translatable("se.prinegorerouse.hint.no_blade"), true);
                return;
            }
            if (activate) {
                boolean ok = ChaosHandler.isGainedSE(blade, id)
                        ? ChaosHandler.tryActivateGainedSE(player, blade, id)
                        : NRSpecialEffect.tryActivate(player, blade, id);
                if (!ok) {
                    player.displayClientMessage(Component.translatable("se.prinegorerouse.hint.insufficient"), true);
                    return;
                }
                player.displayClientMessage(Component.translatable("se.prinegorerouse.hint.activated",
                        SpecialEffect.getDescription(id)), true);
            } else {
                // CHAOS 本体 / EMPTY 永久激活不可解除；deactivate 内部也已拦截
                if (NRSpecialEffect.isChaosSE(id) || NRSpecialEffect.isEmptySE(id)) {
                    return;
                }
                // 解除并按实际支付额返还时间素
                NRSpecialEffect.deactivate(player, blade, id);
                player.displayClientMessage(Component.translatable("se.prinegorerouse.hint.deactivated",
                        SpecialEffect.getDescription(id)), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
