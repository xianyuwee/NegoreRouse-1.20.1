package net.xianyu.prinegorerouse.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class NRConfig {
    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.BooleanValue TIME_CAN_CHANGE;
    public static ForgeConfigSpec.BooleanValue OFFHAND_CAN_ACTIVE;
    public static ForgeConfigSpec.BooleanValue DO_DEBUFF_WORK;
    public static ForgeConfigSpec.IntValue DIFFICULTY;

    public NRConfig() {
    }

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("General Settings").push("general");
        TIME_CAN_CHANGE = COMMON_BUILDER.comment(new String[]{"Determines whether the SlashArt 'DivineCrossSA' can change the time","if true,day and night will change when you use it."}).define("time_can_change",true);
        OFFHAND_CAN_ACTIVE = COMMON_BUILDER.comment(new String[]{"Determine whether SpecialEffects will be active when blades in offhand","if true, the SpecialEffects will be active in Offhand."}).define("offhand_can_active",true);
        DO_DEBUFF_WORK = COMMON_BUILDER.comment(new String[] {"Determines whether the debuff from SE can be active or not","if true, players will get the buffs from SE."}).define("do_debuff_work",true);
        DIFFICULTY = COMMON_BUILDER.comment(new String[]{"Determines the difficulty of crafting blades", "ranging zero to ten."}).defineInRange("difficulty", 5,0,10);

        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();
    }
}
