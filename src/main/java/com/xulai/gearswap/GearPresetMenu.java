package com.xulai.gearswap;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 预设界面菜单：3 行 × 6 格（主手 + 副手 + 四件盔甲）加上玩家背包。
 *
 * <p>槽位顺序与 {@link GearPresetData} 的套内下标一致：主手 → 副手 → 头/胸/腿/靴。</p>
 *
 * <p>布局常量同时被客户端界面 {@code GearPresetScreen} 引用，改动只需改这里。</p>
 */
public class GearPresetMenu extends AbstractContainerMenu {
    /** 面板宽度（比原版容器的 176 宽 24 像素，用来多放一列「组合键提示」）。 */
    public static final int IMAGE_WIDTH = 200;
    /** 面板高度。 */
    public static final int IMAGE_HEIGHT = 168;

    /** 预设行第一格的 x。 */
    public static final int PRESET_X = 49;
    /** 槽位边长。 */
    public static final int SLOT_SIZE = 18;
    /** 第一行预设槽的 y。 */
    public static final int ROW_Y = 18;
    /** 主手格 x。 */
    public static final int WEAPON_X = PRESET_X;
    /** 副手格 x（紧跟主手右侧）。 */
    public static final int OFFHAND_X = PRESET_X + SLOT_SIZE;
    /** 第一件盔甲格 x。 */
    public static final int ARMOR_X = PRESET_X + 2 * SLOT_SIZE;
    /** 预设槽区域的左边界（含 1 像素边框）。 */
    public static final int PRESET_LEFT = PRESET_X - 1;
    /** 预设槽区域的右边界（槽位点击判定到这一像素为止）。 */
    public static final int PRESET_RIGHT = PRESET_X + GearPresetData.SLOTS_PER_PRESET * SLOT_SIZE;
    /**
     * 一行内三块内容（左侧行标签 / 中间预设槽 / 右侧组合键提示）之间的统一间距。
     *
     * <p>标签按 {@link #LABEL_RIGHT} 右对齐、键帽按 {@link #KEYCAP_X} 左对齐，
     * 于是「标签↔槽」「槽↔键帽」两处留白恒等，且不受语言（中/英）文本宽度影响。</p>
     */
    public static final int ROW_GAP = 10;
    /** 行标签的右对齐位置（文字右边缘）。 */
    public static final int LABEL_RIGHT = PRESET_LEFT - ROW_GAP;
    /** 组合键提示键帽的 x。 */
    public static final int KEYCAP_X = PRESET_RIGHT + ROW_GAP;
    public static final int KEYCAP_Y_OFFSET = 3;
    public static final int KEYCAP_WIDTH = 22;
    public static final int KEYCAP_HEIGHT = 12;

    /** 预设格数量，同时也是玩家背包在 slots 列表中的起始下标。 */
    public static final int PRESET_SLOTS = GearPresetData.TOTAL_SLOTS;
    /** 玩家背包第一行 y。 */
    public static final int INV_ROW_Y = 85;
    /** 快捷栏 y。 */
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

        // 与客户端同步「当前生效的预设」，用于界面高亮
        this.activeSlot = new DataSlot() {
            @Override
            public int get() {
                return owner.getData(GearSwap.GEAR_PRESETS).getActive();
            }

            @Override
            public void set(int value) {
                owner.getData(GearSwap.GEAR_PRESETS).setActive(value);
            }
        };
        this.addDataSlot(this.activeSlot);
    }

    /**
     * Shift 点击时盔甲/副手格只接受对应部位的**单个**物品，
     * 原版 {@code moveItemStackTo} 会把整堆数进去，所以这里先按容量裁剪一份拷贝。
     */
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        if (stack.getCount() > 1 && startIndex < PRESET_SLOTS) {
            ItemStack single = stack.copyWithCount(1);
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
                // 预设格 -> 玩家背包
                if (!this.moveItemStackTo(stack, PRESET_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 -> 能接受的预设格（盔甲格会自行校验部位）
                if (!this.moveItemStackTo(stack, 0, PRESET_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
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
