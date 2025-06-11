package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityCountableSwords;
import net.xianyu.prinegorerouse.entity.EntityNRBlisteringSword;
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
                Vec3 offset = new Vec3(playerIn.getEyePosition().x, playerIn.getEyePosition().y, playerIn.getEyePosition().z);
                float yaw = 0F;
                float pitch = 0F;
                Level worldIn = playerIn.level();
                int colorR = 135000000;
                int colorG = 206000;
                int colorB = 150;
                int count = 54;
                EntityNRBlisteringSword ns = new EntityNRBlisteringSword(NrEntitiesRegistry.NRBlisteringSword, worldIn);
                ns.setSpeed(speed);
                ns.setDamage(20);
                ns.setPITCH(pitch);
                ns.setYAW(yaw);
                for (int i=1; i<count; i++) {
                    ns.setColor(colorR + colorG + colorB + 100000 * i);

                    EntityNRBlisteringSword.spawnSwords(playerIn, worldIn, offset, EntityNRBlisteringSword.SpawnMode.RANDOM
                                , 1, false, yaw,pitch,0,0, ns.getDamage(), ns.getColor(), false, 20 + i);

                }
            });
        }
    }
}
