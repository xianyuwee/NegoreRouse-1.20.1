package net.xianyu.prinegorerouse.registry;

import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;


public class NrSlashArtRegistry {
    public static final DeferredRegister<SlashArts> NR_SLASH_ARTS;
    public static final RegistryObject<SlashArts> ZENITH12TH;
    public static final RegistryObject<SlashArts> STORM_SWORDS;
    public static final RegistryObject<SlashArts> DIVINE_CROSS_SA;

    public NrSlashArtRegistry(){
    }

    static {
        NR_SLASH_ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY, "prinegorerouse");
        ZENITH12TH = NR_SLASH_ARTS.register("zenith12th", () -> {
            return new SlashArts((e) -> {
                return NrComboStateRegistry.ZENITH12TH.getId();
            });
        });


        STORM_SWORDS = NR_SLASH_ARTS.register("magnetic_storm_sword", () -> {
            return new SlashArts((e) -> {
                return NrComboStateRegistry.STORM_SWORDS.getId();
            });
        });

        DIVINE_CROSS_SA = NR_SLASH_ARTS.register("divine_cross_sa", () -> {
            return new SlashArts((e) -> {
                return NrComboStateRegistry.DIVINE_CROSS_SA.getId();
            });
        });
    }
}