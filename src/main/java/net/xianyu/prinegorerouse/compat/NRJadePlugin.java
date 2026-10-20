package net.xianyu.prinegorerouse.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.prinegorerouse;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * 玉(Jade)适配：把本模组注册的全部实体从玉的抬头提示中隐藏。
 * 本模组的实体全部是短生命周期的特效弹道（幻影刃/追踪剑/瞳孔等），
 * 出现在抬头提示里只会挡住玩家真正想看的方块与生物信息。
 *
 * 发现机制（Jade 1.20.1 Forge）：Jade 在加载完成时扫描 FML ModFileScanData 中
 * 所有模组登记的 @WailaPlugin 注解（value 为启用该插件的模组 id，仅当该模组存在才加载），
 * 以无参构造实例化后在物理客户端回调 registerClient——因此未安装 Jade 时本类不会被加载，无类耦合。
 */
@WailaPlugin(prinegorerouse.MOD_ID)
public class NRJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 无需服务端注册：隐藏目标纯客户端射线行为
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 按命名空间遍历实体注册表而非硬编码清单：
        // 本模组所有实体类型（含后续新增）自动进入玉的 hideEntities 屏蔽表
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            ResourceLocation key = EntityType.getKey(type);
            if (key != null && prinegorerouse.MOD_ID.equals(key.getNamespace())) {
                registration.hideTarget(type);
            }
        }
    }
}
