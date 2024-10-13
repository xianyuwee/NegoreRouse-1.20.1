package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.entity.EntityZenith12thSword;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

import java.util.Random;


@Mod.EventBusSubscriber
public class Fate extends SpecialEffect {
    public Fate() {
        super(30, true, true);
    }

    static Random random = new Random();

    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Fate.getId())) {
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Fate.get(), level)) {
                double decimal = random.nextDouble();
                if (decimal - 0.5d <= 0.000001) {
                    player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 10));
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 10, 1));
                    event.getTarget().addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 1));
                }
                if (decimal >= 0.2d) {
                    Level worldIn = player.level();
                    EntityZenith12thSword ss = new EntityZenith12thSword(NrEntitiesRegistry.Zenith12th_Sword, worldIn);
                    worldIn.addFreshEntity(ss);
                    ss.setSpeed(10.0F);
                    ss.setNoClip(false);
                    ss.setDamage(5);
                    ss.startRiding(player, true);
                    ss.setShooter(player);
                    ss.setDelay(5);
                    RandomSource random = worldIn.random;
                    double xOffset = random.nextDouble() * 2;
                    double zOffset = (double) random.nextFloat() * 2;
                    double yOffset = random.nextFloat();
                    ss.setPos(player.getEyePosition().add(xOffset, yOffset, zOffset));
                    ss.setOffset(new Vec3(xOffset, yOffset, zOffset));
                }
            }
        }
    }
}