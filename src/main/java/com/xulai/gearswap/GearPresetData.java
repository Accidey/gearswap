package com.xulai.gearswap;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

public class GearPresetData implements INBTSerializable<CompoundTag> {
    public static final int PRESET_COUNT = 3;
    public static final int SLOT_WEAPON = 0;
    public static final int SLOT_OFFHAND = 1;
    public static final int SLOT_ARMOR_START = 2;
    public static final int ARMOR_COUNT = 4;
    public static final int EQUIPMENT_COUNT = 1 + ARMOR_COUNT;
    public static final int SLOTS_PER_PRESET = 1 + EQUIPMENT_COUNT;
    public static final int TOTAL_SLOTS = PRESET_COUNT * SLOTS_PER_PRESET;

    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_ORIGIN = "Origin";
    private static final String TAG_WEAPON_ITEM = "WeaponItem";
    private static final String TAG_WEAPON_SLOT = "WeaponSlot";

    private NonNullList<ItemStack> items = emptyList();
    private int active = -1;
    private final int[] origin = new int[SLOTS_PER_PRESET];
    private String weaponItem = "";
    private int weaponSlot = -1;

    public GearPresetData() {
        for (int i = 0; i < this.origin.length; i++) {
            this.origin[i] = -1;
        }
    }

    private static NonNullList<ItemStack> emptyList() {
        return NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    }

    public static int index(int preset, int slot) {
        return preset * SLOTS_PER_PRESET + slot;
    }

    public ItemStack get(int index) {
        return index >= 0 && index < TOTAL_SLOTS ? this.items.get(index) : ItemStack.EMPTY;
    }

    public void set(int index, ItemStack stack) {
        if (index >= 0 && index < TOTAL_SLOTS) {
            this.items.set(index, stack);
        }
    }

    public List<ItemStack> items() {
        return this.items;
    }

    public int getActive() {
        return this.active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public int getOrigin(int bodySlot) {
        return bodySlot >= 0 && bodySlot < this.origin.length ? this.origin[bodySlot] : -1;
    }

    public void setOrigin(int bodySlot, int preset) {
        if (bodySlot >= 0 && bodySlot < this.origin.length) {
            this.origin[bodySlot] = preset;
        }
    }

    public String getWeaponItem() {
        return this.weaponItem;
    }

    public int getWeaponSlot() {
        return this.weaponSlot;
    }

    public void setOutWeapon(String itemId, int slot) {
        this.weaponItem = itemId == null ? "" : itemId;
        this.weaponSlot = slot;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt(TAG_ACTIVE, this.active);
        tag.putIntArray(TAG_ORIGIN, this.origin);
        tag.putString(TAG_WEAPON_ITEM, this.weaponItem);
        tag.putInt(TAG_WEAPON_SLOT, this.weaponSlot);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.items = emptyList();
        ContainerHelper.loadAllItems(nbt, this.items);
        this.active = nbt.getInt(TAG_ACTIVE);
        int[] stored = nbt.getIntArray(TAG_ORIGIN);
        for (int i = 0; i < this.origin.length; i++) {
            this.origin[i] = i < stored.length ? stored[i] : -1;
        }
        this.weaponItem = nbt.getString(TAG_WEAPON_ITEM);
        this.weaponSlot = nbt.contains(TAG_WEAPON_SLOT) ? nbt.getInt(TAG_WEAPON_SLOT) : -1;
    }
}
