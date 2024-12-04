package net.xianyu.prinegorerouse.registry;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class NrSpecialEffectsRegistry {
    public static final DeferredRegister<SpecialEffect> SPECIAL_EFFECT = DeferredRegister.create(SpecialEffect.REGISTRY_KEY,"prinegorerouse");
    public static final RegistryObject<SpecialEffect> Oracle = SPECIAL_EFFECT.register("oracle", net.xianyu.prinegorerouse.specialeffect.Oracle::new);
    public static final RegistryObject<SpecialEffect> Back = SPECIAL_EFFECT.register("back", net.xianyu.prinegorerouse.specialeffect.Back::new);
    public static final RegistryObject<SpecialEffect> AbsolutePower = SPECIAL_EFFECT.register("absolute_power", net.xianyu.prinegorerouse.specialeffect.AbsolutePower::new);
    public static final RegistryObject<SpecialEffect> ReversePower = SPECIAL_EFFECT.register("reverse_power", net.xianyu.prinegorerouse.specialeffect.ReversePower::new);
    public static final RegistryObject<SpecialEffect> Eternity = SPECIAL_EFFECT.register("eternity", net.xianyu.prinegorerouse.specialeffect.Eternity::new);
    public static final RegistryObject<SpecialEffect> Clear = SPECIAL_EFFECT.register("clear",net.xianyu.prinegorerouse.specialeffect.Clear::new);
    public static final RegistryObject<SpecialEffect> Porgatory = SPECIAL_EFFECT.register("porgatory",net.xianyu.prinegorerouse.specialeffect.Porgatory::new);
    public static final RegistryObject<SpecialEffect> Fate = SPECIAL_EFFECT.register("fate",net.xianyu.prinegorerouse.specialeffect.Fate::new);
    public static final RegistryObject<SpecialEffect> Empty = SPECIAL_EFFECT.register("empty",net.xianyu.prinegorerouse.specialeffect.Empty::new);
    public static final RegistryObject<SpecialEffect> Phantom = SPECIAL_EFFECT.register("phantom",net.xianyu.prinegorerouse.specialeffect.Phantom::new);
}


