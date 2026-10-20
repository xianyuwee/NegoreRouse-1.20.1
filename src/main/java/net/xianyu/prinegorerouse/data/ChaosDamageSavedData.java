package net.xianyu.prinegorerouse.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 卡俄斯（CHAOS_BLADE）面板攻击力的“跟随存档”随机值持久化。
 *
 * <p>与武器合成难度（{@link DifficultySavedData}）同为世界存档级 {@link SavedData}：每个存档各自惰性生成一次、
 * 随世界一同保存 / 加载，之后固定不变（无法修改、不随重新合成而重随），所有维度共用主世界这一份。
 *
 * <p>两条彼此独立的随机：
 * <ul>
 *   <li><b>正常合成</b>的卡俄斯：面板攻击力在 {@code [10,100]} 之间，保留两位小数；</li>
 *   <li><b>生存转化</b>（前三把刀按概率变卡俄斯）得到的卡俄斯：面板攻击力在 {@code (1,10)} 之间，保留两位小数。</li>
 * </ul>
 */
public class ChaosDamageSavedData extends SavedData {
    private static final String DATA_NAME = "prinegorerouse_chaos_damage";
    private static final String TAG_CRAFTED = "craftedDamage";
    private static final String TAG_CONVERTED = "convertedDamage";

    /** 合法值恒 &gt; 1，故用 &le; 0 作为“尚未生成”的哨兵，首次访问时惰性生成并落盘。 */
    private float craftedDamage = 0F;
    private float convertedDamage = 0F;

    public ChaosDamageSavedData() {
    }

    public static ChaosDamageSavedData load(CompoundTag tag) {
        ChaosDamageSavedData data = new ChaosDamageSavedData();
        data.craftedDamage = tag.getFloat(TAG_CRAFTED);
        data.convertedDamage = tag.getFloat(TAG_CONVERTED);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putFloat(TAG_CRAFTED, craftedDamage);
        tag.putFloat(TAG_CONVERTED, convertedDamage);
        return tag;
    }

    /** 正常合成卡俄斯的攻击力：[10,100]，两位小数；本存档首次访问时生成，之后固定。 */
    public float crafted() {
        if (craftedDamage <= 0F) {
            craftedDamage = (float) round2(10.0 + ThreadLocalRandom.current().nextDouble() * 90.0);
            setDirty();
        }
        return craftedDamage;
    }

    /** 生存转化卡俄斯的攻击力：(1,10)，两位小数；本存档首次访问时生成，之后固定。 */
    public float converted() {
        if (convertedDamage <= 0F) {
            double v;
            // 严格落在开区间 (1,10)：两位小数四舍五入后仍要保证 >1 且 <10
            do {
                v = round2(1.0 + ThreadLocalRandom.current().nextDouble() * 9.0);
            } while (v <= 1.0 || v >= 10.0);
            convertedDamage = (float) v;
            setDirty();
        }
        return convertedDamage;
    }

    /** 四舍五入保留两位小数。 */
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** 从主世界读取（所有维度共用同一份）；不存在时自动创建（攻击力值惰性生成）。 */
    public static ChaosDamageSavedData get(ServerLevel level) {
        ServerLevel target = level.getServer().overworld();
        if (target == null) target = level;
        return target.getDataStorage().computeIfAbsent(
                ChaosDamageSavedData::load, ChaosDamageSavedData::new, DATA_NAME);
    }
}
