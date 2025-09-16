package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

@EventBusSubscriber
public class ReversePower extends SpecialEffect {

    public ReversePower() {
        super(35, false, false);
    }

    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if(state.hasSpecialEffect(NrSpecialEffectsRegistry.ReversePower.getId())) {
            // 实体判断逻辑已存在且完整，无需修改：先验证event.getEntity()是否为Player，非Player直接返回
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected())) {
                return;
            }
            int level = player.experienceLevel;
            if(SpecialEffect.isEffective((SpecialEffect)NrSpecialEffectsRegistry.ReversePower.get(),level)) {
                if(player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())){
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 3));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 1));
                    if (NRConfig.DO_DEBUFF_WORK.get().equals(true)) {
                        player.addEffect(new MobEffectInstance(MobEffects.UNLUCK,40,5));
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,40,1));
                    }
                }
                else if (player.isUsingItem() && player.getOffhandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 1));
                }
            }
        }
    }
}
