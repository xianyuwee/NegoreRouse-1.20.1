package net.xianyu.prinegorerouse.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.IEnergyStorage;

@AutoRegisterCapability // 用于自动注册能力(capbility)
public class FEBladeStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {
    // 当前存储的能量
    protected int energy;
    // 最大可存储的能量
    protected int capacity;

    // 每tick最大可接收的能量
    protected int maxReceive = 20000;

    // 每tick最大可提取的能量
    protected int maxExtract = 20000;

    // 充能消耗的能量
    protected int powerupExtract;

    // 待机时消耗的能量
    protected int standbyExtract;

    // 能量替换耐久显示(即无耐久设定)
    protected boolean energyDurability;

    // 是否正在启用
    protected boolean isPowered = false;


    public FEBladeStorage(int energy, int capacity, int powerupExtract, int standbyExtract, boolean energyDurability) {
        this.energy = energy;
        this.capacity = capacity;
        this.powerupExtract = powerupExtract;
        this.standbyExtract = standbyExtract;
        this.energyDurability = energyDurability;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive())
            return 0;

        int energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate)
            energy += energyReceived;
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract())
            return 0;

        int energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) {
            energy -= energyExtracted;
            if(energy <= 0) {
                this.setPowered(false);
            }
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }


    public void setMaxEnergyStored(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    public boolean isEnergyDurability() {
        return energyDurability;
    }

    public int getPowerupExtract() {
        return powerupExtract;
    }

    public void setPowerupExtract(int powerupExtract) {
        this.powerupExtract = powerupExtract;
    }

    public int getStandbyExtract() {
        return standbyExtract;
    }

    public void setStandbyExtract(int standbyExtract) {
        this.standbyExtract = standbyExtract;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", energy);
        tag.putInt("Capacity", capacity);
        tag.putInt("MaxReceive", maxReceive);
        tag.putInt("MaxExtract", maxExtract);
        tag.putInt("PowerupExtract", powerupExtract);
        tag.putInt("StandbyExtract", standbyExtract);
        tag.putBoolean("EnergyDurability", energyDurability);
        tag.putBoolean("isPowered", isPowered);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt != null) {
            energy = nbt.getInt("Energy");
            capacity = nbt.getInt("Capacity");
            maxReceive = nbt.getInt("MaxReceive");
            maxExtract = nbt.getInt("MaxExtract");
            powerupExtract = nbt.getInt("PowerupExtract");
            standbyExtract = nbt.getInt("StandbyExtract");
            energyDurability = nbt.getBoolean("EnergyDurability");
            isPowered = nbt.getBoolean("isPowered");
        }
    }

    public boolean isPowered() {
        return isPowered;
    }

    public void setPowered(boolean isPowered) {
        this.isPowered = isPowered;
    }
}