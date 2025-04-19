package net.xianyu.prinegorerouse.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class NRConfig {
    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.BooleanValue TIME_CAN_CHANGE;
    public static ForgeConfigSpec.BooleanValue OFFHAND_CAN_ACTIVE;

    public NRConfig() {
    }

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("General Settings").push("general");
        TIME_CAN_CHANGE = COMMON_BUILDER.comment(new String[]{"Determines whether the SlashArt 'DivineCrossSA' can change the time","if true,day and night will change when you use it."}).define("time_can_change",true);
        OFFHAND_CAN_ACTIVE = COMMON_BUILDER.comment(new String[]{"Determine whether SpecialEffects will be active when blades in offhand","if true, the SpecialEffects will be active in Offhand."}).define("offhand_can_active",true);


        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();
    }
}
