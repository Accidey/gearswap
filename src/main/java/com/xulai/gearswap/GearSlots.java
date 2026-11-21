package com.xulai.gearswap;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;

public final class GearSlots {
    private GearSlots() {}

    public static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public static final EquipmentSlot[] BODY = {
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public static final Identifier[] ARMOR_ICON = {
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    public static final Identifier OFFHAND_ICON = InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;

    public static final int WEAPON_ICON_U = 0;
    public static final int WEAPON_ICON_V = 180;

    public static Identifier iconFor(int bodySlot) {
        if (bodySlot == GearPresetData.SLOT_OFFHAND) {
            return OFFHAND_ICON;
        }
        if (isArmorSlot(bodySlot)) {
            return ARMOR_ICON[bodySlot - GearPresetData.SLOT_ARMOR_START];
        }
        return null;
    }

    public static boolean isArmorSlot(int bodySlot) {
        return bodySlot >= GearPresetData.SLOT_ARMOR_START
                && bodySlot < GearPresetData.SLOT_ARMOR_START + GearPresetData.ARMOR_COUNT;
    }

    public static boolean isEquipmentSlot(int bodySlot) {
        return isArmorSlot(bodySlot);
    }

    public static EquipmentSlot equipmentFor(int bodySlot) {
        return isArmorSlot(bodySlot) ? ARMOR[bodySlot - GearPresetData.SLOT_ARMOR_START] : null;
    }
}
