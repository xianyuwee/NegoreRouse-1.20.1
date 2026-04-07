package net.xianyu.prinegorerouse.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.item.BladeEnergyHelper;
import net.xianyu.prinegorerouse.prinegorerouse;

@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, value = Dist.CLIENT)
public class EnergyTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!BladeEnergyHelper.isAritemisSBlade(stack)) return;

        int current = BladeEnergyHelper.getCurrentEnergy(stack);
        int max = BladeEnergyHelper.getMaxEnergy(stack);

        String currentFormatted = BladeEnergyHelper.formatEnergy(current);
        String maxFormatted = BladeEnergyHelper.formatEnergy(max); // 200M → "200.0M"? 可以优化为整数
        // 若总量为 200,000,000，格式化后为 "200.0M"，可改为 "200M"
        if (max == 200_000_000) maxFormatted = "200M"; // 手动处理

        Component energyLine = Component.literal("能量: " + currentFormatted + " / " + maxFormatted)
                .withStyle(ChatFormatting.GREEN);
        event.getToolTip().add(energyLine);
    }
}