package com.xulai.gearswap;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GearPresetMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 200;
    public static final int IMAGE_HEIGHT = 168;

    public static final int PRESET_X = 49;
    public static final int SLOT_SIZE = 18;
    public static final int ROW_Y = 18;
    public static final int WEAPON_X = PRESET_X;
    public static final int OFFHAND_X = PRESET_X + SLOT_SIZE;
    public static final int ARMOR_X = PRESET_X + 2 * SLOT_SIZE;
    public static final int PRESET_LEFT = PRESET_X - 1;
    public static final int PRESET_RIGHT = PRESET_X + GearPresetData.SLOTS_PER_PRESET * SLOT_SIZE;
    public static final int ROW_GAP = 10;
    public static final int LABEL_RIGHT = PRESET_LEFT - ROW_GAP;
    public static final int KEYCAP_X = PRESET_RIGHT + ROW_GAP;
    public static final int KEYCAP_Y_OFFSET = 3;
    public static final int KEYCAP_WIDTH = 22;
    public static final int KEYCAP_HEIGHT = 12;

    public static final int PRESET_SLOTS = GearPresetData.TOTAL_SLOTS;
    public static final int INV_ROW_Y = 85;
    public static final int HOTBAR_Y = 143;

    private final GearPresetContainer container;
    private final Player owner;
    private final DataSlot activeSlot;

    public GearPresetMenu(int windowId, Inventory playerInventory) {
        super(ModMenus.GEAR_PRESETS.get(), windowId);
        this.owner = playerInventory.player;
        this.container = new GearPresetContainer(this.owner);

        for (int preset = 0; preset < GearPresetData.PRESET_COUNT; preset++) {
            int rowY = ROW_Y + preset * SLOT_SIZE;
            for (int i = 0; i < GearPresetData.SLOTS_PER_PRESET; i++) {
                int index = GearPresetData.index(preset, i);
                boolean offhand = i == GearPresetData.SLOT_OFFHAND;
                this.addSlot(new PresetSlot(this.container, index, PRESET_X + i * SLOT_SIZE, rowY,
                        this.owner, GearSlots.equipmentFor(i), offhand, GearSlots.iconFor(i)));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + col + row * 9, 8 + col * 18, INV_ROW_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }

        this.activeSlot = new DataSlot() {
            @Override
            public int get() {
                return GearCapabilities.presets(owner).getActive();
            }

            @Override
            public void set(int value) {
                GearCapabilities.presets(owner).setActive(value);
            }
        };
        this.addDataSlot(this.activeSlot);
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        if (stack.getCount() > 1 && startIndex < PRESET_SLOTS) {
            ItemStack single = stack.copy();
            single.setCount(1);
            if (super.moveItemStackTo(single, startIndex, endIndex, reverseDirection)) {
                stack.shrink(1);
                return true;
            }
            return false;
        }
        return super.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
    }

    public int getActivePreset() {
        return this.activeSlot.get();
    }

    public Player owner() {
        return this.owner;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < PRESET_SLOTS) {
                if (!this.moveItemStackTo(stack, PRESET_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, PRESET_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
