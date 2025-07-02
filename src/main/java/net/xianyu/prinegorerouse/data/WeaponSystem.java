package net.xianyu.prinegorerouse.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class WeaponSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static int difficulty = 5; // 默认难度

    // 存储不同武器的要求
    private static final Map<String, WeaponRequirement> weaponRequirements = new HashMap<>();

    public static void setDifficulty(int newDifficulty) {
        difficulty = newDifficulty;
        LOGGER.info("武器系统难度已更新为: {}", difficulty);

        // 更新所有武器的要求
        updateWeaponRequirements();
    }

    public static int getDifficulty() {
        return difficulty;
    }

    public static void initialize() {
        // 初始化武器要求
        weaponRequirements.put("AEON_BLADE", new WeaponRequirement(10000, 1000, 10));
        weaponRequirements.put("ANANKE_BLADE", new WeaponRequirement(5000, 500, 8));
        weaponRequirements.put("ANTAUGE_BLADE", new WeaponRequirement(2000, 100, 1));
        weaponRequirements.put("ARITEMIS_BLADE", new WeaponRequirement(800,50,1));
        weaponRequirements.put("CHAOS_BLADE", new WeaponRequirement(1500, 150, 2));
        weaponRequirements.put("CHRONOS_BLADE", new WeaponRequirement(1000, 100,1));
        weaponRequirements.put("CHRONOSN_BLADE", new WeaponRequirement(8000, 800, 8));
        weaponRequirements.put("EREBUS_BLADE", new WeaponRequirement(6000,600,5));
        weaponRequirements.put("HERCULES_BLADE", new WeaponRequirement(800,50,1));
        weaponRequirements.put("NIER_BLADE", new WeaponRequirement(800,50,1));
        weaponRequirements.put("PROTOGENOI_BLADE", new WeaponRequirement(10000, 1000, 10));
        weaponRequirements.put("TARTARUS_BLADE" , new WeaponRequirement(800,50,1));

        updateWeaponRequirements();
    }

    private static void updateWeaponRequirements() {
        // 根据当前难度更新所有武器要求
        for (WeaponRequirement requirement : weaponRequirements.values()) {
            requirement.update(difficulty);
        }
    }

    public static WeaponRequirement getRequirementForWeapon(String weaponId) {
        return weaponRequirements.getOrDefault(weaponId, new WeaponRequirement(0, 0, 0));
    }

    public static class WeaponRequirement {
        private int baseProudSoul;
        private int baseKillCount;
        private int baseRefineCount;

        private int proudSoul;
        private int killCount;
        private int refineCount;

        public WeaponRequirement(int baseProudSoul, int baseKillCount, int baseRefineCount) {
            this.baseProudSoul = baseProudSoul;
            this.baseKillCount = baseKillCount;
            this.baseRefineCount = baseRefineCount;
            update(WeaponSystem.getDifficulty());
        }

        public void update(int difficulty) {
            this.proudSoul = baseProudSoul * difficulty;
            this.killCount = baseKillCount * difficulty;
            this.refineCount = baseRefineCount * difficulty;
        }

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