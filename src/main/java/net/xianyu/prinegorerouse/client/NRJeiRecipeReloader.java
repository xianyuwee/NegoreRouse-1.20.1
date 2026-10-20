package net.xianyu.prinegorerouse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.xianyu.prinegorerouse.prinegorerouse;

/**
 * 客户端强制刷新 JEI 配方索引。
 *
 * <p>背景：当前使用的 JEI 版本只在“世界加载”时构建配方索引——它的
 * {@code mezz.jei.forge.startup.StartEventObserver} 只有在同时观察到
 * {@link TagsUpdatedEvent} 与 {@link RecipesUpdatedEvent} 这对“必需事件”后才会调用 restart()
 * 重建；而会话中途我们发的 {@code ClientboundUpdateRecipesPacket} 只会触发 RecipesUpdatedEvent，
 * 缺少 TagsUpdatedEvent，于是 JEI 只是“收到事件但不重建”，导致不重进存档就看不到新配方。</p>
 *
 * <p>做法：在收到本模组的“配方重载”封包后，于客户端主线程补投这对事件，
 * 满足 JEI 的重建条件、逼其在不重进存档的情况下刷新。若该机制失效（JEI 内部类/流程变动而抛错），
 * 则捕获并向玩家提示重进存档。</p>
 */
public final class NRJeiRecipeReloader {

    private NRJeiRecipeReloader() {
    }

    // 必须在客户端主线程调用。
    public static void forceReloadJeiRecipes() {
        Minecraft mc = Minecraft.getInstance();
        try {
            // 不在世界中时无法刷新，静默跳过（原版合成已通过配方同步包更新）。
            if (mc.level == null) {
                return;
            }
            // 先补投 Recipes（若紧随配方同步包之后 JEI 已记录 Recipes，这里重复投递无副作用；
            // 若顺序颠倒也能确保 Recipes 被记录），再投 Tags，凑齐这对必需事件触发 JEI restart。
            MinecraftForge.EVENT_BUS.post(new RecipesUpdatedEvent(mc.level.getRecipeManager()));
            MinecraftForge.EVENT_BUS.post(new TagsUpdatedEvent(mc.level.registryAccess(), mc.isSingleplayer(), false));
        } catch (Throwable t) {
            // JEI 内部重建机制/类失效或重建过程抛错：无法自动刷新，提示玩家重进存档以更新 JEI 显示。
            prinegorerouse.LOGGER.warn("强制刷新 JEI 配方索引失败，需要玩家重新进入世界后才会看到最新合成表。", t);
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal("§e[尼格洛兹] 配方已更新，但自动刷新 JEI 失败，请重新进入世界以更新合成表显示。"), false);
            }
        }
    }
}
