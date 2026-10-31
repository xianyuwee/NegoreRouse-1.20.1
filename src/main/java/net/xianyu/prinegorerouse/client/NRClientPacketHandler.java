package net.xianyu.prinegorerouse.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class NRClientPacketHandler {
    private NRClientPacketHandler() {
    }

    public static void handleBladeRecipeReload() {
        NRJeiRecipeReloader.forceReloadJeiRecipes();
    }

    public static void handleAquaSync(long amount, boolean emptyBanned) {
        ClientAquaState.set(amount, emptyBanned);
    }

    public static void handlePorgatoryRing(int entityId, int stacks, int maxStacks) {
        PorgatoryRingState.update(entityId, stacks, maxStacks);
    }
}
