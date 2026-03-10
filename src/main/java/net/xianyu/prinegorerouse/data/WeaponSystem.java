package net.xianyu.prinegorerouse.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class WeaponSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static int difficulty = 1; // 默认难度1（基础值）

    // 存储不同武器的基础要求（难度1时的数值）
    private static final Map<String, WeaponRequirement> baseWeaponRequirements = new HashMap<>();
    // 存储实时计算后的要求（基础值 × 当前难度）
    private static final Map<String, WeaponRequirement> runtimeWeaponRequirements = new HashMap<>();

    // 设置难度并更新实时要求
    public static void setDifficulty(int newDifficulty) {
        if (newDifficulty < 1) newDifficulty = 1; // 保底为1
        difficulty = newDifficulty;
        LOGGER.info("武器系统难度已更新为: {}", difficulty);
        updateRuntimeWeaponRequirements(); // 重新计算实时要求
    }

    public static int getDifficulty() {
        return difficulty;
    }

    // 初始化基础要求（难度1的数值）
    public static void initialize() {
        // 基础值：难度1时的 荣耀值/杀敌数/锻造数
        baseWeaponRequirements.put("AEON_BLADE", new WeaponRequirement(10000, 1000, 10));
        baseWeaponRequirements.put("ANANKE_BLADE", new WeaponRequirement(5000, 500, 8));
        baseWeaponRequirements.put("ANTAUGE_BLADE", new WeaponRequirement(2000, 100, 1));
        baseWeaponRequirements.put("ARITEMIS_BLADE", new WeaponRequirement(800,50,1));
        baseWeaponRequirements.put("CHAOS_BLADE", new WeaponRequirement(1500, 150, 2));
        baseWeaponRequirements.put("CHRONOS_BLADE", new WeaponRequirement(1000, 100,1));
        baseWeaponRequirements.put("CHRONOSN_BLADE", new WeaponRequirement(8000, 800, 8));
        baseWeaponRequirements.put("EREBUS_BLADE", new WeaponRequirement(6000,600,5));
        baseWeaponRequirements.put("HERCULES_BLADE", new WeaponRequirement(800,50,1));
        baseWeaponRequirements.put("NIER_BLADE", new WeaponRequirement(800,50,1));
        baseWeaponRequirements.put("PROTOGENOI_BLADE", new WeaponRequirement(10000, 1000, 10));
        baseWeaponRequirements.put("TARTARUS_BLADE" , new WeaponRequirement(800,50,1));

        updateRuntimeWeaponRequirements(); // 初始化实时要求
    }

    // 根据当前难度更新实时要求
    private static void updateRuntimeWeaponRequirements() {
        runtimeWeaponRequirements.clear();
        for (Map.Entry<String, WeaponRequirement> entry : baseWeaponRequirements.entrySet()) {
            WeaponRequirement baseReq = entry.getValue();
            // 实时值 = 基础值 × 当前难度
            int proudSoul = baseReq.baseProudSoul * difficulty;
            int killCount = baseReq.baseKillCount * difficulty;
            int refineCount = baseReq.baseRefineCount * difficulty;
            runtimeWeaponRequirements.put(entry.getKey(), new WeaponRequirement(proudSoul, killCount, refineCount));
        }
        LOGGER.info("已更新所有武器的实时合成要求，共{}种武器", runtimeWeaponRequirements.size());
    }

    // 获取某武器的实时合成要求
    public static WeaponRequirement getRequirementForWeapon(String weaponId) {
        return runtimeWeaponRequirements.getOrDefault(weaponId, new WeaponRequirement(0, 0, 0));
    }

    // 武器要求模型（存储计算后的值，baseXXX仅用于初始化）
    public static class WeaponRequirement {
        private int baseProudSoul;
        private int baseKillCount;
        private int baseRefineCount;

        private int proudSoul;
        private int killCount;
        private int refineCount;

        // 初始化基础值（难度1）
        public WeaponRequirement(int baseProudSoul, int baseKillCount, int baseRefineCount) {
            this.baseProudSoul = baseProudSoul;
            this.baseKillCount = baseKillCount;
            this.baseRefineCount = baseRefineCount;
            // 初始化时直接赋值（难度1）
            this.proudSoul = baseProudSoul;
            this.killCount = baseKillCount;
            this.refineCount = baseRefineCount;
        }

        // 供外部获取实时值
        public int getProudSoul() {
            return proudSoul;
        }

        public int getKillCount() {
            return killCount;
        }

        public int getRefineCount() {
            return refineCount;
        }
    }
}