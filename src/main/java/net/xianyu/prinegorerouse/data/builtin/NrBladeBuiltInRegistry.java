package net.xianyu.prinegorerouse.data.builtin;

import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrSlashArtRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

import java.util.List;


public class NrBladeBuiltInRegistry {
    public static final ResourceKey<SlashBladeDefinition> AEON_BLADE = register("aeon_blade");
    public static final ResourceKey<SlashBladeDefinition> ANANKE_BLADE = register("ananke_blade");
    public static final ResourceKey<SlashBladeDefinition> ANTAUGE_BLADE = register("antauge_blade");
    public static final ResourceKey<SlashBladeDefinition> ARITEMIS_BLADE = register("aritemis_blade");
    public static final ResourceKey<SlashBladeDefinition> CHAOS_BLADE = register("chaos_blade");
    public static final ResourceKey<SlashBladeDefinition> CHRONOS_BLADE = register("chronos_blade");
    public static final ResourceKey<SlashBladeDefinition> CHRONOSN_BLADE = register("chronosn_blade");
    public static final ResourceKey<SlashBladeDefinition> DELIGUN_BLADE = register("deligun_blade");
    public static final ResourceKey<SlashBladeDefinition> EREBUS_BLADE = register("erebus_blade");
    public static final ResourceKey<SlashBladeDefinition> HERCULES_BLADE = register("hercules_blade");
    public static final ResourceKey<SlashBladeDefinition> NIER_BLADE = register("nier_blade");
    public static final ResourceKey<SlashBladeDefinition> CHRONOSSY_BLADE = register("chronossy_blade");
    public static final ResourceKey<SlashBladeDefinition> NYX_BLADE = register("nyx_blade");
    public static final ResourceKey<SlashBladeDefinition> PROTOGENOI_BLADE = register("protogenoi_blade");
    public static final ResourceKey<SlashBladeDefinition> TARTARUS_BLADE = register("tartarus_blade");

    //NINJA: 右上到左下斜插
    //DEFAULT/PS02: 横插
    //RNINJA: 左上到右下斜插
    //KATANA: 腰插(刀刃向上)
    //NONE: ?

    public static void registerAll(BootstapContext<SlashBladeDefinition> bootstrap) {
        bootstrap.register(AEON_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("aeon_blade"),
                RenderDefinition.Builder.newInstance()
                        .effectColor(25518515)
                        .textureName(prinegorerouse.prefix("model/named/custom/aeon/aeon.png"))
                        .modelName(prinegorerouse.prefix("model/named/custom/aeon/aeon.obj"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(300.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Eternity.getId())
                                .maxDamage(40)
                                .build(),
                        List.of()));

        bootstrap.register(ANANKE_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("ananke_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(25518515)
                                .textureName(prinegorerouse.prefix("model/named/custom/ananke/ananke.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/ananke/ananke_blade.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(46.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(NrSlashArtRegistry.ZENITH12TH.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Fate.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .maxDamage(100)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.POWER_ARROWS),9),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),10),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_ASPECT),3),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.KNOCKBACK),2))));

        bootstrap.register(ANTAUGE_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("antauge_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(245222179)
                                .textureName(prinegorerouse.prefix("model/named/custom/antauge/antauge.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/antauge/antauge.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(45.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Back.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),7),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),4),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),8),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_ASPECT),5))));

        bootstrap.register(ARITEMIS_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("aritemis_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(32178170)
                                .textureName(prinegorerouse.prefix("model/named/custom/aritemis/artemis_blade.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/aritemis/artemis_blade.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(24.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(NrSlashArtRegistry.STORM_SWORDS.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Clear.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.POWER_ARROWS),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),3),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.FISHING_LUCK),5))));

        bootstrap.register(CHAOS_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("chaos_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(255236139)
                                .textureName(prinegorerouse.prefix("model/named/custom/chaos/chaos_blade.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/chaos/chaos_blade.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(51.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),10))));

        bootstrap.register(CHRONOS_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("chronos_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(132112255)
                                .textureName(prinegorerouse.prefix("model/named/custom/chronos/chronos.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/chronos/chronos.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(25.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.POWER_ARROWS),10),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),3),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),5))));

        bootstrap.register(CHRONOSN_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("chronosn_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(10690205)
                                .textureName(prinegorerouse.prefix("model/named/custom/chronosn/chronosn.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/chronosn/chronosn.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(35.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.AbsolutePower.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),10),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.MOB_LOOTING),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_ASPECT),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),5))));

        bootstrap.register(CHRONOSSY_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("chronossy_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(100149237)
                                .textureName(prinegorerouse.prefix("model/named/custom/chronossy/chronossy.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/chronossy/chronossy.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(0.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Empty.getId())
                                .maxDamage(2)
                                .build(),
                        List.of()));

        bootstrap.register(DELIGUN_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("deligun_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(30144255)
                                .textureName(prinegorerouse.prefix("model/named/custom/deligun/deligun.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/deligun/deligun.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(37.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),7))));

        bootstrap.register(EREBUS_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("erebus_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(10690205)
                                .textureName(prinegorerouse.prefix("model/named/custom/erebus/erebus.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/erebus/erebus.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(35.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.ReversePower.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),2),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.MOB_LOOTING),3),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_ASPECT),3),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),5))));

        bootstrap.register(HERCULES_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("hercules_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(2059292)
                                .textureName(prinegorerouse.prefix("model/named/custom/hercules/hercules_blade.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/hercules/hercules_blade.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(27.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.POWER_ARROWS),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_PROTECTION),1),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),3),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_ASPECT),2),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),3))));

        bootstrap.register(NIER_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("nier_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(132112255)
                                .textureName(prinegorerouse.prefix("model/named/custom/nier/nier.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/nier/nier.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(19.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.POWER_ARROWS),8),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),1),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),5))));

        bootstrap.register(NYX_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("nyx_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(21816532)
                                .textureName(prinegorerouse.prefix("model/named/custom/nyx/nyx.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/nyx/nyx.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(25.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(NrSlashArtRegistry.DIVINE_CROSS_SA.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Phantom.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),5),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),3),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.POWER_ARROWS),10),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),5))));

        bootstrap.register(PROTOGENOI_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("protogenoi_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(20832144)
                                .textureName(prinegorerouse.prefix("model/named/custom/protogenoi/protogenoi.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/protogenoi/protogenoi.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(60.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(NrSlashArtRegistry.COSMIC_LINE.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.AbsolutePower.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Back.getId())
                                .maxDamage(100)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),10),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING),10),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.MOB_LOOTING),10),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_ASPECT),2),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.BLOCK_FORTUNE),8),
                                new EnchantmentDefinition(getEnchantmentID(Enchantments.BANE_OF_ARTHROPODS),8))));

        bootstrap.register(TARTARUS_BLADE,
                new SlashBladeDefinition(prinegorerouse.prefix("tartarus_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(2384444)
                                .textureName(prinegorerouse.prefix("model/named/custom/tartarus/tartarus.png"))
                                .modelName(prinegorerouse.prefix("model/named/custom/tartarus/tartarus.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(35.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(NrSlashArtRegistry.BURNING_FIRE_SA.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.Porgatory.getId())
                                .addSpecialEffect(NrSpecialEffectsRegistry.ReversePower.getId())
                                .maxDamage(50)
                                .build(),
                        List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SHARPNESS),10))));
    }



    private static ResourceKey<SlashBladeDefinition> register(String id) {
        ResourceKey<SlashBladeDefinition> loc = ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY,
                prinegorerouse.prefix(id));
        return loc;
    }
    private static ResourceLocation getEnchantmentID(Enchantment enchantment){
        return ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
    }
}
