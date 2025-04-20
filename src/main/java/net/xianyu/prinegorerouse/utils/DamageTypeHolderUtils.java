package net.xianyu.prinegorerouse.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public class DamageTypeHolderUtils {
    public static Holder<DamageType> getHolder(Level level, String id) {
        ResourceKey<DamageType> key = ResourceKey.create(
                Registries.DAMAGE_TYPE,
                new ResourceLocation(id)
        );
        return level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
    }
}
