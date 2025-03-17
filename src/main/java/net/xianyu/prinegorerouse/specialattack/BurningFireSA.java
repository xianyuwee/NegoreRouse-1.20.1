package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.entity.EntityFireDrive;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.VectorHelper;

import static java.lang.Math.*;

public class BurningFireSA {
    public BurningFireSA() {
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, Vec3 centerOffset, boolean critical, boolean clip, float speed) {
        doSlash(playerIn, roll, lifetime, centerOffset, critical, clip , KnockBacks.cancel, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, Vec3 centerOffset, boolean critical, boolean clip, KnockBacks knockBacks, float speed) {
        int colorCode = 255000000;
        doSlash(playerIn, roll, lifetime,colorCode, centerOffset, critical, clip, knockBacks, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, int colorCode, Vec3 centerOffset,
                               boolean critical, boolean clip, KnockBacks knockBacks, float speed) {
        if (!playerIn.level().isClientSide()) {
            playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent((state) -> {
                Level world = playerIn.level();
                int count = 3 + (int) (playerIn.getMainHandItem().getEnchantmentLevel(Enchantments.POWER_ARROWS)/2);
                for (int i = 1; i <= count; i++) {
                    EntityFireDrive driveEx = new EntityFireDrive(NrEntitiesRegistry.FireDrive, world);
                    world.addFreshEntity(driveEx);
                    if (state.getKillCount() >= 100) {
                        driveEx.setDamage((state.getAttackAmplifier() + state.getBaseAttackModifier()) * 1.1);
                    }
                    else {
                        driveEx.setDamage((state.getAttackAmplifier() + state.getBaseAttackModifier()) * 0.9);
                    }

                    boolean isRight = i % 2 ==0;

                    Vec3 lookAngle = playerIn.getLookAngle();
                    double x_ = asin(lookAngle.x);
                    double z_ = asin(lookAngle.z);

                    Vec3 pos = playerIn.position().add(0.0D, (double) playerIn.getEyeHeight() * 0.75D, 0.0D);

                    pos = pos.add(VectorHelper.getVectorForRotation(0.0F, playerIn.getViewYRot(0)).scale(centerOffset.y))
                            .add(VectorHelper.getVectorForRotation(0, playerIn.getViewYRot(0) + 90).scale(centerOffset.z))
                            .add(lookAngle.scale(centerOffset.z));

                    driveEx.setSpeed(speed);

                    if (x_ * z_ >= 0) {
                        driveEx.setPos(pos.x + sin(x_ + (22.5 * i * PI /180) * (double) (isRight ? 1:-1)),
                                pos.y,
                                pos.z + (sin(z_ + (22.5 * (i-1) * PI /180)) * (double) (isRight ? 1:-1)));
                        driveEx.shoot(sin(x_ + (22.5 * i * PI /180) * (double) (isRight ? 1:-1)),
                                0.0D,
                                cos(z_ + (22.5 * (i * PI /180)) * (double) (isRight ? 1:-1)),
                                driveEx.getSpeed(),
                                0);
                    } else {
                        driveEx.setPos(pos.x - sin(x_ + (22.5 * i * PI /180) * (double) (isRight ? 1:-1)),
                                pos.y,
                                pos.z - (sin(z_ + (22.5 * (i-1) * PI /180)) * (double) (isRight ? 1:-1)));
                        driveEx.shoot(sin(x_ - (22.5 * i * PI /180) * (double) (isRight ? 1:-1)),
                                0.0D,
                                sin(z_ - (22.5 * (i * PI /180)) * (double) (isRight ? 1:-1)),
                                driveEx.getSpeed(),
                                0);
                    }

                    driveEx.setOwner(playerIn);
                    driveEx.setDelay(20);
                    driveEx.setColor(colorCode);
                    driveEx.setIsCritical(critical);
                    driveEx.setNoClip(clip);
                    driveEx.setKnockBack(knockBacks);
                    driveEx.setLifetime(lifetime);
                    driveEx.setRotationRoll(roll);

                    if (playerIn != null) {
                        playerIn.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                                .ifPresent(rank -> driveEx.setRank(rank.getRankLevel(playerIn.level().getGameTime())));
                    }



                }
            });
        }
    }
    private static ResourceLocation getEnchantmentID(Enchantment enchantment) {
        return ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
    }
}
