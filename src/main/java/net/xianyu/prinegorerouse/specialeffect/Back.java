package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

import java.util.Random;


@EventBusSubscriber
public class Back extends SpecialEffect{

    public Back() {
        super(90, true, true);
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
                if(player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2));
                }
            }
        }
    }

    static Random random = new Random();


    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if(state.hasSpecialEffect(NrSpecialEffectsRegistry.Back.getId())) {
            if (!(event.getUser() instanceof Player))
                return;
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if(SpecialEffect.isEffective(NrSpecialEffectsRegistry.Back.get(),level)) {
                double decimal = random.nextDouble();
                if ( decimal-0.4d <= 0.00001) {
                    Entity entity = event.getTarget();
                    DamageSource damageSource = entity.damageSources().indirectMagic(entity,player);
                    player.hurt(damageSource, event.getSlashBladeState().getBaseAttackModifier());
                    int blade_damage = state.getDamage();
                    state.setDamage(blade_damage - 1);
                }
            }
        }
    }

}
