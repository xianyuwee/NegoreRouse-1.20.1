package net.xianyu.prinegorerouse.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NRConfig {
    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.BooleanValue TIME_CAN_CHANGE;
    public static ForgeConfigSpec.BooleanValue OFFHAND_CAN_ACTIVE;

    // ===== 负向神力（ReversePower）debuff 相关配置 =====
    // 造成伤害时施加到玩家身上的随机 debuff 持续时间（tick，100=5s）。默认值。可 config。
    public static ForgeConfigSpec.IntValue REVERSE_DEBUFF_DURATION;
    // 随机 debuff 的等级（amplifier，0=等级1）。
    public static ForgeConfigSpec.IntValue REVERSE_DEBUFF_AMPLIFIER;
    // 黑名单：这些效果 id 不参与抽取与转移（不会进 buff 池）。
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> REVERSE_DEBUFF_BLACKLIST;
    // 白名单：这些效果 id 可存在于玩家身上，但转移时不会被发射成幻影剑（留在玩家身上）。
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> REVERSE_DEBUFF_WHITELIST;

    // ===== 特效（SpecialEffect）时间素激活相关配置 =====
    // 每个特效独立配置：以特效 id（namespace:path 的 path 部分）为键。
    public static final Map<String, ForgeConfigSpec.LongValue> SE_COST = new HashMap<>();
    public static final Map<String, ForgeConfigSpec.EnumValue<AquaSEMode>> SE_MODE = new HashMap<>();
    public static final Map<String, ForgeConfigSpec.IntValue> SE_HITS = new HashMap<>();

    // ===== 时间素（AquaTime）背包虚拟格子相关配置 =====
    // 是否启用时间素系统
    public static ForgeConfigSpec.BooleanValue AQUA_ENABLE;
    // 是否在 HUD 常驻显示时间素格子（背包界面内始终显示）
    public static ForgeConfigSpec.BooleanValue AQUA_SHOW_HUD;
    // 白名单：仅这些物品 id 可以被存入虚拟格子并转化为时间素
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> AQUA_WHITELIST;
    // 每个白名单物品转化成的时间素数量（乘以物品堆叠数）
    public static ForgeConfigSpec.IntValue AQUA_PER_ITEM;
    // 每分钟（1200 tick）每把背包内的刀自动注入的时间素数量
    public static ForgeConfigSpec.IntValue AQUA_FILL_PER_MINUTE;
    // 半程点：达到该数量时格子内流体显示为 50%（存储上限无限，满度按 amount/(amount+半程点) 渐近）
    public static ForgeConfigSpec.LongValue AQUA_HALF_POINT;

    // ===== CHAOS（卡俄斯吞噬特效）相关配置 =====
    public static ForgeConfigSpec.LongValue CHAOS_K;
    public static ForgeConfigSpec.IntValue CHAOS_MAX_STAGE;
    public static ForgeConfigSpec.DoubleValue CHAOS_DMG_PER_STAGE;
    public static ForgeConfigSpec.DoubleValue CHAOS_VOID_PER_STAGE;
    public static ForgeConfigSpec.IntValue CHAOS_ROLL_SOUL;
    public static ForgeConfigSpec.IntValue CHAOS_ROLL_KILL;
    public static ForgeConfigSpec.IntValue CHAOS_ROLL_DMG;
    // 卡俄斯自发转化：玩家进入世界后获得的前三把刀按概率变为卡俄斯（每存档至多一次）
    public static ForgeConfigSpec.BooleanValue CHAOS_CONVERT_ENABLE;
    public static ForgeConfigSpec.IntValue CHAOS_CHANCE_1;
    public static ForgeConfigSpec.IntValue CHAOS_CHANCE_2;
    public static ForgeConfigSpec.IntValue CHAOS_CHANCE_3;

    public NRConfig() {
    }

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("General Settings").push("general");
        TIME_CAN_CHANGE = COMMON_BUILDER.comment(new String[]{"Determines whether the SlashArt 'DivineCrossSA' can change the time","if true,day and night will change when you use it."}).define("time_can_change",true);
        OFFHAND_CAN_ACTIVE = COMMON_BUILDER.comment(new String[]{"Determine whether SpecialEffects will be active when blades in offhand","if true, the SpecialEffects will be active in Offhand."}).define("offhand_can_active",true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("ReversePower Settings").push("reverse_power");
        REVERSE_DEBUFF_DURATION = COMMON_BUILDER.comment("Duration (ticks) of the random debuff ReversePower inflicts on the player when dealing damage. 100 = 5s.")
                .defineInRange("debuff_duration", 100, 1, Integer.MAX_VALUE);
        REVERSE_DEBUFF_AMPLIFIER = COMMON_BUILDER.comment("Amplifier (0 = level 1) of the random debuff ReversePower inflicts.")
                .defineInRange("debuff_amplifier", 0, 0, 255);
        REVERSE_DEBUFF_BLACKLIST = COMMON_BUILDER.comment("Effect ids (namespace:path) excluded from ReversePower's random debuff draw and transfer (never enter the buff pool).")
                .defineListAllowEmpty("debuff_blacklist", List.of("minecraft:instant_damage", "minecraft:wither"), o -> o instanceof String);
        REVERSE_DEBUFF_WHITELIST = COMMON_BUILDER.comment("Effect ids (namespace:path) that may stay on the player but are NEVER launched out as phantom swords during transfer.")
                .defineListAllowEmpty("debuff_whitelist", List.of(), o -> o instanceof String);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("AquaTime Settings").push("aquatime");
        AQUA_ENABLE = COMMON_BUILDER.comment("Enable the AquaTime virtual inventory slot.").define("enable", true);
        AQUA_SHOW_HUD = COMMON_BUILDER.comment("Also show the AquaTime slot as a HUD overlay outside the inventory screen.").define("show_hud", true);
        AQUA_WHITELIST = COMMON_BUILDER.comment("Whitelisted item ids (namespace:path) that can be deposited into the AquaTime slot.")
                .defineListAllowEmpty("whitelist", List.of("minecraft:diamond", "minecraft:gold_ingot"), o -> o instanceof String);
        AQUA_PER_ITEM = COMMON_BUILDER.comment("AquaTime gained per deposited whitelisted item (multiplied by item count).")
                .defineInRange("aqua_per_item", 2400, 0, Integer.MAX_VALUE);
        AQUA_FILL_PER_MINUTE = COMMON_BUILDER.comment("AquaTime auto-injected every minute per blade in the player's inventory.")
                .defineInRange("fill_per_minute", 1200, 0, Integer.MAX_VALUE);
        AQUA_HALF_POINT = COMMON_BUILDER.comment("Amount at which the fluid gauge reads exactly 50%. Infinite storage approaches 100%.")
                .defineInRange("half_point", 720000L, 1L, Long.MAX_VALUE);
        COMMON_BUILDER.pop();

        // 每个特效的默认成本（时间素量）：由原经验需求 * 100 推导，最低 100。
        Map<String, Long> defaults = new LinkedHashMap<>();
        defaults.put("oracle", 100L);
        defaults.put("back", 9000L);
        defaults.put("absolute_power", 5000L);
        defaults.put("reverse_power", 3500L);
        defaults.put("eternity", 20000L);
        defaults.put("clear", 100L);
        defaults.put("porgatory", 100L);
        defaults.put("fate", 3000L);
        defaults.put("empty", 100L);
        defaults.put("phantom", 100L);

        COMMON_BUILDER.comment("SpecialEffect AquaTime Settings").push("specialeffect");
        for (Map.Entry<String, Long> e : defaults.entrySet()) {
            String se = e.getKey();
            long cost = e.getValue();
            COMMON_BUILDER.push(se);
            SE_MODE.put(se, COMMON_BUILDER.comment("AquaTime consumption mode for special effect '" + se + "'.")
                    .defineEnum("mode", AquaSEMode.ONE_TIME));
            SE_COST.put(se, COMMON_BUILDER.comment("AquaTime cost for special effect '" + se + "'. "
                            + "ONE_TIME: deducted once at activation; drain modes: deducted per interval.")
                    .defineInRange("cost", cost, 0L, Long.MAX_VALUE));
            SE_HITS.put(se, COMMON_BUILDER.comment("For PER_HITS mode: how many hits before one deduction.")
                    .defineInRange("hits", 10, 1, Integer.MAX_VALUE));
            COMMON_BUILDER.pop();
        }
        COMMON_BUILDER.pop();

        // CHAOS 卡俄斯吞噬特效：SE 解锁费用 = i * k * 10000^(n-1)；阶段伤害与虚空伤害按吞噬量数量级。
        COMMON_BUILDER.comment("Chaos SpecialEffect Settings").push("chaos");
        CHAOS_K = COMMON_BUILDER.comment("Universal conversion constant k in the Chaos SE cost formula i*k*10000^(n-1).")
                .defineInRange("convert_k", 1L, 0L, Long.MAX_VALUE);
        CHAOS_MAX_STAGE = COMMON_BUILDER.comment("Highest reachable Chaos stage (stage advances each order of magnitude of devoured AquaTime).")
                .defineInRange("max_stage", 5, 0, 1000);
        CHAOS_DMG_PER_STAGE = COMMON_BUILDER.comment("Final damage increase per Chaos stage (0.2 = +20%).")
                .defineInRange("dmg_per_stage", 0.2D, 0.0D, 100.0D);
        CHAOS_VOID_PER_STAGE = COMMON_BUILDER.comment("Extra void (out_of_world) damage ratio per Chaos stage (0.2 = 20% of melee damage).")
                .defineInRange("void_per_stage", 0.2D, 0.0D, 100.0D);
        CHAOS_ROLL_SOUL = COMMON_BUILDER.comment("Proud Soul gain when Chaos devours AquaTime (roll weight 2).")
                .defineInRange("roll_soul", 1000, 0, Integer.MAX_VALUE);
        CHAOS_ROLL_KILL = COMMON_BUILDER.comment("Kill count gain when Chaos devours AquaTime (roll weight 2).")
                .defineInRange("roll_kill", 100, 0, Integer.MAX_VALUE);
        CHAOS_ROLL_DMG = COMMON_BUILDER.comment("Base attack modifier gain when Chaos devours AquaTime (roll weight 1).")
                .defineInRange("roll_dmg", 5, 0, Integer.MAX_VALUE);
        CHAOS_CONVERT_ENABLE = COMMON_BUILDER.comment("Enable: the first three blades a player obtains after entering a world each have a chance to become a Chaos blade (at most once per save).")
                .define("convert_enable", true);
        CHAOS_CHANCE_1 = COMMON_BUILDER.comment("Percent chance (0-100) for the 1st obtained blade to become Chaos.")
                .defineInRange("convert_chance_1", 30, 0, 100);
        CHAOS_CHANCE_2 = COMMON_BUILDER.comment("Percent chance (0-100) for the 2nd obtained blade to become Chaos.")
                .defineInRange("convert_chance_2", 50, 0, 100);
        CHAOS_CHANCE_3 = COMMON_BUILDER.comment("Percent chance (0-100) for the 3rd obtained blade to become Chaos.")
                .defineInRange("convert_chance_3", 70, 0, 100);
        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    /** 取某特效的时间素成本；无配置项时返回 0。参数可为完整 id 或 path。 */
    public static long seCost(String id) {
        ForgeConfigSpec.LongValue v = SE_COST.get(strip(id));
        return v == null ? 0L : v.get();
    }

    /** 取某特效的消耗模式；无配置项时返回 ONE_TIME。 */
    public static AquaSEMode seMode(String id) {
        ForgeConfigSpec.EnumValue<AquaSEMode> v = SE_MODE.get(strip(id));
        return v == null ? AquaSEMode.ONE_TIME : v.get();
    }

    /** 取某特效 PER_HITS 模式的命中间隔；无配置项时返回 10。 */
    public static int seHits(String id) {
        ForgeConfigSpec.IntValue v = SE_HITS.get(strip(id));
        return v == null ? 10 : v.get();
    }

    /** 去掉 namespace 前缀，只保留 path 部分作为配置键。 */
    private static String strip(String id) {
        int i = id.indexOf(':');
        return i >= 0 ? id.substring(i + 1) : id;
    }

    // ===== CHAOS getter =====
    public static long chaosK() {
        return CHAOS_K.get();
    }

    public static int chaosMaxStage() {
        return CHAOS_MAX_STAGE.get();
    }

    public static double chaosDmgPerStage() {
        return CHAOS_DMG_PER_STAGE.get();
    }

    public static double chaosVoidPerStage() {
        return CHAOS_VOID_PER_STAGE.get();
    }

    public static int chaosRollSoul() {
        return CHAOS_ROLL_SOUL.get();
    }

    public static int chaosRollKill() {
        return CHAOS_ROLL_KILL.get();
    }

    public static int chaosRollDmg() {
        return CHAOS_ROLL_DMG.get();
    }

    public static boolean chaosConvertEnabled() {
        return CHAOS_CONVERT_ENABLE.get();
    }

    /** 第 n 把（1..3）获得刀的转化概率（百分比）；超出范围返回 0。 */
    public static int chaosChance(int n) {
        switch (n) {
            case 1:
                return CHAOS_CHANCE_1.get();
            case 2:
                return CHAOS_CHANCE_2.get();
            case 3:
                return CHAOS_CHANCE_3.get();
            default:
                return 0;
        }
    }
}
