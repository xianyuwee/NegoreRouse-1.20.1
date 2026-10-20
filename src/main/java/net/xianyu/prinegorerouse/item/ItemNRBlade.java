package net.xianyu.prinegorerouse.item;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.Tier;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ItemNRBlade extends ItemSlashBlade {
    public ItemNRBlade(Tier tier, int AttackDamageIn, float AttackSpeedIn, Properties builder) {
        super(tier, AttackDamageIn, AttackSpeedIn, builder);
    }
}
