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
public class Porgatory extends SpecialEffect {
    public Porgatory() {
        super(1,false,false);
    }

    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if(state.hasSpecialEffect(NrSpecialEffectsRegistry.Porgatory.getId())) {
            // 添加类型检查 - 修复崩溃的关键
        if (!(event.getUser() instanceof Player)) {
            return; // 如果不是玩家，直接返回
        }
        
        Player player = (Player) event.getUser(); // 现在安全了
        int level = player.experienceLevel;
            
            if(SpecialEffect.isEffective(NrSpecialEffectsRegistry.Porgatory.get(),level)) {
                event.getTarget().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,20));
            }
        }
    }
}
