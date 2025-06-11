package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityEnchantedSword;
import net.xianyu.prinegorerouse.entity.EntityNRBlisteringSword;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class Zenith12th{
    public Zenith12th() {
    }

    public static void doSlash(LivingEntity playerIn, boolean critical, float speed) {
        int colorCode = (Integer)playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map((state) -> {
            return state.getColorCode();
        }).orElse(-13421569);
        doSlash(playerIn, colorCode, critical, speed);
    }

    public static void doSlash(LivingEntity playerIn, int colorCode, boolean critical, float speed) {
        if (!playerIn.level().isClientSide()) {
            playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent((state) -> {
                Level worldIn = playerIn.level();
                int count = 13 ;
                float pitch =0F,yaw = 0F;
                for (int i = 0; i < count; ++i) {
                    EntityEnchantedSword es = new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, worldIn);
                    es.setOwner(playerIn);
                    if (state.getKillCount() >= 200) {
                        es.setDamage(state.getBaseAttackModifier() * 1.2);
                    } else if (state.getKillCount()>=100) {
                        es.setDamage(state.getBaseAttackModifier() * 0.8);
                    } else {
                        es.setDamage(state.getBaseAttackModifier() * 0.5);
                    }
                    es.setSpeed(speed);
                    es.setNoClip(true);
                    es.setIsCritical(critical);
                    es.setColor(16766720);
                    es.enableSmartTracking(true);
                    Vec3 offset = new Vec3(playerIn.getEyePosition().x, playerIn.getEyePosition().y, playerIn.getEyePosition().z);
                    EntityEnchantedSword.spawnSwords(playerIn,worldIn,offset, EntityNRBlisteringSword.SpawnMode.RANDOM,1,
                            true,yaw,pitch,0,0,es.getDamage(),es.getColor(), false, 20 + i);
                    playerIn.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.2F,1.45F);
                }
            });
        }
    }
}
