package net.xianyu.prinegorerouse.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

/**
 * 客户端配置：时间素 HUD 格子的位置。位置是纯本地视觉偏好，且要能被玩家在游戏内拖拽写入，
 * 因此放在 CLIENT 配置（prinegorerouse-client.toml），而不是 COMMON。
 * <p>约定：{@code hud_x}/{@code hud_y} 为格子左上角的缩放像素坐标；-1 表示“自动”（贴右下角）。</p>
 */
public class NRClientConfig {
    public static ForgeConfigSpec CLIENT_CONFIG;
    public static ForgeConfigSpec.IntValue AQUA_HUD_X;
    public static ForgeConfigSpec.IntValue AQUA_HUD_Y;

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
        AQUA_HUD_X = BUILDER.comment("HUD slot left X in scaled pixels; -1 = auto (dock bottom-right).")
                .defineInRange("hud_x", -1, -1, 1_000_000);
        AQUA_HUD_Y = BUILDER.comment("HUD slot top Y in scaled pixels; -1 = auto (dock bottom-right).")
                .defineInRange("hud_y", -1, -1, 1_000_000);
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
