package net.xianyu.prinegorerouse.client.back;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 纯 OBJ 解析器：按建模时的 {@code handle}/{@code blade} 分组反推刀镡（tsuba）位置与刀身长度。
 * <p>约定（本 mod 所有 NR 刀都遵守）：刀身沿 -X 延伸到刀尖，柄与身在 +X 端交界，该交界面即刀镡。
 * 因此刀镡点 = {@code (handle.maxX, handle 的 Y 中心, handle 的 Z 中心)}；刀长 = {@code blade.maxX - blade.minX}。</p>
 * <p>不依赖任何 Minecraft 类，便于离线测试；解析失败或缺分组时返回 {@code null}（调用方据此回退）。</p>
 */
public final class ObjGuardParser {

    /** 刀镡点（模型空间）与刀身长度。 */
    public record Guard(float gx, float gy, float gz, float len) {
    }

    private ObjGuardParser() {
    }

    /** 解析 OBJ 流；无 {@code handle} 或 {@code blade} 分组、或解析异常时返回 null（不抛出）。 */
    public static Guard parse(InputStream in) {
        try {
            return doParse(in);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static Guard doParse(InputStream in) throws IOException {
        List<float[]> verts = new ArrayList<>();
        Map<String, Set<Integer>> groups = new HashMap<>();
        String cur = "_";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                if (line.startsWith("v ")) {
                    String[] p = line.substring(2).trim().split("\\s+");
                    if (p.length >= 3) {
                        verts.add(new float[]{Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                    }
                } else if (line.startsWith("g ")) {
                    String name = line.substring(2).trim().split("\\s+")[0];
                    cur = name;
                    groups.computeIfAbsent(name, k -> new HashSet<>());
                } else if (line.startsWith("f ")) {
                    Set<Integer> bucket = groups.computeIfAbsent(cur, k -> new HashSet<>());
                    int nv = verts.size();
                    for (String tok : line.substring(2).trim().split("\\s+")) {
                        String idxStr = tok.split("/")[0];
                        if (idxStr.isEmpty()) {
                            continue;
                        }
                        int idx;
                        try {
                            idx = Integer.parseInt(idxStr);
                        } catch (NumberFormatException e) {
                            continue;
                        }
                        if (idx < 0) {
                            idx = nv + idx + 1; // 负索引相对当前已定义顶点数
                        }
                        if (idx >= 1 && idx <= nv) {
                            bucket.add(idx);
                        }
                    }
                }
            }
        }

        Set<Integer> handle = groups.get("handle");
        Set<Integer> blade = groups.get("blade");
        if (handle == null || handle.isEmpty() || blade == null || blade.isEmpty()) {
            return null;
        }

        float[] hbox = bounds(handle, verts);   // {minX,maxX,minY,maxY,minZ,maxZ}
        float[] bbox = bounds(blade, verts);
        float gx = hbox[1];                       // 刀镡 X = 柄靠身一侧的交界面
        float gy = (hbox[2] + hbox[3]) * 0.5F;
        float gz = (hbox[4] + hbox[5]) * 0.5F;
        float len = bbox[1] - bbox[0];            // 刀身长度（供参考/归一）
        return new Guard(gx, gy, gz, len);
    }

    /** 一组顶点索引的包围盒：{minX,maxX,minY,maxY,minZ,maxZ}。 */
    private static float[] bounds(Set<Integer> idxSet, List<float[]> verts) {
        float minx = Float.MAX_VALUE, maxx = -Float.MAX_VALUE;
        float miny = Float.MAX_VALUE, maxy = -Float.MAX_VALUE;
        float minz = Float.MAX_VALUE, maxz = -Float.MAX_VALUE;
        for (int idx : idxSet) {
            float[] v = verts.get(idx - 1);
            if (v[0] < minx) minx = v[0];
            if (v[0] > maxx) maxx = v[0];
            if (v[1] < miny) miny = v[1];
            if (v[1] > maxy) maxy = v[1];
            if (v[2] < minz) minz = v[2];
            if (v[2] > maxz) maxz = v[2];
        }
        return new float[]{minx, maxx, miny, maxy, minz, maxz};
    }
}
