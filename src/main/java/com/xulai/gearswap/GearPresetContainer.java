package com.xulai.gearswap;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GearPresetContainer implements Container {
    private final Player owner;
    private final GearPresetData data;

    public GearPresetContainer(Player owner) {
        this.owner = owner;
        this.data = GearCapabilities.presets(owner);
    }

    public GearPresetData data() {
        return this.data;
    }

    @Override
    public int getContainerSize() {
        return GearPresetData.TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.data.items()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.data.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = this.data.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) {
            this.data.set(slot, ItemStack.EMPTY);
        }
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.data.get(slot);
        this.data.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.data.set(slot, stack);
    }

    @Override
    public void setChanged() {}

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isRemoved() && player.isAlive();
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < GearPresetData.TOTAL_SLOTS; i++) {
            this.data.set(i, ItemStack.EMPTY);
        }
    }

    public Player owner() {
        return this.owner;
    }
}
