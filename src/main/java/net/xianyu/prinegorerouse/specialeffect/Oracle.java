package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.entity.EntityShinyDrive;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.utils.VectorHelper;

import java.util.Random;

@EventBusSubscriber
public class Oracle extends SpecialEffect {

    public Oracle() {
        super(1, true, true);
    }

    public static Random random = new Random();

    public static boolean hasSpecialEffects(ItemStack stack, String effect) {
        CompoundTag tag = stack.getOrCreateTag();

        if (tag.contains("bladeState")) {
            CompoundTag forgeCaps = tag.getCompound("bladeState");

            if (forgeCaps.contains("SpecialEffects")) {
                ListTag specialEffects = forgeCaps.getList("SpecialEffects", 8);
                for (int i = 0; i < specialEffects.size(); i++){
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
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected())) {
                return;
            }
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Oracle.get(), level)) {
                if (player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 4));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDoingSlash(SlashBladeEvent.DoSlashEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        int num = (int) (Math.random() * 4) + 1;
        float decimal = random.nextFloat();
        Player player = (Player) event.getUser();
        Level level1 = player.level();
        int level = player.experienceLevel;
        ItemStack offhandItem = player.getOffhandItem();
        if (!(offhandItem.getItem() instanceof ItemSlashBlade)) return;
        CompoundTag nbt = offhandItem.getOrCreateTag();
        if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Oracle.get(), level) && NRConfig.OFFHAND_CAN_ACTIVE.get().equals(true)
                && nbt != null) {
            if (!hasSpecialEffects(player.getOffhandItem(), prinegorerouse.MOD_ID+":oracle"))return;
            if (decimal - 0.5F <= 0.000001F) {
                Vec3 centerOffset = Vec3.ZERO;
                EntityShinyDrive drive = new EntityShinyDrive(NrEntitiesRegistry.ShinyDrive, level1);
                Vec3 lookAngle = player.getLookAngle();

                Vec3 pos = player.position().add(0.0D, (double) player.getEyeHeight() * 0.75D, 0.0D);

                pos = pos.add(VectorHelper.getVectorForRotation(0.0F, player.getViewYRot(0)).scale(centerOffset.y))
                        .add(VectorHelper.getVectorForRotation(0, player.getViewYRot(0) + 90).scale(centerOffset.z))
                        .add(lookAngle.scale(centerOffset.z));
                drive.setDamage(0.05F * state.getBaseAttackModifier());
                drive.setSpeed(3.0F);
                drive.setColor(111111111);
                drive.setPos(pos.x, pos.y, pos.z);
                drive.setOwner(player);
                drive.shoot(lookAngle.x, lookAngle.y, lookAngle.z, drive.getSpeed(), 0);
                drive.setDelay(10);
                drive.setLifetime(50);

                switch (num) {
                    case 1:
                        drive.setRotationRoll(80.0F);
                        break;
                    case 2:
                        drive.setRotationRoll(-80.0F);
                        break;
                    case 3:
                        drive.setRotationRoll(30.0F);
                        break;
                    case 4:
                        drive.setRotationRoll(-30.0F);
                        break;
                }

                player.level().addFreshEntity(drive);
            }
        }
    }


}
