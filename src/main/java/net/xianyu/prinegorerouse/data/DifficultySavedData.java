package net.xianyu.prinegorerouse.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 以“世界存档数据（SavedData）”的形式持久化武器合成难度，替代原先的 toml 配置文件。
 *
 * <p>每个存档各自记忆自己的难度：新创建的存档默认使用 {@link WeaponSystem#DEFAULT_DIFFICULTY}（=5），
 * 随世界一同保存 / 加载，完全不依赖任何配置文件。所有维度共用主世界的这一份数据。
 */
public class DifficultySavedData extends SavedData {
    private static final String DATA_NAME = "prinegorerouse_difficulty";
    private static final String TAG_DIFFICULTY = "difficulty";

    private int difficulty = WeaponSystem.DEFAULT_DIFFICULTY;

    public DifficultySavedData() {
    }

    public static DifficultySavedData load(CompoundTag tag) {
        DifficultySavedData data = new DifficultySavedData();
        int stored = tag.getInt(TAG_DIFFICULTY);
        // 缺省或非法值回退到默认难度 5
        data.difficulty = stored >= 1 ? stored : WeaponSystem.DEFAULT_DIFFICULTY;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(TAG_DIFFICULTY, difficulty);
        return tag;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = Math.max(1, difficulty);
        setDirty();
    }

    /** 从主世界读取（所有维度共用同一份）；不存在时自动以默认难度创建。 */
    public static DifficultySavedData get(ServerLevel level) {
        // 优先用主世界的存储；若主世界尚未就绪（例：主世界自身首次加载事件中）则回退到当前维度。
        ServerLevel target = level.getServer().overworld();
        if (target == null) target = level;
        return target.getDataStorage().computeIfAbsent(
                DifficultySavedData::load, DifficultySavedData::new, DATA_NAME);
    }
}
