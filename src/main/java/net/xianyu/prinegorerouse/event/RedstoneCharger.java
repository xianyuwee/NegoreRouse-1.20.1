package net.xianyu.prinegorerouse.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.item.BladeEnergyHelper;
import net.xianyu.prinegorerouse.prinegorerouse;

@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID)
public class RedstoneCharger {
    private static int tickCounter = 0;
    private static final int CHARGE_INTERVAL = 5; // 每5 tick充能一次

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return; // 仅在服务端执行

        tickCounter++;
        if (tickCounter < CHARGE_INTERVAL) return;
        tickCounter = 0;

        // 检查玩家主手或副手是否持有耀月
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack blade = null;
        if (BladeEnergyHelper.isAritemisSBlade(mainHand)) {
            blade = mainHand;
        } else if (BladeEnergyHelper.isAritemisSBlade(offHand)) {
            blade = offHand;
        } else {
            return; // 未持有耀月
        }

        int current = BladeEnergyHelper.getCurrentEnergy(blade);
        int max = BladeEnergyHelper.getMaxEnergy(blade);
        if (current >= max) return; // 已满

        // 尝试充能
        int totalProvided = 0;
        // 优先检查红石块
        int blockSlot = findItem(player, Items.REDSTONE_BLOCK);
        if (blockSlot != -1) {
            // 消耗十个红石块，提供90000 FE
            player.getInventory().removeItem(blockSlot, 10);
            totalProvided = 900000;
        } else {
            // 否则消耗红石粉，最多5个，每个1000 FE
            int powderCount = countItems(player, Items.REDSTONE);
            if (powderCount > 0) {
                int consume = Math.min(5, powderCount);
                removeItems(player, Items.REDSTONE, consume);
                totalProvided = consume * 1000;
            }
        }

        if (totalProvided > 0) {
            int newEnergy = Math.min(current + totalProvided, max);
            BladeEnergyHelper.setCurrentEnergy(blade, newEnergy);
            // 可选：播放充能音效或粒子效果（此处省略）
        }
    }

    // 查找物品槽位，返回第一个匹配的槽位索引，否则-1
    private static int findItem(Player player, net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    // 统计物品数量
    private static int countItems(Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // 移除指定数量的物品
    private static void removeItems(Player player, net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }
}