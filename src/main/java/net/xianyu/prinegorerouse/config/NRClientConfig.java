package net.xianyu.prinegorerouse.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

/**
 * 客户端配置：时间素格子的位置（HUD 常驻格子 + 背包内的虚拟格子）。位置是纯本地视觉偏好，
 * 且要能被玩家在游戏内拖拽写入，因此放在 CLIENT 配置（prinegorerouse-client.toml），而不是 COMMON。
 * <p>约定：</p>
 * <ul>
 *   <li>{@code hud_edge_x}/{@code hud_edge_y}：HUD 格子到屏幕<b>右/下边缘</b>的偏移（缩放像素），-1 = 自动（内置 4px 边距）。
 *       绑边而不是存绝对坐标，是为了换分辨率、全屏/窗口互换、改 GUI 缩放时格子仍跟着右下角走。</li>
 *   <li>{@code hud_x}/{@code hud_y}：旧版本的<b>绝对屏幕坐标</b>，已废弃，仅保留供 {@code AquaHudLayout} 一次性迁移
 *       （{@code hud_migrated} 标记完成后就不再读取）。</li>
 *   <li>{@code inv_x}/{@code inv_y}：背包虚拟格子相对<b>背包 GUI 左上角</b>的偏移（跟随 GUI 平移，不会被配方书错位），
 *       允许为负以停到面板外侧；因此“是否用内置默认位置”另由 {@code inv_auto} 布尔标记记录，不能用 -1 当哨兵。</li>
 * </ul>
 */
public class NRClientConfig {
    public static ForgeConfigSpec CLIENT_CONFIG;
    // HUD 格子：绑右/下边缘的偏移（-1 = 自动边距）
    public static ForgeConfigSpec.IntValue AQUA_HUD_EDGE_X;
    public static ForgeConfigSpec.IntValue AQUA_HUD_EDGE_Y;
    // 旧绝对坐标遗留键 + 迁移完成标记（迁移后 hud_x/hud_y 被写回 -1 并置 true）
    public static ForgeConfigSpec.IntValue AQUA_HUD_X;
    public static ForgeConfigSpec.IntValue AQUA_HUD_Y;
    public static ForgeConfigSpec.BooleanValue AQUA_HUD_MIGRATED;
    // 背包虚拟格子：默认位置开关 + GUI 相对偏移（偏移可负，故开关单独存）
    public static ForgeConfigSpec.BooleanValue AQUA_INV_AUTO;
    public static ForgeConfigSpec.IntValue AQUA_INV_X;
    public static ForgeConfigSpec.IntValue AQUA_INV_Y;

    // ===== 回溯（back）星星锚点：模型空间刀镡点 -> 视线偏移 的全局映射常数 =====
    /** 总开关：关闭则所有刀都走 BackStarField 的固定偏移回退逻辑。 */
    public static ForgeConfigSpec.BooleanValue BACK_STAR_ANCHOR_ENABLE;
    /** 标定基准刀的刀镡点（模型空间 {x,y,z}），默认 = 卡俄斯 chaos。 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Double>> BACK_STAR_GUARD_REF;
    /** 基准刀已对齐的视线偏移 {forward, side, up}；side 为负表示主手向刀身一侧（左）。 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Double>> BACK_STAR_BASE;
    /** 每轴增益（视线单位 / 模型单位）：{forward, side, up}。默认只让 side 随刀镡 X 微调。 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Double>> BACK_STAR_UNIT_SCALE;
    /** 每个视线偏移分量取哪个模型轴：{0=x,1=y,2=z}。 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> BACK_STAR_AXIS_SRC;
    /** 每个视线偏移分量的符号：{+1,-1}。 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> BACK_STAR_AXIS_SIGN;

    /** 注册后由 {@code ModConfigEvent} 回填，用于 {@link #save()} 落盘。 */
    private static ModConfig instance;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        BUILDER.comment("AquaTime HUD Settings").push("aquatime_hud");
        // 绑边偏移：-1 = 自动（由 AquaHudLayout 内置边距接管），>=0 = 距右/下边缘的像素数
        AQUA_HUD_EDGE_X = BUILDER.comment("HUD slot: distance from the RIGHT screen edge in scaled pixels; -1 = auto margin.")
                .defineInRange("hud_edge_x", -1, -1, 100_000);
        AQUA_HUD_EDGE_Y = BUILDER.comment("HUD slot: distance from the BOTTOM screen edge in scaled pixels; -1 = auto margin.")
                .defineInRange("hud_edge_y", -1, -1, 100_000);
        // 旧绝对坐标字段：保留只为读一次做迁移换算，迁移后恒为 -1，不会再影响显示
        AQUA_HUD_X = BUILDER.comment("[legacy] Old absolute HUD X; kept only for one-time migration to hud_edge_x, safe to ignore.")
                .defineInRange("hud_x", -1, -1, 1_000_000);
        AQUA_HUD_Y = BUILDER.comment("[legacy] Old absolute HUD Y; kept only for one-time migration to hud_edge_y, safe to ignore.")
                .defineInRange("hud_y", -1, -1, 1_000_000);
        AQUA_HUD_MIGRATED = BUILDER.comment("Set to true once the legacy hud_x/hud_y have been converted into hud_edge_x/hud_edge_y.")
                .define("hud_migrated", false);
        // 背包虚拟格子：坐标是相对 GUI 左上角的偏移，默认落在 2×2 合成区正下方的空带 (134, 62)，
        // 避开面板右缘那一带（活跃 buff 图标就是沿面板右上/右缘画下来的，旧默认值会与之重叠）。
        AQUA_INV_AUTO = BUILDER.comment("Inventory virtual slot: true = use the built-in default offset; false = use inv_x/inv_y below.")
                .define("inv_auto", true);
        AQUA_INV_X = BUILDER.comment("Inventory virtual slot X offset from the GUI left edge; may be negative to park it outside the panel.")
                .defineInRange("inv_x", 134, -64, 256);
        AQUA_INV_Y = BUILDER.comment("Inventory virtual slot Y offset from the GUI top edge; may be negative to park it outside the panel.")
                .defineInRange("inv_y", 62, -64, 256);
        BUILDER.pop();

        BUILDER.comment("Back-star anchor mapping (model-space guard point -> view-space offset)").push("back_star");
        BACK_STAR_ANCHOR_ENABLE = BUILDER.comment("Master switch; off = every blade uses the fixed fallback offset.")
                .define("anchor_enable", true);
        BACK_STAR_GUARD_REF = BUILDER.comment("Reference blade guard point in model space {x,y,z}; default = chaos_blade.")
                .defineList("guard_ref", List.of(80.6, 3.05, 0.0), e -> e instanceof Number);
        BACK_STAR_BASE = BUILDER.comment("Calibrated view offset {forward, side, up} of the reference blade; negative side = left (main hand).")
                .defineList("base", List.of(0.55, -0.30, 0.12), e -> e instanceof Number);
        BACK_STAR_UNIT_SCALE = BUILDER.comment("Per-axis gain (view units per model unit) {forward, side, up}. Default only nudges side by guard X.")
                .defineList("unit_scale", List.of(0.0, 0.002, 0.0), e -> e instanceof Number);
        BACK_STAR_AXIS_SRC = BUILDER.comment("Which model axis (0=x,1=y,2=z) feeds each view offset component {forward, side, up}.")
                .defineList("axis_src", List.of(0, 0, 0), e -> e instanceof Number);
        BACK_STAR_AXIS_SIGN = BUILDER.comment("Sign (+1/-1) applied to each view offset component {forward, side, up}.")
                .defineList("axis_sign", List.of(1, 1, 1), e -> e instanceof Number);
        BUILDER.pop();

        CLIENT_CONFIG = BUILDER.build();
    }

    /** 在主模组构造器里注册。 */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG);
    }

    /** 由 ModConfigEvent.Load 捕获 ModConfig 实例。 */
    public static void capture(ModConfig config) {
        if (config.getSpec() == CLIENT_CONFIG) {
            instance = config;
        }
    }

    /** 把当前内存值写回 prinegorerouse-client.toml。 */
    public static void save() {
        if (instance != null) {
            instance.save();
        }
    }
}
