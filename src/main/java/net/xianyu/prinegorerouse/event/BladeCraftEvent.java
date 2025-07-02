package net.xianyu.prinegorerouse.event;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = "prinegorerouse", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BladeCraftEvent {
    // 自定义 NBT 键
    private static final String OWNER_KEY = "prinegorerouse_owner";
    private static final String BLADE_DATA_KEY = "SlashBlade"; // 重锋的主要 NBT 键
    private static final String DAMAGE_KEY = "baseAttackDamage"; // 重锋的伤害值键
    private static final String MODEL_KEY = "model"; // 重锋的模型键，用于识别刀具

    private static final Component HIDDEN_DAMAGE = Component.literal("???").withStyle(ChatFormatting.DARK_GRAY);

    // 特殊刀具的模型名称
    private static final String[] SPECIAL_BLADES = {
            "prinegorerouse:oracle",
            "prinegorerouse:empty",
            "prinegorerouse:clear",
            "prinegorerouse:eternity"
    };

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        // 跳过非特殊刀具
        if (!isSpecialBlade(stack)) return;

        List<Component> tooltip = event.getToolTip();

        // 查找并替换伤害显示行
        for (int i = 0; i < tooltip.size(); i++) {
            Component comp = tooltip.get(i);
            if (comp.getString().contains("Attack Damage:")) {
                tooltip.set(i, Component.literal("Attack Damage: ")
                        .append(getDamageDisplay(stack, player))
                        .withStyle(comp.getStyle()));
                break;
            }
        }
    }

    private static Component getDamageDisplay(ItemStack stack, @Nullable Player player) {
        CompoundTag nbt = stack.getTag();

        // 检查是否已合成（有拥有者数据）
        if (nbt == null || !nbt.contains(OWNER_KEY)) {
            return HIDDEN_DAMAGE;
        }

        // 如果没有玩家上下文（如物品展示框）
        if (player == null) return HIDDEN_DAMAGE;

        // 检查是否为拥有者
        UUID ownerId = nbt.getUUID(OWNER_KEY);
        return ownerId.equals(player.getUUID()) ?
                Component.literal(String.format("%.1f", getBladeDamage(stack))) :
                HIDDEN_DAMAGE;
    }

    // 直接从 NBT 获取伤害值（适配重锋）
    private static float getBladeDamage(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null) return 0.0f;

        // 检查重锋专用 NBT
        if (nbt.contains(BLADE_DATA_KEY)) {
            CompoundTag bladeNbt = nbt.getCompound(BLADE_DATA_KEY);

            // 重锋使用 "baseAttackDamage" 键存储伤害值
            if (bladeNbt.contains(DAMAGE_KEY)) {
                return bladeNbt.getFloat(DAMAGE_KEY);
            }
        }

        return 0.0f;
    }

    // 检查是否为特殊刀具（适配重锋）
    public static boolean isSpecialBlade(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null) return false;

        // 检查重锋专用 NBT
        if (nbt.contains(BLADE_DATA_KEY)) {
            CompoundTag bladeNbt = nbt.getCompound(BLADE_DATA_KEY);

            // 重锋使用 "model" 键存储刀具模型
            if (bladeNbt.contains(MODEL_KEY)) {
                String model = bladeNbt.getString(MODEL_KEY);
                for (String specialBlade : SPECIAL_BLADES) {
                    if (specialBlade.equals(model)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // 合成时设置拥有者
    public static void setBladeOwner(ItemStack blade, Player player) {
        if (player == null) return;

        CompoundTag nbt = blade.getOrCreateTag();
        nbt.putUUID(OWNER_KEY, player.getUUID());

        // 确保重锋数据存在
        if (!nbt.contains(BLADE_DATA_KEY)) {
            nbt.put(BLADE_DATA_KEY, new CompoundTag());
        }
    }
}
