package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.utils.DamageTypeHolderUtils;

import java.util.Random;

@EventBusSubscriber
public class Back extends SpecialEffect {

    public Back() {
        super(90, false, false);
    }

    public static boolean hasSpecialEffects(ItemStack stack, String effect) {
        CompoundTag tag = stack.getOrCreateTag();

        if (tag.contains("bladeState")) {
            CompoundTag forgeCaps = tag.getCompound("bladeState");

            if (forgeCaps.contains("SpecialEffects")) {
                ListTag specialEffects = forgeCaps.getList("SpecialEffects", 8);
                for (int i = 0; i < specialEffects.size(); i++) {
                    String currentEffect = specialEffects.getString(i);
                    if (effect.equals(currentEffect)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Back.getId())) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected()))
                return;
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Back.get(), level)) {
                if (player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2));
                }
            }
        }
    }

    static Random random = new Random();

    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        // 修复核心：先判断事件触发者是否为Player，非Player直接返回，避免强制转换异常
        if (!(event.getUser() instanceof Player))
            return;
        
        Player player = (Player) event.getUser();
        ISlashBladeState state = event.getSlashBladeState();
        ItemStack offhandItem = player.getOffhandItem();

        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Back.getId())) {
            Level level1 = player.level();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective(NrSpecialEffectsRegistry.Back.get(), level) && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                double decimal = random.nextDouble();
                if (decimal - 0.4d <= 0.00001) {
                    Entity entity = event.getTarget();
                    entity.hurt(new DamageSource(DamageTypeHolderUtils.getHolder(level1, "out_of_world"), player, player), state.getBaseAttackModifier() * 0.5F);
                    int blade_damage = state.getDamage();
                    state.setDamage(blade_damage - 1);
                }
            }
        } else if (NRConfig.OFFHAND_CAN_ACTIVE.get().equals(true) && offhandItem.getItem() instanceof ItemSlashBlade) {
            Level level = player.level();
            if (!hasSpecialEffects(player.getOffhandItem(), prinegorerouse.MOD_ID + ":back"))
                return;
            double decimal = random.nextDouble();
            if (decimal - 0.5d <= 0.00001) {
                Entity entity = event.getTarget();
                entity.hurt(new DamageSource(DamageTypeHolderUtils.getHolder(level, "out_of_world"), player, player), state.getBaseAttackModifier() * 0.2F);
                int blade_damage = state.getDamage();
                state.setDamage(blade_damage - 1);
            }
        }
    }
}
