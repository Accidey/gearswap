package com.xulai.gearswap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class GearPresetProvider implements ICapabilitySerializable<CompoundTag> {
    private final GearPresetData backend = new GearPresetData();
    private final LazyOptional<GearPresetData> optional = LazyOptional.of(() -> this.backend);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == GearSwap.GEAR_PRESETS ? this.optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.backend.deserializeNBT(nbt);
    }

    public void invalidate() {
        this.optional.invalidate();
    }
}
