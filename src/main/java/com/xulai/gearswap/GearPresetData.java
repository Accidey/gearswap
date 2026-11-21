package com.xulai.gearswap;

import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * 单名玩家的装备预设数据：3 套预设 × 6 个格子（主手 + 副手 + 四件盔甲）。
 *
 * <p>槽位编号规则：{@code preset * SLOTS_PER_PRESET + slot}，
 * 其中 slot 0 = 主手，1 = 副手，2..5 = 头盔/胸甲/护腿/靴子
 * （与 {@link GearSlots#ARMOR} 顺序一致）。副手紧跟主手，界面上也是相邻两格。</p>
 *
 * <p>副手格接受**任意**物品（原版副手本就什么都能拿，盾牌只是常见用法）；
 * 4 个盔甲格才是严格的装备槽，只接受对应部位的装备。</p>
 */
public class GearPresetData implements INBTSerializable<CompoundTag> {
    /** 预设套数。 */
    public static final int PRESET_COUNT = 3;
    /** 单套预设内：主手武器的槽位下标。 */
    public static final int SLOT_WEAPON = 0;
    /** 单套预设内：副手的槽位下标（紧跟主手）。 */
    public static final int SLOT_OFFHAND = 1;
    /** 单套预设内：第一件盔甲的槽位下标。 */
    public static final int SLOT_ARMOR_START = 2;
    /** 单套预设内：盔甲件数。 */
    public static final int ARMOR_COUNT = 4;
    /** 单套预设内：装备槽（副手 + 盔甲）占据的槽位数。 */
    public static final int EQUIPMENT_COUNT = 1 + ARMOR_COUNT;
    /** 单套预设的槽位总数。 */
    public static final int SLOTS_PER_PRESET = 1 + EQUIPMENT_COUNT;
    /** 全部预设的槽位总数。 */
    public static final int TOTAL_SLOTS = PRESET_COUNT * SLOTS_PER_PRESET;

    private static final String TAG_ACTIVE = "Active";
    /** 每个部位当前身上装备的「出处预设」：-1 表示不是从预设里拿的。 */
    private static final String TAG_ORIGIN = "Origin";
    /** 现在「应该在外面」的那把预设武器是哪个物品（物品 id），空串表示没有。 */
    private static final String TAG_WEAPON_ITEM = "WeaponItem";
    /** 装上那把武器时它所在的格子（快捷栏下标），用于快速认领。 */
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

    /** 把「第几套预设 + 套内第几格」换算成全局槽位下标。 */
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

    /** 最近一次换装使用的预设下标，-1 表示没有生效中的预设（仅用于界面高亮）。 */
    public int getActive() {
        return this.active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    /** 第 bodySlot 件装备当前来自哪套预设；-1 表示不是预设里的东西。 */
    public int getOrigin(int bodySlot) {
        return bodySlot >= 0 && bodySlot < this.origin.length ? this.origin[bodySlot] : -1;
    }

    public void setOrigin(int bodySlot, int preset) {
        if (bodySlot >= 0 && bodySlot < this.origin.length) {
            this.origin[bodySlot] = preset;
        }
    }

    /** 当前应该在外面的预设武器的物品 id；空串表示没有。 */
    public String getWeaponItem() {
        return this.weaponItem;
    }

    /** 那把武器上次被放到哪个背包格（快捷栏下标），-1 表示未知。 */
    public int getWeaponSlot() {
        return this.weaponSlot;
    }

    /**
     * 记录 / 清除「当前在外面」的预设武器。玩家切换快捷栏后手上就不是它了，
     * 靠这份记录才能在换装时把它从背包里找出来归位。
     */
    public void setOutWeapon(String itemId, int slot) {
        this.weaponItem = itemId == null ? "" : itemId;
        this.weaponSlot = slot;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, this.items, provider);
        tag.putInt(TAG_ACTIVE, this.active);
        tag.putIntArray(TAG_ORIGIN, this.origin);
        tag.putString(TAG_WEAPON_ITEM, this.weaponItem);
        tag.putInt(TAG_WEAPON_SLOT, this.weaponSlot);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.items = emptyList();
        ContainerHelper.loadAllItems(nbt, this.items, provider);
        this.active = nbt.getInt(TAG_ACTIVE);
        int[] stored = nbt.getIntArray(TAG_ORIGIN);
        for (int i = 0; i < this.origin.length; i++) {
            this.origin[i] = i < stored.length ? stored[i] : -1;
        }
        this.weaponItem = nbt.getString(TAG_WEAPON_ITEM);
        this.weaponSlot = nbt.contains(TAG_WEAPON_SLOT) ? nbt.getInt(TAG_WEAPON_SLOT) : -1;
    }
}
