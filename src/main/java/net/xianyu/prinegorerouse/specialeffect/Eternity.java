package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

// 移除无用导入：javax.swing.text.JTextComponent（与Minecraft逻辑无关，避免冗余依赖）
@Mod.EventBusSubscriber
public class Eternity extends SpecialEffect {
    public Eternity() {
        super(200, false, false);
    }


    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Eternity.getId())) {
            // 修复核心：先判断event.getUser()是否为Player，非Player直接返回，避免强制转换异常
            if (!(event.getUser() instanceof Player))
                return;
            
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            
            if (!SpecialEffect.isEffective(NrSpecialEffectsRegistry.Eternity.get(), level)) {
                event.getSlashBladeState().setBroken(true);
            } else {
                float hp = event.getTarget().getHealth();
                event.getTarget().setHealth(0.5f * hp);
            }
        }
    }
}
