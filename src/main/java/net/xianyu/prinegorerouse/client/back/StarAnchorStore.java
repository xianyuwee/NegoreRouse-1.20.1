package net.xianyu.prinegorerouse.client.back;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.config.NRClientConfig;
import net.xianyu.prinegorerouse.client.back.ObjGuardParser.Guard;
import net.xianyu.prinegorerouse.prinegorerouse;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 回溯星星锚点存储：扫描本 mod 的刀 OBJ 得到每把刀的模型空间刀镡点，缓存为带 mod 版本的全局 JSON，
 * 运行时按当前刀的 {@code RenderDefinition.getModelName()}（即 obj 资源 RL）查表，映射成视线偏移。
 * <p>版本未变时只读文件、不重扫；命中不到（无分组 / 非本 mod 刀 / 关开关）时 {@link #offsetFor} 返回
 * {@code null}，由 {@code BackStarField} 回退到固定偏移。</p>
 */
public final class StarAnchorStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "prinegorerouse_star_anchors.json";
    private static final String SCAN_PREFIX = "model/named/custom";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** obj 资源 RL 字符串 -> 刀镡点。reload 时整体替换（volatile 保证渲染线程可见）。 */
    private static volatile Map<String, Guard> table = Map.of();

    private StarAnchorStore() {
    }

    // ===== 扫描 / 缓存 =====

    /** 在资源加载线程调用：版本一致则读缓存，否则扫描 OBJ 并回写缓存。 */
    public static synchronized void reload(ResourceManager rm) {
        DEFINITIONS = null; // 资源重载（切世界/F3+T）时失效定义快照
        String ver = modVersion();
        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        JsonObject root = readJson(file);
        if (root != null && ver.equals(str(root, "modVersion"))) {
            Map<String, Guard> loaded = parseAnchors(root);
            if (loaded != null) {
                table = loaded;
                LOGGER.debug("[BackStar] 锚点缓存命中，版本 {}，共 {} 条", ver, loaded.size());
                return;
            }
        }
        Map<String, Guard> scanned = scan(rm);
        table = scanned;
        writeJson(file, ver, scanned);
        LOGGER.info("[BackStar] 已扫描 NR 刀 OBJ 刀镡锚点，共 {} 条", scanned.size());
    }

    private static Map<String, Guard> scan(ResourceManager rm) {
        Map<String, Guard> out = new HashMap<>();
        Map<ResourceLocation, Resource> resources = rm.listResources(SCAN_PREFIX,
                loc -> prinegorerouse.MOD_ID.equals(loc.getNamespace()) && loc.getPath().endsWith(".obj"));
        for (Map.Entry<ResourceLocation, Resource> e : resources.entrySet()) {
            try (InputStream in = e.getValue().open()) {
                Guard g = ObjGuardParser.parse(in);
                if (g != null) {
                    out.put(e.getKey().toString(), g);
                }
            } catch (Exception ex) {
                LOGGER.warn("[BackStar] 解析 OBJ 失败：{}", e.getKey(), ex);
            }
        }
        return out;
    }

    // ===== 运行时查询 + 映射 =====

    /**
     * 当前刀的星星视线偏移 {@code {forward, side, up}}（side 已按主手约定为负=向左）；
     * 未启用 / 未命中表时返回 {@code null}，调用方回退固定偏移。
     */
    public static double[] offsetFor(ItemStack blade, HolderLookup.Provider registries) {
        if (blade == null || blade.isEmpty() || !NRClientConfig.BACK_STAR_ANCHOR_ENABLE.get()) {
            return null;
        }
        ResourceLocation rl = resolveModelRL(blade, registries);
        if (rl == null) {
            return null;
        }
        Guard g = table.get(rl.toString());
        if (g == null) {
            return null;
        }
        double[] ref = dbl(NRClientConfig.BACK_STAR_GUARD_REF.get(), 3, 0.0);
        double[] base = dbl(NRClientConfig.BACK_STAR_BASE.get(), 3, 0.0);
        double[] unit = dbl(NRClientConfig.BACK_STAR_UNIT_SCALE.get(), 3, 0.0);
        int[] src = ints(NRClientConfig.BACK_STAR_AXIS_SRC.get(), 3, 0);
        int[] sign = ints(NRClientConfig.BACK_STAR_AXIS_SIGN.get(), 3, 1);
        double[] md = {g.gx() - ref[0], g.gy() - ref[1], g.gz() - ref[2]};
        double dF = unit[0] * sign[0] * md[clamp(src[0])];
        double dS = unit[1] * sign[1] * md[clamp(src[1])];
        double dU = unit[2] * sign[2] * md[clamp(src[2])];
        return new double[]{base[0] + dF, base[1] + dS, base[2] + dU};
    }

    /** 取当前刀的 obj 资源 RL：优先运行时 state 字段，为空则按翻译键/物品 id 回退定义注册表。 */
    private static ResourceLocation resolveModelRL(ItemStack blade, HolderLookup.Provider registries) {
        ISlashBladeState st = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve().orElse(null);
        if (st != null) {
            ResourceLocation m = st.getModel().orElse(null);
            if (m != null) {
                return m;
            }
        }
        if (registries == null) {
            return null;
        }
        String tk = st == null ? "" : st.getTranslationKey();
        String item = itemId(blade);
        SlashBladeDefinition byItem = null;
        List<SlashBladeDefinition> defs = definitionsOf(registries);
        for (SlashBladeDefinition def : defs) {
            if (!tk.isEmpty() && tk.equals(def.getTranslationKey())) {
                return modelName(def);
            }
            if (byItem == null && item != null && item.equals(def.getItemName().toString())) {
                byItem = def;
            }
        }
        return byItem == null ? null : modelName(byItem);
    }

    /** 定义注册表运行期不变；缓存快照避免每帧全量遍历并分配 List。 */
    private static volatile List<SlashBladeDefinition> DEFINITIONS;

    private static List<SlashBladeDefinition> definitionsOf(HolderLookup.Provider registries) {
        List<SlashBladeDefinition> d = DEFINITIONS;
        if (d == null) {
            d = SlashBlade.getSlashBladeDefinitionRegistry(registries)
                    .listElements().map(h -> h.value()).toList();
            DEFINITIONS = d;
        }
        return d;
    }

    private static ResourceLocation modelName(SlashBladeDefinition def) {
        return def.getRenderDefinition() == null ? null : def.getRenderDefinition().getModelName();
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? null : key.toString();
    }

    // ===== JSON 读写 =====

    private static JsonObject readJson(Path file) {
        try {
            if (!Files.exists(file)) {
                return null;
            }
            JsonElement el = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeJson(Path file, String ver, Map<String, Guard> data) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("modVersion", ver);
            JsonObject anchors = new JsonObject();
            for (Map.Entry<String, Guard> e : data.entrySet()) {
                JsonObject o = new JsonObject();
                Guard g = e.getValue();
                o.addProperty("gx", g.gx());
                o.addProperty("gy", g.gy());
                o.addProperty("gz", g.gz());
                o.addProperty("len", g.len());
                anchors.add(e.getKey(), o);
            }
            root.add("anchors", anchors);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warn("[BackStar] 写锚点缓存失败", e);
        }
    }

    private static Map<String, Guard> parseAnchors(JsonObject root) {
        try {
            JsonElement a = root.get("anchors");
            if (a == null || !a.isJsonObject()) {
                return null;
            }
            Map<String, Guard> out = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : a.getAsJsonObject().entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                out.put(e.getKey(), new Guard(
                        o.get("gx").getAsFloat(), o.get("gy").getAsFloat(),
                        o.get("gz").getAsFloat(), o.get("len").getAsFloat()));
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    // ===== 小工具 =====

    private static String modVersion() {
        return ModList.get().getModContainerById(prinegorerouse.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null ? null : e.getAsString();
    }

    private static int clamp(int axis) {
        return axis < 0 || axis > 2 ? 0 : axis;
    }

    private static double[] dbl(List<? extends Number> list, int n, double def) {
        double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            r[i] = (list != null && i < list.size() && list.get(i) != null) ? list.get(i).doubleValue() : def;
        }
        return r;
    }

    private static int[] ints(List<? extends Number> list, int n, int def) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = (list != null && i < list.size() && list.get(i) != null) ? list.get(i).intValue() : def;
        }
        return r;
    }
}
