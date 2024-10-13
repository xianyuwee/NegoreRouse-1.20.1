package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityZenith12thSword;
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
                for (int i = 0; i < count; ++i) {
                    EntityZenith12thSword ss = new EntityZenith12thSword(NrEntitiesRegistry.Zenith12th_Sword, worldIn);
                    worldIn.addFreshEntity(ss);
                    if (state.getKillCount() >= 200) {
                        ss.setDamage(state.getMaxDamage() * 0.4);
                    } else if (state.getKillCount()>=100) {
                        ss.setDamage((double) state.getDamage() * 0.1);
                    } else {
                        ss.setDamage(30.0D);
                    }
                    ss.setSpeed(speed);
                    if (state.getTargetEntity(worldIn) != null){
                        ss.setNoClip(true);
                    } else {
                        ss.setNoClip(false);
                    }
                    ss.setIsCritical(critical);
                    ss.setOwner(playerIn);
                    ss.setColor(16766720);
                    ss.setRoll(0.0F);
                    ss.startRiding(playerIn, true);
                    ss.setDelay(10 + i);
                    boolean isRight = ss.getDelay() % 2 == 0;
                    RandomSource random = worldIn.random;
                    double xOffset = random.nextDouble() * 10 * (double) (isRight ? 1 : -1);
                    double zOffset = (double) random.nextFloat() * 5;
                    double yOffset = random.nextFloat() * 10;
                    if (!(state.getTargetEntity(worldIn) == null)){
                        ss.setPos(state.getTargetEntity(worldIn).getEyePosition().add(xOffset, yOffset, zOffset));
                    } else {
                        ss.setPos(playerIn.getEyePosition().add(xOffset, yOffset, zOffset));
                    }
                    ss.setOffset(new Vec3(xOffset, yOffset, zOffset));
                    playerIn.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.2F,1.45F);
                }
            });
        }
    }
}
