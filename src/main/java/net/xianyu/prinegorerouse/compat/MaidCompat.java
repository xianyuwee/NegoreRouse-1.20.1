package net.xianyu.prinegorerouse.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.UUID;

/**
 * Touhou Little Maid（豆腐小女仆）适配工具。
 * <p>本 mod 的 SA/SE 不应对玩家所属的女仆造成伤害或施加特殊逻辑；
 * 女仆持用本 mod 拔刀剑时 SE 也不应触发。</p>
 * <p>实现要点：TLM 的 {@code EntityMaid} 继承原版 {@link TamableAnimal}，
 * 因此全程只依赖原版类即可覆盖女仆（以及玩家自己的狼/猫等宠物，同属误伤保护），
 * 不需要对 TLM 产生编译期或运行期硬依赖——未安装女仆 mod 时逻辑自然为空。</p>
 */
public final class MaidCompat {

    private MaidCompat() {
    }

    /** 实体是否为可驯服生物（TLM 女仆继承 {@link TamableAnimal}，原版宠物同享此判定）。 */
    public static boolean isTameable(Entity e) {
        return e instanceof TamableAnimal;
    }

    /**
     * 目标是否为「已被 {@code possibleOwner} 驯服归属」的女仆/宠物。
     * 仅保护归属该攻击者自己的驯服生物；他人宠物不受影响（PvP 场景仍可正常攻击）。
     */
    public static boolean isOwnedBy(Entity target, Entity possibleOwner) {
        if (!(target instanceof TamableAnimal t) || possibleOwner == null) {
            return false;
        }
        // 已驯服才会写入主人 UUID，无需再单独判 isTamed
        UUID ownerUuid = t.getOwnerUUID();
        if (ownerUuid == null) {
            return false;
        }
        // 玩家主人：UUID 直接比对（攻击者为主人本人即命中保护，他人宠物不受影响）
        return ownerUuid.equals(possibleOwner.getUUID());
    }
}
