package com.xulai.gearswap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xulai.gearswap.network.SyncPresetPayload;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

/**
 * 换装核心逻辑。
 *
 * <p>切换到某套预设时，逐部位处理身上原有的装备：</p>
 * <ol>
 *   <li>它本来就是从某套预设拿出来的 → 放回**那套预设原来的格子**（不污染目标预设）；</li>
 *   <li>它不是预设里的东西（或原格已被占用）→ 放回玩家背包；</li>
 *   <li>背包放不下 → 整体取消，提示「背包已满，无法替换」；</li>
 *   <li>随后才穿上目标预设的装备，因此流程绝不会丢物品。</li>
 * </ol>
 *
 * <p>**手部例外**：目标预设的主手 / 副手格子如果是空的，而手上这件又**本来就不属于任何预设**
 * （origin == -1，比如随手拿着的镐子、火把），就让它原样留在手上 —— 既不塞进背包、也不做归位。
 * 否则「只配了盔甲的预设」一切换，就会把玩家手里正在用的工具丢进背包。
 * 反过来说，手上这件若是**别的预设**的装备（origin >= 0），仍按归位模型送回它自己的预设格；
 * 是**当前这套**的装备（origin == target）则送回目标格，也就是「再按一次同一套 = 卸下」照旧可用。
 * 盔甲格不适用此例外：空预设依旧能用来脱下盔甲（提示「已卸下」）。</p>
 *
 * <p>主手武器额外处理：玩家换快捷栏后手上就不是那把预设武器了（它还躺在快捷栏里），
 * 所以额外记录「当前应该在外面的预设武器」的物品 id 与所在格
 * （{@link GearPresetData#getWeaponItem()}），换装时把它从背包里找出来一起归位。</p>
 *
 * <p>副手（{@link GearPresetData#SLOT_OFFHAND}）与四件盔甲同样属于「装备槽」，
 * 走完全相同的归还 / 穿脱流程，武器之外的所有部位都会被一起替换。
 * 区别只在**格子**层面：副手格什么都能放，盔甲格才严格限部位。</p>
 */
public final class PresetSwitcher {
    private PresetSwitcher() {}

    /** 部位数量：主手 + 四件盔甲 + 副手。 */
    private static final int BODY_SLOTS = GearPresetData.SLOTS_PER_PRESET;
    /** 该部位不做任何变更（绑定诅咒）。 */
    private static final int DEST_SKIP = -2;
    /** 该部位保持原样（目标预设这一格空着，且手上这件不属于任何预设：别塞进背包）。 */
    private static final int DEST_KEEP = -3;
    /** 该部位的东西退回背包。 */
    private static final int DEST_BACKPACK = -1;

    /** 背包格数（不含盔甲格）：快捷栏 0-8 + 主背包 9-35。 */
    private static final int INVENTORY_SLOTS = 36;
    /** 优先塞主背包，再塞快捷栏，避免换装把快捷栏搅乱。 */
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

    /**
     * 切换到第 target 套预设。
     *
     * @return 是否真的发生了变更
     */
    public static boolean swap(ServerPlayer player, int target) {
        if (target < 0 || target >= GearPresetData.PRESET_COUNT) {
            return false;
        }

        GearPresetData data = player.getData(GearSwap.GEAR_PRESETS);
        Inventory inventory = player.getInventory();
        boolean creative = player.isCreative();

        // ---- 0. 主手武器：先把「当前应该在外面」的那把武器找出来 ----
        //    （手上这件若不属于任何预设，origin < 0 → trackingWeapon 为 false，
        //      下面的归位分支自然不会触发，无需为「手部例外」额外加判断）
        int weaponOrigin = data.getOrigin(GearPresetData.SLOT_WEAPON);
        boolean trackingWeapon = weaponOrigin >= 0 && !data.getWeaponItem().isEmpty();
        int weaponSlotFound = trackingWeapon ? locateOutWeapon(data, inventory) : -1;
        // 手上不是它（玩家换过快捷栏），但确实还在背包里 → 需要专门归位
        boolean weaponRelocating = false;
        int weaponHome = DEST_BACKPACK;
        if (weaponSlotFound >= 0 && weaponSlotFound != inventory.selected) {
            int homeIndex = GearPresetData.index(weaponOrigin, GearPresetData.SLOT_WEAPON);
            if (weaponOrigin == target || data.get(homeIndex).isEmpty()) {
                weaponRelocating = true;
                weaponHome = homeIndex;
            }
        }
        // 记录里说有把预设武器在外面，但背包里找不到（被丢掉了/放进箱子了）→ 手上这件不算预设的
        boolean weaponLost = trackingWeapon && weaponSlotFound < 0;

        // ---- 1. 规划：决定每件身上装备该去哪儿 ----
        int[] destination = new int[BODY_SLOTS];
        List<ItemStack> toBackpack = new ArrayList<>();
        boolean blocked = false;
        boolean changed = false;

        for (int bodySlot = 0; bodySlot < BODY_SLOTS; bodySlot++) {
            int targetIndex = GearPresetData.index(target, bodySlot);
            ItemStack worn = getEquipped(player, bodySlot);

            // 目标预设这一格是空的，而手上这件本来就不属于任何预设 → 原样留在手上。
            // 否则「没配武器的预设」一切换，就把玩家手里正在用的工具丢进背包了。
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

        // 背包塞不下就整体放弃，绝不半途而废
        if (changed && !toBackpack.isEmpty() && !fitsInBackpack(inventory, toBackpack)) {
            player.displayClientMessage(Component.translatable("gearswap.message.backpack_full"), true);
            return false;
        }

        // ---- 2. 应用 ----
        boolean equippedAnything = false;

        // 2a. 武器在背包别处：与手上物品对调位置后归位
        if (weaponRelocating) {
            int handSlot = inventory.selected;
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

        // 2b. 其余部位（以及武器就在手上时的常规流程）
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
                    // 理论上不会发生（前面已校验），兜底放回刚腾空的格子，保证不丢物品
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

        // ---- 3. 更新武器追踪并反馈 ----
        if (data.getOrigin(GearPresetData.SLOT_WEAPON) >= 0) {
            ItemStack hand = player.getMainHandItem();
            data.setOutWeapon(BuiltInRegistries.ITEM.getKey(hand.getItem()).toString(), inventory.selected);
        } else {
            data.setOutWeapon("", -1);
        }

        if (changed) {
            data.setActive(equippedAnything ? target : -1);
            syncNow(player);
            PacketDistributor.sendToPlayer(player, new SyncPresetPayload(data.getActive()));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(),
                    SoundSource.PLAYERS, 0.6F, 1.15F);
            player.displayClientMessage(Component.translatable(
                    equippedAnything ? "gearswap.message.switched" : "gearswap.message.cleared",
                    Component.translatable(presetNameKey(target))), true);
        } else if (blocked) {
            player.displayClientMessage(Component.translatable("gearswap.message.prevented"), true);
        } else {
            player.displayClientMessage(Component.translatable("gearswap.message.nothing",
                    Component.translatable(presetNameKey(target))), true);
        }
        return changed;
    }

    /** 在背包（含快捷栏）里找出当前应该在外面的那把预设武器所在的格子；找不到返回 -1。 */
    private static int locateOutWeapon(GearPresetData data, Inventory inventory) {
        Item expected = resolveItem(data.getWeaponItem());
        if (expected == null) {
            return -1;
        }
        // 手上拿着就是最常见情况
        if (inventory.getItem(inventory.selected).is(expected)) {
            return inventory.selected;
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
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        return key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
    }

    /** 决定某件身上装备该回哪个预设格：返回 DEST_BACKPACK 表示回背包。 */
    private static int findHome(GearPresetData data, int bodySlot, int target, int targetIndex) {
        int origin = data.getOrigin(bodySlot);
        if (origin == target) {
            // 同一套来回切：直接放回原位（目标格正好会被腾空）
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

    /** 只有盔甲格受绑定诅咒保护；主手 / 副手沿用原版行为。 */
    private static boolean isArmorSlot(int bodySlot) {
        return GearSlots.isArmorSlot(bodySlot);
    }

    /** 主手 / 副手：「目标预设为空则不动手上物品」这条例外只适用这两格。 */
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
        if (bodySlot == GearPresetData.SLOT_OFFHAND) {
            // setItemSlot(OFFHAND, ...) 会重算主手属性、还可能触发盾牌的冷却，
            // 只做搬运的话直接用副手槽自身的容器，语义更干净。
            player.getInventory().offhand.set(0, stack);
            return;
        }
        EquipmentSlot slot = GearSlots.equipmentFor(bodySlot);
        if (slot != null) {
            player.setItemSlot(slot, stack);
        }
    }

    // ---------------- 背包容量判断与放入 ----------------

    /** 只做模拟，不变更背包。 */
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

    /** 先并入同类堆叠、再占用空格；全部放下返回 true。 */
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

    /** 立刻把背包/当前打开的界面推给客户端，避免出现装备「看起来没换」的延迟。 */
    private static void syncNow(ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.sendAllDataToRemote();
        }
    }

    /** 预设名的翻译键。 */
    public static String presetNameKey(int preset) {
        return "gearswap.preset." + (preset + 1);
    }
}
