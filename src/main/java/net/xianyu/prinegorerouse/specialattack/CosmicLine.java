package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityLineDrive;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.utils.VectorHelper;

import static java.lang.Math.*;
import static java.lang.Math.toRadians;

public class CosmicLine {
    public CosmicLine() {}
    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, Vec3 centerOffset, boolean critical, boolean clip, float speed) {
        doSlash(playerIn, roll, lifetime, centerOffset, critical, clip, KnockBacks.cancel, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, Vec3 centerOffset, boolean critical, boolean clip, KnockBacks knockBacks, float speed) {
        int colorCode = -255000255;
        doSlash(playerIn, roll, lifetime, colorCode, centerOffset, critical, clip, knockBacks, speed);
    }

    public static void doSlash(LivingEntity playerIn, float roll, int lifetime, int colorCode, Vec3 centerOffset,
                               boolean critical, boolean clip, KnockBacks knockBacks, float speed) {
        if (!playerIn.level().isClientSide()) {
            playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent((state) -> {
                Level world = playerIn.level();
                int rank = playerIn.getCapability(CapabilityConcentrationRank.RANK_POINT).map(r -> r.getRank(world.getGameTime()).level).orElse(0);

                int count = 1;
                switch (rank) {
                    case 0,1,2 -> count = 3;
                    case 3,4 -> count = 5;
                    case 5,6 -> count = 7;
                    case 7-> count = 9;
                }
                // 基础爪形展开角度（度数）
                float baseSpreadAngle = 8.0F;

                // 获取玩家偏航角（水平旋转角度）
                float yaw = playerIn.getYRot();

                // 计算水平右向量（基于玩家偏航角）
                Vec3 rightVector = new Vec3(-sin(toRadians(yaw)), 0, cos(toRadians(yaw)));

                // 获取玩家视线方向（用于前进方向）
                Vec3 lookAngle = playerIn.getLookAngle();
                for (int i = 1; i <= count; i++) {
                    EntityLineDrive lineDrive = new EntityLineDrive(NrEntitiesRegistry.LineDrive, world);
                    if (state.getKillCount() >= 10000) {
                        lineDrive.setDamage(20/ Mth.sqrt((float) playerIn.getAttributeValue(Attributes.ATTACK_DAMAGE)));
                    } else{
                        lineDrive.setDamage(7.5/Mth.sqrt((float) playerIn.getAttributeValue(Attributes.ATTACK_DAMAGE)));
                    }
                    // 基础位置（玩家眼睛高度）
                    Vec3 pos = playerIn.position().add(0.0D, (double) playerIn.getEyeHeight() * 0.75D, 0.0D);

                    // 计算角度偏移（形成爪形）
                    float angleOffset = 0;

                    // 计算偏移方向和强度
                    if (i > 1) {
                        // 确定偏移方向：偶数向右，奇数向左
                        float direction = (i % 2 == 0) ? 1 : -1;

                        // 计算偏移强度：每对火球使用相同的偏移量
                        int pairIndex = i / 2; // 计算属于第几对（从1开始）
                        angleOffset = direction * baseSpreadAngle * pairIndex;
                    }
                    // 1. 位置偏移：在水平面上偏移
                    // 计算水平偏移向量
                    Vec3 horizontalOffset = rightVector.scale(tan(toRadians(angleOffset)) * centerOffset.z);

                    // 应用位置偏移
                    pos = pos
                            .add(lookAngle.scale(centerOffset.z)) // 向前偏移
                            .add(horizontalOffset); // 水平偏移

                    lineDrive.setPos(pos);

                    // 2. 方向偏移：在水平面上旋转
                    Vec3 directionVec = VectorHelper.rotateVectorAroundY(lookAngle, angleOffset);

                    lineDrive.shoot(directionVec.x, directionVec.y, directionVec.z, speed, 0.0F);

                    lineDrive.setSpeed(speed);
                    lineDrive.setOwner(playerIn);
                    lineDrive.setColor(colorCode);
                    lineDrive.setIsCritical(critical);
                    lineDrive.setNoClip(clip);
                    lineDrive.setKnockBack(knockBacks);
                    lineDrive.setLifetime(lifetime);
                    lineDrive.setRotationRoll(roll);

                    if (playerIn != null) {
                        playerIn.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                                .ifPresent(r -> lineDrive.setRank(r.getRankLevel(playerIn.level().getGameTime())));
                    }
                    world.addFreshEntity(lineDrive);
                    playerIn.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.2F, 1.5F);
                }
            });
        }
    }
}
