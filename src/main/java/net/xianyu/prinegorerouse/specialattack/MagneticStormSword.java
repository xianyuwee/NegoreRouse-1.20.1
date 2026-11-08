package net.xianyu.prinegorerouse.specialattack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xianyu.prinegorerouse.entity.EntityCountableSwords;
import net.xianyu.prinegorerouse.entity.EntityNRBlisteringSword;
import net.xianyu.prinegorerouse.entity.EntityStormSword;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;

/**
 * 磁暴（MagneticStormSword）SA 生成逻辑。
 *
 * <p>在玩家眼睛周围 5×5×5 的随机立方体内生成 {@code count-1} 把幻影剑，全部朝玩家朝向发射，
 * 颜色逐把递进；<b>每 tick 发射 2 把</b>（两根共用同一个 delay），所以整轮齐射的持续时间
 * 只有原来的一半（20..73 tick → 20..46 tick）。</p>
 *
 * <p>生成位置 / 数量 / 延迟起点 / 颜色 / 穿墙等参数与原来保持一致，只把发射节奏翻倍；
 * 剑本身使用 {@code setMagneticStorm(true)} 的「磁暴样式」：加速冲刺 + 恒定角速度平滑追踪 + 客户端拖尾
 * （见 {@link EntityNRBlisteringSword#isMagneticStorm()}）。</p>
 */
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

                    // 发射节奏：每 tick 2 根（相邻两根共用同一个 delay），整轮从 53 tick 压缩到 27 tick
                    int fireDelay = 20 + ((i - 1) >> 1);
                    EntityNRBlisteringSword.spawnSwords(playerIn, worldIn, offset, EntityNRBlisteringSword.SpawnMode.RANDOM
                                , 1, false, yaw,pitch,0,0, ns.getDamage(), ns.getColor(), true, fireDelay, true);

                }
            });
        }
    }
}
