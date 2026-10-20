package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;


/**
 * Phantom（幻影）：一个<b>纯特效（外观）SE</b>，本身不再提供任何战斗 / 增幅效果。
 * <p>它唯一的表现在客户端——当玩家<b>主手</b>持有一把“携带 Phantom 且已激活”的刀时，
 * 天空会出现一个与当前时间 {@code +13000} 相对应的天体（太阳↔月亮，只画天体本身）。
 * 渲染逻辑见 {@code net.xianyu.prinegorerouse.client.PhantomSkyRenderer}。</p>
 */
@Mod.EventBusSubscriber
public class Phantom extends NRSpecialEffect {
    public Phantom() {
        super(1, false, false);
    }
}
