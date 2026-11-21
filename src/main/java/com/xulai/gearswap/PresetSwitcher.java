package com.xulai.gearswap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.network.PacketDistributor;

import com.xulai.gearswap.network.SyncPresetPayload;

public final class PresetSwitcher {
    private PresetSwitcher() {}

    private static final int BODY_SLOTS = GearPresetData.SLOTS_PER_PRESET;
    private static final int DEST_SKIP = -2;
    private static final int DEST_KEEP = -3;
    private static final int DEST_BACKPACK = -1;

    private static final int INVENTORY_SLOTS = 36;
    private static final int[] BACKPACK_ORDER = buildOrder();

    private static int[] buildOrder() {
        int[] order = new int[INVENTORY_SLOTS];
        int cursor = 0;
        for (int slot = 9; slot < INVENTORY_SLOTS; slot++) {
            order[cursor++] = slot;
        }
        for (int slot = 0; slot < 9; slot++) {
            order[cursor++] = slot;
        }
        return order;
    }

    public static boolean swap(ServerPlayer player, int target) {
        if (target < 0 || target >= GearPresetData.PRESET_COUNT) {
            return false;
        }

        GearPresetData data = player.getData(GearSwap.GEAR_PRESETS);
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        boolean creative = player.isCreative();

        int weaponOrigin = data.getOrigin(GearPresetData.SLOT_WEAPON);
        boolean trackingWeapon = weaponOrigin >= 0 && !data.getWeaponItem().isEmpty();
        int weaponSlotFound = trackingWeapon ? locateOutWeapon(data, inventory) : -1;
        boolean weaponRelocating = false;
        int weaponHome = DEST_BACKPACK;
        if (weaponSlotFound >= 0 && weaponSlotFound != selected) {
            int homeIndex = GearPresetData.index(weaponOrigin, GearPresetData.SLOT_WEAPON);
            if (weaponOrigin == target || data.get(homeIndex).isEmpty()) {
                weaponRelocating = true;
                weaponHome = homeIndex;
            }
        }
        boolean weaponLost = trackingWeapon && weaponSlotFound < 0;

        int[] destination = new int[BODY_SLOTS];
        List<ItemStack> toBackpack = new ArrayList<>();
        boolean blocked = false;
        boolean changed = false;

        for (int bodySlot = 0; bodySlot < BODY_SLOTS; bodySlot++) {
            int targetIndex = GearPresetData.index(target, bodySlot);
            ItemStack worn = getEquipped(player, bodySlot);

            if (isHandSlot(bodySlot) && data.get(targetIndex).isEmpty()
                    && data.getOrigin(bodySlot) < 0) {
                destination[bodySlot] = DEST_KEEP;
                continue;
            }

            if (bodySlot == GearPresetData.SLOT_WEAPON && weaponRelocating) {
                destination[bodySlot] = weaponHome;
                changed = true;
                continue;
            }

            if (isArmorSlot(bodySlot) && !worn.isEmpty() && !creative
                    && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                destination[bodySlot] = DEST_SKIP;
                blocked = true;
                continue;
            }

            destination[bodySlot] = bodySlot == GearPresetData.SLOT_WEAPON && weaponLost
                    ? DEST_BACKPACK
                    : findHome(data, bodySlot, target, targetIndex);
            if (destination[bodySlot] == DEST_BACKPACK && !worn.isEmpty()) {
                toBackpack.add(worn);
            }
            if (!worn.isEmpty() || !data.get(targetIndex).isEmpty()) {
                changed = true;
            }
        }

        if (changed && !toBackpack.isEmpty() && !fitsInBackpack(inventory, toBackpack)) {
            player.sendOverlayMessage(Component.translatable("gearswap.message.backpack_full"));
            return false;
        }

        boolean equippedAnything = false;

        if (weaponRelocating) {
            int handSlot = inventory.getSelectedSlot();
            ItemStack handItem = inventory.getItem(handSlot);
            ItemStack locatedWeapon = inventory.getItem(weaponSlotFound);
            int targetIndex = GearPresetData.index(target, GearPresetData.SLOT_WEAPON);
            ItemStack targetStack = data.get(targetIndex);

            data.set(targetIndex, ItemStack.EMPTY);
            data.set(weaponHome, locatedWeapon.copy());
            inventory.setItem(weaponSlotFound, handItem.isEmpty() ? ItemStack.EMPTY : handItem.copy());
            equip(player, GearPresetData.SLOT_WEAPON, targetStack);
            data.setOrigin(GearPresetData.SLOT_WEAPON, targetStack.isEmpty() ? -1 : target);
            if (!targetStack.isEmpty()) {
                equippedAnything = true;
            }
        }

        for (int bodySlot = 0; bodySlot < BODY_SLOTS; bodySlot++) {
            if (bodySlot == GearPresetData.SLOT_WEAPON && weaponRelocating) {
                continue;
            }
            if (destination[bodySlot] == DEST_SKIP || destination[bodySlot] == DEST_KEEP) {
                continue;
            }

            int targetIndex = GearPresetData.index(target, bodySlot);
            ItemStack worn = getEquipped(player, bodySlot);
            ItemStack targetStack = data.get(targetIndex);

            data.set(targetIndex, ItemStack.EMPTY);
            if (!worn.isEmpty()) {
                if (destination[bodySlot] >= 0) {
                    data.set(destination[bodySlot], worn.copy());
                } else if (!insertIntoBackpack(inventory, worn)) {
                    GearSwap.LOGGER.warn("Failed to move {} into the backpack, keeping it in the preset", worn);
                    data.set(targetIndex, worn.copy());
                }
            }

            equip(player, bodySlot, targetStack);
            data.setOrigin(bodySlot, targetStack.isEmpty() ? -1 : target);
            if (!targetStack.isEmpty()) {
                equippedAnything = true;
            }
        }

        if (data.getOrigin(GearPresetData.SLOT_WEAPON) >= 0) {
            ItemStack hand = player.getMainHandItem();
            data.setOutWeapon(BuiltInRegistries.ITEM.getKey(hand.getItem()).toString(), inventory.getSelectedSlot());
        } else {
            data.setOutWeapon("", -1);
        }

        if (changed) {
            data.setActive(equippedAnything ? target : -1);
            syncNow(player);
            PacketDistributor.sendToPlayer(player, new SyncPresetPayload(data.getActive()));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(),
                    SoundSource.PLAYERS, 0.6F, 1.15F);
            player.sendOverlayMessage(Component.translatable(
                    equippedAnything ? "gearswap.message.switched" : "gearswap.message.cleared",
                    Component.translatable(presetNameKey(target))));
        } else if (blocked) {
            player.sendOverlayMessage(Component.translatable("gearswap.message.prevented"));
        } else {
            player.sendOverlayMessage(Component.translatable("gearswap.message.nothing",
                    Component.translatable(presetNameKey(target))));
        }
        return changed;
    }

    private static int locateOutWeapon(GearPresetData data, Inventory inventory) {
        Item expected = resolveItem(data.getWeaponItem());
        if (expected == null) {
            return -1;
        }
        int selected = inventory.getSelectedSlot();
        if (inventory.getItem(selected).is(expected)) {
            return selected;
        }
        int remembered = data.getWeaponSlot();
        if (remembered >= 0 && remembered < INVENTORY_SLOTS && inventory.getItem(remembered).is(expected)) {
            return remembered;
        }
        for (int slot = 0; slot < INVENTORY_SLOTS; slot++) {
            if (inventory.getItem(slot).is(expected)) {
                return slot;
            }
        }
        return -1;
    }

    private static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        Identifier key = Identifier.tryParse(itemId);
        return key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
    }

    private static int findHome(GearPresetData data, int bodySlot, int target, int targetIndex) {
        int origin = data.getOrigin(bodySlot);
        if (origin == target) {
            return targetIndex;
        }
        if (origin >= 0 && origin < GearPresetData.PRESET_COUNT) {
            int candidate = GearPresetData.index(origin, bodySlot);
            if (data.get(candidate).isEmpty()) {
                return candidate;
            }
        }
        return DEST_BACKPACK;
    }

    private static boolean isArmorSlot(int bodySlot) {
        return GearSlots.isArmorSlot(bodySlot);
    }

    private static boolean isHandSlot(int bodySlot) {
        return bodySlot == GearPresetData.SLOT_WEAPON || bodySlot == GearPresetData.SLOT_OFFHAND;
    }

    private static ItemStack getEquipped(ServerPlayer player, int bodySlot) {
        if (bodySlot == GearPresetData.SLOT_WEAPON) {
            return player.getMainHandItem();
        }
        if (bodySlot == GearPresetData.SLOT_OFFHAND) {
            return player.getOffhandItem();
        }
        EquipmentSlot slot = GearSlots.equipmentFor(bodySlot);
        return slot == null ? ItemStack.EMPTY : player.getItemBySlot(slot);
    }

    private static void equip(ServerPlayer player, int bodySlot, ItemStack stack) {
        if (bodySlot == GearPresetData.SLOT_WEAPON) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            return;
        }
        EquipmentSlot slot = bodySlot == GearPresetData.SLOT_OFFHAND
                ? EquipmentSlot.OFFHAND
                : GearSlots.equipmentFor(bodySlot);
        if (slot != null) {
            player.setItemSlot(slot, stack);
        }
    }

    private static boolean fitsInBackpack(Inventory inventory, List<ItemStack> stacks) {
        return insertAll(snapshot(inventory), stacks);
    }

    private static boolean insertIntoBackpack(Inventory inventory, ItemStack stack) {
        ItemStack[] slots = snapshot(inventory);
        if (!insertAll(slots, Collections.singletonList(stack))) {
            return false;
        }
        writeBack(inventory, slots);
        return true;
    }

    private static ItemStack[] snapshot(Inventory inventory) {
        ItemStack[] slots = new ItemStack[INVENTORY_SLOTS];
        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            slots[i] = inventory.getItem(i).copy();
        }
        return slots;
    }

    private static void writeBack(Inventory inventory, ItemStack[] slots) {
        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            inventory.setItem(i, slots[i]);
        }
    }

    private static boolean insertAll(ItemStack[] slots, List<ItemStack> stacks) {
        for (ItemStack source : stacks) {
            if (source.isEmpty()) {
                continue;
            }
            ItemStack remaining = source.copy();
            for (int index : BACKPACK_ORDER) {
                if (remaining.isEmpty()) {
                    break;
                }
                ItemStack existing = slots[index];
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                    int room = existing.getMaxStackSize() - existing.getCount();
                    if (room > 0) {
                        int move = Math.min(room, remaining.getCount());
                        slots[index] = existing.copyWithCount(existing.getCount() + move);
                        remaining.shrink(move);
                    }
                }
            }
            for (int index : BACKPACK_ORDER) {
                if (remaining.isEmpty()) {
                    break;
                }
                if (slots[index].isEmpty()) {
                    int move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    slots[index] = remaining.copyWithCount(move);
                    remaining.shrink(move);
                }
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void syncNow(ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.sendAllDataToRemote();
        }
    }

    public static String presetNameKey(int preset) {
        return "gearswap.preset." + (preset + 1);
    }
}
