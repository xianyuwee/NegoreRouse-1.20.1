package net.xianyu.prinegorerouse.energy;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.xianyu.prinegorerouse.energy.FEBladeStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FECapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    private final LazyOptional<ISlashBladeState> bladeState;
    private final LazyOptional<FEBladeStorage> energy;
    private final ItemStack stack;

    // 如果希望保留父提供者，可以添加这个字段，但本修改暂不引入
    // private final ICapabilityProvider parent;

    public FECapabilityProvider(ItemStack stack, int energy, int capacity, int powerupExtract, int standbyExtract, boolean energyDurability) {
        this.stack = stack;
        // 关键修改：直接创建新的 SlashBladeState，不再从 stack 获取能力，避免递归
        this.bladeState = LazyOptional.of(() -> new SlashBladeState(stack));
        this.energy = LazyOptional.of(() -> new FEBladeStorage(energy, capacity, powerupExtract, standbyExtract, energyDurability));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ItemSlashBlade.BLADESTATE) {
            return bladeState.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energy.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        bladeState.ifPresent(state -> tag.put("bladeState", state.serializeNBT()));
        energy.ifPresent(e -> tag.put("Energy", e.serializeNBT()));
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("bladeState")) {
            bladeState.ifPresent(state -> state.deserializeNBT(nbt.getCompound("bladeState")));
        }
        if (nbt.contains("Energy")) {
            energy.ifPresent(e -> e.deserializeNBT(nbt.getCompound("Energy")));
        }
    }

    public void invalidate() {
        bladeState.invalidate();
        energy.invalidate();
    }
}