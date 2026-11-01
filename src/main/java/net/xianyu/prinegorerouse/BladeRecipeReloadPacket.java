package net.xianyu.prinegorerouse;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.xianyu.prinegorerouse.client.NRClientPacketHandler;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：通知“拔刀剑动态配方已重载”，请在客户端主线程强制刷新 JEI 配方索引。
 * 需在 {@code ClientboundUpdateRecipesPacket} 之后发送，确保此时客户端 RecipeManager 已是新数据。
 */
public class BladeRecipeReloadPacket {

    public BladeRecipeReloadPacket(FriendlyByteBuf buffer) {
        // 无载荷
    }

    public BladeRecipeReloadPacket() {
    }

    public void toBytes(FriendlyByteBuf buf) {
        // 无载荷
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NRClientPacketHandler.handleBladeRecipeReload())
        );
        ctx.get().setPacketHandled(true);
    }
}
