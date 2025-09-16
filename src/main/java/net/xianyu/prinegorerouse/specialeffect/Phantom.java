package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

@Mod.EventBusSubscriber
public class Phantom extends SpecialEffect {
    public Phantom() {
        super(1, false, false);
    }

    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        
        // 添加类型检查 - 修复崩溃的关键
        if (!(event.getUser() instanceof Player)) {
            return; // 如果不是玩家，直接返回
        }
        
        Player player = (Player) event.getUser(); // 现在安全了
        int level = player.experienceLevel;
        
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Phantom.getId())) {
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Phantom.get(), level)) {
                float maxHealth = event.getTarget().getMaxHealth();
                float health = event.getTarget().getHealth();
                float heal = maxHealth - health;
                event.getUser().heal(heal);
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 1));
            }
        }
    }
}
