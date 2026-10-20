package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

/**
 * 疏影（Shadoon）。
 * <p>纯增伤 SE：玩家手持“携带疏影且已激活”的刀进行<b>近战命中</b>时，依<b>连续月龄</b>追加固定伤害——
 * 月亮越满增伤越低、越新（接近无月）增伤越高，增伤在 1~5 之间随月龄连续线性变化。</p>
 * <p>不取离散的 {@link Level#getMoonPhase()}（0~7、每日整点跳变），而用连续月龄
 * {@code age = floorMod(dayTime / 24000.0, 8)}（含小数、盈亏平滑过渡）；以 {@code d = min(age, 8 - age)}
 *（0~4）表示“距满月有多远”（age=0 满月、age=4 新月），追加伤害
 * {@code = MIN_BONUS + d*(MAX_BONUS-MIN_BONUS)/4}：满月 1、新月 5，随月龄连续线性变化。</p>
 * <p>沿用 {@link Porgatory} 的近战结算范式：仅服务端、仅玩家自身近战（{@code direct==entity==player}，
 * 排除投射物 / 幻影刃 / 其它追加伤害的递归），主手优先其次副手判激活。</p>
 */
@Mod.EventBusSubscriber
public class Shadoon extends NRSpecialEffect {

    public Shadoon() {
        super(1, false, false);
    }

    /** 增伤下限（满月）。 */
    private static final int MIN_BONUS = 1;
    /** 增伤上限（新月）。 */
    private static final int MAX_BONUS = 5;
    /** 月相相位总数（0~7）。 */
    private static final int MOON_PHASES = 8;

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) {
            return;
        }
        DamageSource src = event.getSource();
        // 仅结算玩家自身的近战命中（direct==entity==player），避免投射物 / 幻影刃 / 追加伤害递归。
        if (!(src.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        if (!(src.getDirectEntity() instanceof Player) || src.getDirectEntity() != player) {
            return;
        }
        if (event.getEntity() == player) {
            return;
        }
        // 女仆/宠物保护：增伤不作用于主人自己的驯服生物
        if (MaidCompat.isOwnedBy(event.getEntity(), player)) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }

        ResourceLocation id = NrSpecialEffectsRegistry.SHADOON.getId();
        // 判定手持（主手优先，其次副手）携带且已激活疏影的刀。
        ItemStack blade = player.getMainHandItem();
        if (!(NRSpecialEffect.carriesSE(blade, id) && NRSpecialEffect.isAquaActive(blade, id))) {
            ItemStack off = player.getOffhandItem();
            if (NRSpecialEffect.carriesSE(off, id) && NRSpecialEffect.isAquaActive(off, id)) {
                blade = off;
            } else {
                return;
            }
        }

        event.setAmount(amount + moonBonus(level));
    }

    /**
     * 依连续月龄返回追加伤害：满月（月龄 0）最低、新月（月龄 4）最高，随盈亏对称、连续线性变化。
     *
     * @param level 服务端世界，用于取连续 {@code dayTime}
     * @return {@code [MIN_BONUS, MAX_BONUS]} 区间内的追加伤害
     */
    private static float moonBonus(Level level) {
        // 连续月龄：dayTime/24000 对 8 取模并保留小数，使盈亏平滑过渡而非每日整点跳变（Math.floorMod 无 double 重载，手动取模）。
        double cycles = level.getDayTime() / 24000.0;
        double age = cycles - Math.floor(cycles / MOON_PHASES) * MOON_PHASES;              // [0,8)
        double distanceFromFull = Math.min(age, MOON_PHASES - age);                        // 0（满月）~ 4（新月）
        return (float) (MIN_BONUS + distanceFromFull * (MAX_BONUS - MIN_BONUS) / (MOON_PHASES / 2.0));
    }
}
