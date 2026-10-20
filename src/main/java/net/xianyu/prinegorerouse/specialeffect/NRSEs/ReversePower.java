package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.entity.EntityReverseSword;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 负向神力（ReversePower）。
 * <p>给予玩家的正面 buff 保持不变；<b>负面 buff 不再被动施加</b>，而是<b>仅在玩家造成伤害时</b>触发：</p>
 * <ol>
 *   <li>从游戏内所有 {@link MobEffectCategory#HARMFUL} 效果（排除瞬间类与 config 黑名单）中随机抽一个，
 *       默认等级 1、默认持续 5s（均可 config）施加到玩家身上；</li>
 *   <li>随后把玩家身上<b>不在黑名单</b>的 debuff 转移为追踪幻影剑（{@link EntityReverseSword}）：
 *       黑名单效果根本不进 buff 池；白名单效果可在身上但<b>不被发射</b>（转移时跳过、留在玩家身上）。
 *       每次攻击最多生成 {@code min(6, 刀上力量附魔等级 + 1)} 支，效果从玩家移除、时长快照不衰减，
 *       剑色取效果粒子色，优先飞向玩家的重锋锁定目标，命中后把该 debuff 按快照时长转嫁到目标身上。</li>
 * </ol>
 */
@EventBusSubscriber
public class ReversePower extends NRSpecialEffect {

    public ReversePower() {
        super(35, false, false);
    }

    private static final Random random = new Random();
    private static final int MAX_SWORDS = 6;

    /** 有害效果池（惰性构建，进程内缓存；MobEffect 注册集在会话内稳定）。 */
    private static List<MobEffect> harmfulPool;

    // ===== 正面 buff：保持不变 =====
    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.ReversePower.getId())) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected())) {
                return;
            }
            if (NRSpecialEffect.isAquaActive(event.getBlade(), NrSpecialEffectsRegistry.ReversePower.getId())) {
                if (player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 3));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 1));
                }
            }
        }
    }

    // ===== 造成伤害时：先施加随机 debuff，再把身上的 debuff 转移成追踪剑 =====
    @SubscribeEvent
    public static void onDoingSlash(LivingHurtEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) {
            return;
        }
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        // 仅结算玩家自身的近战命中，排除幻影剑/投射物造成的伤害，避免递归
        if (!(src.getDirectEntity() instanceof Player) || src.getDirectEntity() != player) {
            return;
        }
        if (event.getEntity() == player || event.getAmount() <= 0.0F) {
            return;
        }
        // 女仆/宠物保护：debuff 转移不针对主人自己的驯服生物触发
        if (MaidCompat.isOwnedBy(event.getEntity(), player)) {
            return;
        }

        ResourceLocation id = NrSpecialEffectsRegistry.ReversePower.getId();
        ItemStack blade = player.getMainHandItem();
        if (!(NRSpecialEffect.carriesSE(blade, id) && NRSpecialEffect.isAquaActive(blade, id))) {
            ItemStack off = player.getOffhandItem();
            if (NRSpecialEffect.carriesSE(off, id) && NRSpecialEffect.isAquaActive(off, id)) {
                blade = off;
            } else {
                return;
            }
        }

        int duration = NRConfig.REVERSE_DEBUFF_DURATION.get();
        int amplifier = NRConfig.REVERSE_DEBUFF_AMPLIFIER.get();

        // 1) 先施加一个随机 debuff 到玩家身上
        MobEffect pick = pickRandomDebuff();
        if (pick != null) {
            player.addEffect(new MobEffectInstance(pick, duration, amplifier));
        }

        // 2) 再把身上（含刚施加的）非黑名单 debuff 转移成追踪剑，最多 min(6, 力量等级+1) 支
        int limit = Math.min(MAX_SWORDS, blade.getEnchantmentLevel(Enchantments.POWER_ARROWS) + 1);
        double dmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 spawn = player.getEyePosition().add(0.0, -0.3, 0.0);

        // 快照一份，避免移除活动效果时并发修改
        List<MobEffectInstance> snapshot = new ArrayList<>();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            snapshot.add(inst);
        }
        int launched = 0;
        for (MobEffectInstance inst : snapshot) {
            if (launched >= limit) {
                break;
            }
            MobEffect effect = inst.getEffect();
            if (!isTransferrable(effect)) {
                continue;
            }
            // 白名单：效果可留在玩家身上，但不被发射成幻影剑（不转移）。
            if (whitelisted(effect)) {
                continue;
            }
            int remaining = inst.getDuration();
            // 无限时长效果的 getDuration() 返回 INFINITE_DURATION(-1)，不能被“<=0 已到期”误判跳过；
            // 仅当“非无限 且 剩余<=0”（真正到期）才不携带。
            if (!(remaining > 0 || inst.isInfiniteDuration())) {
                continue;
            }
            player.removeEffect(effect); // 转移：从玩家身上移除
            EntityReverseSword.spawn(player, level, spawn, effect, remaining, inst.getAmplifier(), dmg);
            launched++;
        }
    }

    /** 该效果能否被抽取/转移：HARMFUL、非瞬间、且不在 config 黑名单。 */
    private static boolean isTransferrable(MobEffect effect) {
        if (effect == null || effect.getCategory() != MobEffectCategory.HARMFUL || effect.isInstantenous()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return key != null && !blacklisted().contains(key.toString());
    }

    /** 该效果是否在白名单上（白名单效果不发射，但仍在 buff 池内）。 */
    private static boolean whitelisted(MobEffect effect) {
        if (effect == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return key != null && whitelist().contains(key.toString());
    }

    /** 从有害池中随机取一个可转移效果；无可选返回 null。 */
    private static MobEffect pickRandomDebuff() {
        List<MobEffect> allowed = new ArrayList<>();
        for (MobEffect e : harmfulPool()) {
            if (isTransferrable(e)) {
                allowed.add(e);
            }
        }
        if (allowed.isEmpty()) {
            return null;
        }
        return allowed.get(random.nextInt(allowed.size()));
    }

    /** 惰性构建所有 HARMFUL、非瞬间效果池。 */
    private static List<MobEffect> harmfulPool() {
        if (harmfulPool == null) {
            List<MobEffect> pool = new ArrayList<>();
            for (MobEffect e : ForgeRegistries.MOB_EFFECTS) {
                if (e.getCategory() == MobEffectCategory.HARMFUL && !e.isInstantenous()) {
                    pool.add(e);
                }
            }
            harmfulPool = pool;
        }
        return harmfulPool;
    }

    private static List<? extends String> blacklisted() {
        List<? extends String> bl = NRConfig.REVERSE_DEBUFF_BLACKLIST.get();
        return bl == null ? List.of() : bl;
    }

    private static List<? extends String> whitelist() {
        List<? extends String> wl = NRConfig.REVERSE_DEBUFF_WHITELIST.get();
        return wl == null ? List.of() : wl;
    }
}
