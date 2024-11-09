package net.xianyu.prinegorerouse.registry;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.specialattack.BurningFireSA;
import net.xianyu.prinegorerouse.specialattack.DivineCrossSA;
import net.xianyu.prinegorerouse.specialattack.MagneticStormSword;
import net.xianyu.prinegorerouse.specialattack.Zenith12th;

import java.util.Objects;


public class NrComboStateRegistry {
    public static final DeferredRegister<ComboState> NR_COMBO_STATE;
    public static final RegistryObject<ComboState> ZENITH12TH;
    public static final RegistryObject<ComboState> ZENITH12TH_END;
    public static final RegistryObject<ComboState> STORM_SWORDS;
    public static final RegistryObject<ComboState> STORM_SWORDS_END;
    public static final RegistryObject<ComboState> DIVINE_CROSS_SA;
    public static final RegistryObject<ComboState> DIVINE_CROSS_SA_END;
    public static final RegistryObject<ComboState> BURNING_FIRE_SA;
    public static final RegistryObject<ComboState> BURNING_FIRE_SA_END;

    public NrComboStateRegistry() {
    }

    static {
        NR_COMBO_STATE = DeferredRegister.create(ComboState.REGISTRY_KEY,"prinegorerouse");
        DeferredRegister<ComboState> var1000 = NR_COMBO_STATE;

        ComboState.Builder var1002 = ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50).motionLoc(DefaultResources.ExMotionLocation).next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> {
            return SlashBlade.prefix("none");
        })).nextOfTimeout((entity) -> {
            return prinegorerouse.prefix("zenith12th_end");
        }).addTickAction(ComboState.TimeLineTickAction.getBuilder().put(2, (entityIn) -> {
            AttackManager.doSlash(entityIn, -50F, Vec3.ZERO, false, false, 2.0F);
        }).put(3, (entityIn) -> {
            Zenith12th.doSlash(entityIn, false, 10.0F);
        }).build()).addHitEffect(StunManager::setStun);
        Objects.requireNonNull(var1002);
        ZENITH12TH = var1000.register("zenith12th", var1002::build);

        var1000 = NR_COMBO_STATE;
        var1002 = ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50).motionLoc(DefaultResources.ExMotionLocation).next((entity) -> {
            return SlashBlade.prefix("none");
        }).nextOfTimeout((entity) -> {
            return SlashBlade.prefix("none");
        }).addTickAction(ComboState.TimeLineTickAction.getBuilder().put(0,AttackManager::playQuickSheathSoundAction).build()).releaseAction(ComboState::releaseActionQuickCharge);
        Objects.requireNonNull(var1002);
        ZENITH12TH_END = var1000.register("zenith12th_end", var1002::build);

        var1000 = NR_COMBO_STATE;
        var1002 = ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50).motionLoc(DefaultResources.ExMotionLocation).next(ComboState.TimeoutNext.buildFromFrame(15, (entity) -> {
            return SlashBlade.prefix("none");
        })).nextOfTimeout((entity) -> {
            return prinegorerouse.prefix("magnetic_storm_sword_end");
        }).addTickAction(ComboState.TimeLineTickAction.getBuilder().put(2, (entityIn) -> {
            AttackManager.doSlash(entityIn, -30F, Vec3.ZERO, false, false, 2.0F);
        }).put(3, (entityIn) -> {
            MagneticStormSword.doSlash(entityIn, false, 8.0F);
        }).build()).addHitEffect(StunManager::setStun);
        Objects.requireNonNull(var1002);
        STORM_SWORDS = var1000.register("magnetic_storm_sword", var1002::build);

        var1000 = NR_COMBO_STATE;
        var1002 = ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50).motionLoc(DefaultResources.ExMotionLocation).next((entity) -> {
            return SlashBlade.prefix("none");
        }).nextOfTimeout((entity) -> {
            return SlashBlade.prefix("none");
        }).addTickAction(ComboState.TimeLineTickAction.getBuilder().put(0,AttackManager::playQuickSheathSoundAction).build()).releaseAction(ComboState::releaseActionQuickCharge);
        Objects.requireNonNull(var1002);
        STORM_SWORDS_END = var1000.register("magnetic_storm_sword_end", var1002::build);

        var1000 = NR_COMBO_STATE;
        var1002 = ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50).motionLoc(DefaultResources.ExMotionLocation).next(ComboState.TimeoutNext.buildFromFrame(15, (entity) ->{
            return SlashBlade.prefix("none");
        })).nextOfTimeout((entity) -> {
            return prinegorerouse.prefix("divine_cross_sa_end");
        }).addTickAction(ComboState.TimeLineTickAction.getBuilder().put(2, (entityIn) -> {
            AttackManager.doSlash(entityIn, -80.0F, Vec3.ZERO,false, false, 0.1);
        }).put(3, (entityIn) -> {
            DivineCrossSA.doSlash(entityIn, 0.0F, 15, Vec3.ZERO, false, true, 20.0, 3.0F);
        }).build()).addHitEffect(StunManager::setStun);
        Objects.requireNonNull(var1002);
        DIVINE_CROSS_SA = var1000.register("divine_cross_sa", var1002::build);

        var1000 = NR_COMBO_STATE;
        var1002 = ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50).motionLoc(DefaultResources.ExMotionLocation).next((entity) -> {
            return SlashBlade.prefix("none");
        }).nextOfTimeout((entity) -> {
            return SlashBlade.prefix("none");
        }).addTickAction(ComboState.TimeLineTickAction.getBuilder().put(0, AttackManager::playQuickSheathSoundAction).build()).releaseAction(ComboState::releaseActionQuickCharge);
        Objects.requireNonNull(var1002);
        DIVINE_CROSS_SA_END = var1000.register("divine_cross_sa_end", var1002::build);

        var1000 = NR_COMBO_STATE;
        var1002 = ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50).motionLoc(DefaultResources.ExMotionLocation).next((entity) -> {
            return SlashBlade.prefix("none");
        }).nextOfTimeout((entity) -> {
            return prinegorerouse.prefix("burning_fire_sa_end");
        }).addTickAction(ComboState.TimeLineTickAction.getBuilder().put(2, (entityIn) -> {
            AttackManager.doSlash(entityIn, -30.0F, Vec3.ZERO, true, false, 0.1F);
        }).put(3,(entityIn) -> {
            BurningFireSA.doSlash(entityIn, Vec3.ZERO,false, 20, 2.0F);
        }).build()).addHitEffect(StunManager::setStun);
        Objects.requireNonNull(var1002);
        BURNING_FIRE_SA = var1000.register("burning_fire_sa", var1002::build);

        var1000 = NR_COMBO_STATE;
        var1002 = ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50).motionLoc(DefaultResources.ExMotionLocation).next((entity) -> {
            return SlashBlade.prefix("none");
        }).nextOfTimeout((entity) -> {
            return SlashBlade.prefix("none");
        }).addTickAction(ComboState.TimeLineTickAction.getBuilder().put(0, AttackManager::playQuickSheathSoundAction).build()).releaseAction(ComboState::releaseActionQuickCharge);
        Objects.requireNonNull(var1002);
        BURNING_FIRE_SA_END = var1000.register("burning_fire_sa_end", var1002::build);
    }
}
