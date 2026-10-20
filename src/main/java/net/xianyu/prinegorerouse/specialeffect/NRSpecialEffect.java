package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.config.AquaSEMode;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.item.ModItems;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

/**
 * 本模组特效（SpecialEffect）的公共基类：以“消耗时间素激活”取代原本的经验等级门槛。
 * <p>激活状态<b>按单把刀判定</b>——直接写在该刀 ItemStack 的 NBT 里，因此会随刀同步到客户端，
 * tooltip 与激活界面都能读取；成本与消耗模式来自 {@link NRConfig}（按特效 id 独立配置）；
 * 持续型消耗由 {@link AquaSEHandler} 在服务端按刀维护，池耗尽自动解除（不返还）。</p>
 * <p>解除激活会<b>返还一次性买断实际支付的时间素</b>；创造模式玩家激活不消耗时间素（返还额记为 0，
 * 不会凭空产生时间素）。</p>
 * <p>子类只需把原先的 {@code SpecialEffect.isEffective(..., experienceLevel)} 判定替换为
 * {@link #isAquaActive(ItemStack, ResourceLocation)} 即可。</p>
 */
@Mod.EventBusSubscriber
public class NRSpecialEffect extends SpecialEffect {

    /** 刀上“激活标志”存储的标签键（ItemStack 根 tag 下的子 CompoundTag）。 */
    public static final String SE_TAG = "prinegorerouse_se_active";
    /** 刀上“已实际支付金额”存储的标签键，用于解除激活时精确返还。 */
    public static final String SE_PAID_TAG = "prinegorerouse_se_paid";

    public NRSpecialEffect(int requestLevel) {
        super(requestLevel);
    }

    public NRSpecialEffect(int requestLevel, boolean isCopiable, boolean isRemovable) {
        super(requestLevel, isCopiable, isRemovable);
    }

    // ===== 按单把刀判定（当前采用） =====

    /** 该特效在这把刀上是否处于激活状态（客户端 / 服务端都直接读刀 NBT）。 */
    public static boolean isAquaActive(ItemStack blade, ResourceLocation seId) {
        if (blade == null || blade.isEmpty() || seId == null) {
            return false;
        }
        // CHAOS 位于卡俄斯上时：恒为激活，不可关闭
        if (isChaosSE(seId) && isChaosBlade(blade)) {
            return true;
        }
        // EMPTY（虚无）：永久激活，只要该刀携带虚无即视为已激活，无需玩家手动激活
        if (isEmptySE(seId)) {
            return carriesSE(blade, seId);
        }
        return blade.getOrCreateTag().getCompound(SE_TAG).getBoolean(seId.toString());
    }

    /** 是否为 CHAOS 特效本体。 */
    public static boolean isChaosSE(ResourceLocation seId) {
        return seId != null && seId.equals(NrSpecialEffectsRegistry.CHAOS.getId());
    }

    /** 是否为 EMPTY（虚无）特效本体——永久激活、无需玩家激活、不可关闭。 */
    public static boolean isEmptySE(ResourceLocation seId) {
        return seId != null && seId.equals(NrSpecialEffectsRegistry.Empty.getId());
    }

    /** 是否为本 mod 的卡俄斯（CHAOS_BLADE）。 */
    public static boolean isChaosBlade(ItemStack blade) {
        return blade != null && !blade.isEmpty() && blade.is(ModItems.CHAOS_BLADE.get());
    }

    /** 取该特效配置的消耗模式。 */
    public static AquaSEMode modeOf(ResourceLocation seId) {
        return NRConfig.seMode(seId.toString());
    }

    /** 取该特效配置的时间素成本。 */
    public static long costOf(ResourceLocation seId) {
        return NRConfig.seCost(seId.toString());
    }

    /** 该刀该特效实际已支付的时间素（解除时返还额）；未激活/无记录为 0。 */
    public static long paidOf(ItemStack blade, ResourceLocation seId) {
        return readPaid(blade, seId);
    }

    /** 是否为免消耗激活场景（创造模式玩家）。 */
    public static boolean isFreeActivate(Player player) {
        return player != null && player.getAbilities() != null && player.getAbilities().instabuild;
    }

    /** 玩家背包（含主手 / 副手）里是否存在携带该特效的拔刀剑。 */
    public static boolean hasSEOnAnyBlade(Player player, ResourceLocation seId) {
        if (player == null) {
            return false;
        }
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            if (carriesSE(stack, seId)) {
                return true;
            }
        }
        for (ItemStack stack : inv.offhand) {
            if (carriesSE(stack, seId)) {
                return true;
            }
        }
        return false;
    }

    /** 指定这把刀是否携带该特效。 */
    public static boolean carriesSE(ItemStack stack, ResourceLocation seId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(seId))
                .orElse(false);
    }

    /**
     * 服务端尝试用时间素激活某把刀上的某特效。
     * <p>创造模式免消耗；一次性买断扣除一次成本并记录支付额，持续型仅做起步校验。
     *
     * @return 激活成功返回 true；刀未携带该特效或池不足返回 false。
     */
    public static boolean tryActivate(Player player, ItemStack blade, ResourceLocation seId) {
        return activateWithCost(player, blade, seId, costOf(seId));
    }

    /**
     * 以给定成本尝试激活某把刀上的某特效（一次性买断语义）。CHAOS 获得的 SE 会传入自定义费用。
     * <p>创造模式免消耗；成功时扣除成本并记录实际支付额（供解除时返还）。
     *
     * @return true 成功或已激活；false 表示刀未携带该特效或池不足。
     */
    public static boolean activateWithCost(Player player, ItemStack blade, ResourceLocation seId, long cost) {
        if (!carriesSE(blade, seId)) {
            return false;
        }
        if (isAquaActive(blade, seId)) {
            return true;
        }
        // 创造模式：不消耗时间素，支付额记 0
        if (isFreeActivate(player)) {
            writeActive(blade, seId, true, 0L);
            return true;
        }
        if (AquaTimeData.get(player) < cost) {
            return false;
        }
        // 扣除成本（chaosAware=false，避免反向吞噬钩子干扰扣费）
        AquaTimeData.add(player, -cost, false);
        writeActive(blade, seId, true, cost);
        return true;
    }

    /**
     * 解除某把刀上某特效的激活，并<b>返还一次性买断实际支付的时间素</b>。
     * 用于玩家主动解除；池耗尽的自动解除走 {@link #clearActive} 不返还。
     */
    public static void deactivate(Player player, ItemStack blade, ResourceLocation seId) {
        if (isChaosSE(seId) || isEmptySE(seId)) {
            return; // CHAOS 本体 / EMPTY 永久激活，不可解除
        }
        long paid = readPaid(blade, seId);
        if (paid > 0L && player != null) {
            // 返还走 chaosAware=false，避免刚返还又被卡俄斯吞掉
            AquaTimeData.add(player, paid, false);
        }
        clearActive(blade, seId);
    }

    /** 仅清除激活标志与支付记录，不返还（池耗尽自动解除使用）。 */
    public static void clearActive(ItemStack blade, ResourceLocation seId) {
        if (isChaosSE(seId) || isEmptySE(seId)) {
            return; // CHAOS 本体 / EMPTY 永久激活，不可清除
        }
        if (blade == null || blade.isEmpty()) {
            return;
        }
        CompoundTag root = blade.getOrCreateTag();
        CompoundTag active = root.getCompound(SE_TAG);
        active.remove(seId.toString());
        root.put(SE_TAG, active);
        CompoundTag paid = root.getCompound(SE_PAID_TAG);
        paid.remove(seId.toString());
        root.put(SE_PAID_TAG, paid);
    }

    /** 写入激活标志与实际支付金额（仅服务端调用）。 */
    private static void writeActive(ItemStack blade, ResourceLocation seId, boolean active, long paid) {
        CompoundTag root = blade.getOrCreateTag();
        CompoundTag tag = root.getCompound(SE_TAG);
        if (active) {
            tag.putBoolean(seId.toString(), true);
        } else {
            tag.remove(seId.toString());
        }
        root.put(SE_TAG, tag);
        CompoundTag paidTag = root.getCompound(SE_PAID_TAG);
        if (paid > 0L) {
            paidTag.putLong(seId.toString(), paid);
        } else {
            paidTag.remove(seId.toString());
        }
        root.put(SE_PAID_TAG, paidTag);
    }

    /** 读取该刀该特效实际已支付的金额（用于解除返还）。 */
    private static long readPaid(ItemStack blade, ResourceLocation seId) {
        if (blade == null || blade.isEmpty()) {
            return 0L;
        }
        return blade.getOrCreateTag().getCompound(SE_PAID_TAG).getLong(seId.toString());
    }
}
