package com.xulai.gearswap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * 预设槽位与原版装备部位/图标的对照表。
 *
 * <p>套内下标：0 = 主手，1 = 副手，2..5 = 头盔/胸甲/护腿/靴子。</p>
 */
public final class GearSlots {
    private GearSlots() {}

    /** 预设槽 2..5 对应的盔甲部位（顺序与槽位下标一致）。 */
    public static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    /**
     * 按「套内槽位下标」索引的部位表：下标 1 = 副手，2..5 = 四件盔甲。
     * 下标 0（主手）与 1（副手）都没有装备限制，表里放的是占位值。
     */
    public static final EquipmentSlot[] BODY = {
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    /** 原版「空装备槽」图标，直接复用让预设格在为空时显示原版剪影。 */
    public static final ResourceLocation[] ARMOR_ICON = {
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    /** 副手的原版空槽图标（盾牌剪影）。 */
    public static final ResourceLocation OFFHAND_ICON = InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;

    /** 槽位 0（主手）在原版中没有对应的空槽图标，由 GUI 自绘提示（贴图内 (0,180) 的剑形图案）。 */
    public static final int WEAPON_ICON_U = 0;
    public static final int WEAPON_ICON_V = 180;

    /** 按「套内槽位下标」取对应的原版空槽图标；主手返回 null（由 GUI 自绘）。 */
    public static ResourceLocation iconFor(int bodySlot) {
        if (bodySlot == GearPresetData.SLOT_OFFHAND) {
            return OFFHAND_ICON;
        }
        if (isArmorSlot(bodySlot)) {
            return ARMOR_ICON[bodySlot - GearPresetData.SLOT_ARMOR_START];
        }
        return null;
    }

    /** 该套内槽位是不是「盔甲格」（严格只接受对应部位的装备）。 */
    public static boolean isArmorSlot(int bodySlot) {
        return bodySlot >= GearPresetData.SLOT_ARMOR_START
                && bodySlot < GearPresetData.SLOT_ARMOR_START + GearPresetData.ARMOR_COUNT;
    }

    /** 该套内槽位是不是「装备槽」。主手格与副手格都接受任意物品，不算。 */
    public static boolean isEquipmentSlot(int bodySlot) {
        return isArmorSlot(bodySlot);
    }

    /** 该盔甲格对应的装备部位；主手 / 副手返回 null。 */
    public static EquipmentSlot equipmentFor(int bodySlot) {
        return isArmorSlot(bodySlot) ? ARMOR[bodySlot - GearPresetData.SLOT_ARMOR_START] : null;
    }
}
