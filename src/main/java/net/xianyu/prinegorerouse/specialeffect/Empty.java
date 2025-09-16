package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;


@Mod.EventBusSubscriber
public class Empty extends SpecialEffect {
    public Empty() {
        super(1,false,false);
    }


    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if(state.hasSpecialEffect(NrSpecialEffectsRegistry.Empty.getId())) {
            // 已存在Player类型前置判断，基础安全逻辑完整
            if (!(event.getUser() instanceof Player))
                return;
            
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            
            if(SpecialEffect.isEffective(NrSpecialEffectsRegistry.Empty.get(), level)) {
                event.getTarget().setHealth(0.0f);
                // 修复优化：复用已验证类型的player对象，替代重复调用event.getUser()，避免潜在类型风险
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
    }
}
