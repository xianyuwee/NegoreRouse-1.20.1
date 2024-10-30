package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityBlisteringSword;
import net.xianyu.prinegorerouse.entity.EntityStormSword;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

public class MagneticStormSword {
    public MagneticStormSword() {
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
                int colorR = 135000000;
                int colorG = 206000;
                int colorB = 150;
                int count = 54;
                for (int j = 1; j <= count; j++){
                    EntityStormSword sse = new EntityStormSword(NrEntitiesRegistry.Storm_Sword, worldIn);
                    EntityBlisteringSword ss = new EntityBlisteringSword(NrEntitiesRegistry.BlisteringSword, worldIn);
                    if (j % 3 !=0){
                        if (state.getTargetEntity(worldIn) != null) {
                            ss.setNoClip(true);
                        } else {
                            ss.setNoClip(false);
                        }
                        worldIn.addFreshEntity(ss);
                        ss.setColor(colorB + colorG + colorR + j);
                        ss.setSpeed(speed);
                        ss.setDamage(8.0D);
                        ss.setOwner(playerIn);
                        ss.setRoll(0.0F);
                        ss.setIsCritical(critical);
                        ss.startRiding(playerIn, true);
                        ss.setDelay((int) (20 + 0.3 * j));
                        boolean isRight = ss.getDelay() % 2 == 0;
                        RandomSource random = worldIn.random;
                        double xOffset = random.nextDouble() * 2.5 * (double) (isRight ? 1 : -1);
                        double yOffset = (double) (random.nextFloat() * 2.0F);
                        double zOffset = (double) random.nextFloat() * 0.5;
                        ss.setPos(playerIn.getEyePosition().add(xOffset, yOffset, zOffset));
                        ss.setOffset(new Vec3(xOffset,yOffset,zOffset));
                    } else {
                        if (state.getTargetEntity(worldIn) != null) {
                            sse.setNoClip(true);
                        } else {
                            sse.setNoClip(false);
                        }
                        worldIn.addFreshEntity(sse);
                        sse.startRiding(playerIn,true);
                        sse.setSpeed(speed);
                        sse.setDamage(8.0D);
                        sse.setOwner(playerIn);
                        sse.setRoll(0.0F);
                        sse.setIsCritical(critical);
                        sse.setColor(175238238);
                        sse.setDelay((int) (12 + 0.3 * j));
                        RandomSource random = worldIn.random;
                        boolean isRight_e = sse.getDelay() % 2 ==0;
                        double xOffset_e = random.nextDouble() * 2.5 * (double) (isRight_e ? 1 : -1);
                        double yOffset = (double) (random.nextFloat() * 2.0F);
                        double zOffset = (double) random.nextFloat() * 0.5;
                        sse.setPos(playerIn.getEyePosition().add(xOffset_e, yOffset, zOffset));
                        sse.setOffset(new Vec3(xOffset_e, yOffset, zOffset));
                    }
                }
            });
        }
    }
}
