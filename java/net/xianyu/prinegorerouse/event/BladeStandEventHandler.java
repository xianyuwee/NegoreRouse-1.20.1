package net.xianyu.prinegorerouse.event;


import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.recipe.RequestDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.data.builtin.NrBladeBuiltInRegistry;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.List;

@Mod.EventBusSubscriber
public class BladeStandEventHandler {

    @SubscribeEvent
    public static void eventnyx(SlashBladeEvent.BladeStandAttackEvent event) {
        var slashBladeDefinitionRegistry = SlashBlade.getSlashBladeDefinitionRegistry(event.getBladeStand().level());
        if(!slashBladeDefinitionRegistry.containsKey(NrBladeBuiltInRegistry.NYX_BLADE.location()))
            return;
        if(!(event.getDamageSource().getEntity() instanceof WitherBoss))
            return;
        if(!(event.getDamageSource().is(DamageTypeTags.IS_EXPLOSION)))
            return;
        EnchantmentDefinition chronosn = new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),5);
        RequestDefinition material = new RequestDefinition(prinegorerouse.prefix("chronosn_blade"),1000,0,0,List.of(chronosn),List.of(SwordType.BEWITCHED));
        if(!material.test(event.getBlade()))
            return;
        event.getBladeStand().setItem(slashBladeDefinitionRegistry.get(NrBladeBuiltInRegistry.NYX_BLADE).getBlade());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void eventdeligun(SlashBladeEvent.BladeStandAttackEvent event) {
        var slashbladeDefinitionRegistry = SlashBlade.getSlashBladeDefinitionRegistry(event.getBladeStand().level());
        BlockPos pos = event.getBladeStand().getOnPos().above(1);
        if(!slashbladeDefinitionRegistry.containsKey(NrBladeBuiltInRegistry.DELIGUN_BLADE.location()))
            return;
        if(!(event.getDamageSource().is(DamageTypeTags.IS_EXPLOSION)))
            return;
        if(!(event.getBladeStand().getBlockStateOn().getFluidState().is(FluidTags.WATER)))
            return;
        EnchantmentDefinition artemis = new EnchantmentDefinition(getEnchantmentID(Enchantments.POWER_ARROWS),5);
        RequestDefinition material = new RequestDefinition(prinegorerouse.prefix("aritemis_blade"),1000,0,0,List.of(artemis),List.of(SwordType.BEWITCHED));
        if(!material.test(event.getBlade()))
            return;
        event.getBladeStand().setItem(slashbladeDefinitionRegistry.get(NrBladeBuiltInRegistry.DELIGUN_BLADE).getBlade());
        event.setCanceled(true);
    }

    private static ResourceLocation getEnchantmentID(Enchantment enchantment){
        return ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
    }
}