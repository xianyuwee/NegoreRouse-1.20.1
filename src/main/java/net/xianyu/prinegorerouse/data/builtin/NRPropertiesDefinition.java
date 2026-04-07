package net.xianyu.prinegorerouse.data.builtin;

import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public class NRPropertiesDefinition {
    // 核心：持有原生 PropertiesDefinition 实例（唯一合法获取方式）
    private final PropertiesDefinition originalProps;
    // 新增：能源槽核心字段
    private final int maxEnergy;
    private final int energyPerUse;
    private final boolean enableEnergy;

    // 构造方法：封装原生实例 + 自定义能源字段
    private NRPropertiesDefinition(PropertiesDefinition originalProps,
                                   int maxEnergy,
                                   int energyPerUse,
                                   boolean enableEnergy) {
        this.originalProps = originalProps;
        this.maxEnergy = maxEnergy;
        this.energyPerUse = energyPerUse;
        this.enableEnergy = enableEnergy;
    }

    // ========== 转发原生 PropertiesDefinition 的所有 Getter（保证外部调用兼容） ==========
    public ResourceLocation getComboRoot() {
        return originalProps.getComboRoot();
    }

    public ResourceLocation getSpecialAttackType() {
        return originalProps.getSpecialAttackType();
    }

    public float getBaseAttackModifier() {
        return originalProps.getBaseAttackModifier();
    }

    public int getMaxDamage() {
        return originalProps.getMaxDamage();
    }

    public List<SwordType> getDefaultType() {
        return originalProps.getDefaultType();
    }

    public List<ResourceLocation> getSpecialEffects() {
        return originalProps.getSpecialEffects();
    }

    public boolean isUnbreakable() {
        return originalProps.isUnbreakable();
    }

    // ========== 新增能源字段的 Getter ==========
    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getEnergyPerUse() {
        return energyPerUse;
    }

    public boolean isEnableEnergy() {
        return enableEnergy;
    }

    // ========== 对外暴露原生 PropertiesDefinition 实例（适配 SlashBladeDefinition 构造） ==========
    public PropertiesDefinition getOriginalProps() {
        return originalProps;
    }

    // ===================== 扩展 Builder 类 =====================
    public static class NRBuilder {
        // 组合原生 Builder 实例（通过公开的 newInstance() 创建）
        private final PropertiesDefinition.Builder parentBuilder;
        // 能源配置字段
        private int maxEnergy = 0;
        private int energyPerUse = 0;
        private boolean enableEnergy = false;

        private NRBuilder() {
            this.parentBuilder = PropertiesDefinition.Builder.newInstance();
        }

        public static NRBuilder newNRInstance() {
            return new NRBuilder();
        }

        // ========== 新增能源配置的链式方法 ==========
        public NRBuilder enableEnergySlot(boolean enable) {
            this.enableEnergy = enable;
            return this;
        }

        public NRBuilder maxEnergy(int maxEnergy) {
            this.maxEnergy = maxEnergy;
            return this;
        }

        public NRBuilder energyPerUse(int energyPerUse) {
            this.energyPerUse = energyPerUse;
            return this;
        }

        // ========== 复用原生 Builder 的所有链式方法（返回子类 Builder） ==========
        public NRBuilder rootComboState(ResourceLocation comboRoot) {
            this.parentBuilder.rootComboState(comboRoot);
            return this;
        }

        public NRBuilder slashArtsType(ResourceLocation specialAttackType) {
            this.parentBuilder.slashArtsType(specialAttackType);
            return this;
        }

        public NRBuilder baseAttackModifier(float baseAttackModifier) {
            this.parentBuilder.baseAttackModifier(baseAttackModifier);
            return this;
        }

        public NRBuilder maxDamage(int maxDamage) {
            this.parentBuilder.maxDamage(maxDamage);
            return this;
        }

        public NRBuilder defaultSwordType(List<SwordType> defaultType) {
            this.parentBuilder.defaultSwordType(defaultType);
            return this;
        }

        public NRBuilder addSpecialEffect(ResourceLocation se) {
            this.parentBuilder.addSpecialEffect(se);
            return this;
        }

        public NRBuilder setUnbreakable(boolean unbreakable) {
            this.parentBuilder.setUnbreakable(unbreakable);
            return this;
        }

        // ========== 构建自定义 NRPropertiesDefinition ==========
        public NRPropertiesDefinition build() {
            // 先构建原生 PropertiesDefinition 实例
            PropertiesDefinition original = this.parentBuilder.build();
            // 封装为自定义实例
            return new NRPropertiesDefinition(
                    original,
                    this.maxEnergy,
                    this.energyPerUse,
                    this.enableEnergy
            );
        }
    }
}