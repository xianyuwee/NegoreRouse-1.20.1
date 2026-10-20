package net.xianyu.prinegorerouse.registry;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class NrSpecialEffectsRegistry {
    public static final DeferredRegister<SpecialEffect> SPECIAL_EFFECT = DeferredRegister.create(SpecialEffect.REGISTRY_KEY,"prinegorerouse");
    public static final RegistryObject<SpecialEffect> Oracle = SPECIAL_EFFECT.register("oracle", net.xianyu.prinegorerouse.specialeffect.NRSEs.Oracle::new);
    public static final RegistryObject<SpecialEffect> Back = SPECIAL_EFFECT.register("back", net.xianyu.prinegorerouse.specialeffect.NRSEs.Back::new);
    public static final RegistryObject<SpecialEffect> AbsolutePower = SPECIAL_EFFECT.register("absolute_power", net.xianyu.prinegorerouse.specialeffect.NRSEs.AbsolutePower::new);
    public static final RegistryObject<SpecialEffect> ReversePower = SPECIAL_EFFECT.register("reverse_power", net.xianyu.prinegorerouse.specialeffect.NRSEs.ReversePower::new);
    public static final RegistryObject<SpecialEffect> Eternity = SPECIAL_EFFECT.register("eternity", net.xianyu.prinegorerouse.specialeffect.NRSEs.Eternity::new);
    public static final RegistryObject<SpecialEffect> Clear = SPECIAL_EFFECT.register("clear", net.xianyu.prinegorerouse.specialeffect.NRSEs.Clear::new);
    public static final RegistryObject<SpecialEffect> Porgatory = SPECIAL_EFFECT.register("porgatory", net.xianyu.prinegorerouse.specialeffect.NRSEs.Porgatory::new);
    public static final RegistryObject<SpecialEffect> Fate = SPECIAL_EFFECT.register("fate", net.xianyu.prinegorerouse.specialeffect.NRSEs.Fate::new);
    public static final RegistryObject<SpecialEffect> Empty = SPECIAL_EFFECT.register("empty", net.xianyu.prinegorerouse.specialeffect.NRSEs.Empty::new);
    public static final RegistryObject<SpecialEffect> Phantom = SPECIAL_EFFECT.register("phantom", net.xianyu.prinegorerouse.specialeffect.NRSEs.Phantom::new);
    public static final RegistryObject<SpecialEffect> CHAOS = SPECIAL_EFFECT.register("chaos", net.xianyu.prinegorerouse.specialeffect.NRSEs.Chaos::new);
    public static final RegistryObject<SpecialEffect> SHADOON = SPECIAL_EFFECT.register("shadoon", net.xianyu.prinegorerouse.specialeffect.NRSEs.Shadoon::new);
}


