package com.xulai.gearswap;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 预设格。
 *
 * <ul>
 *   <li>主手格 / 副手格接受**任意**物品（原版副手本就什么都能拿），沿用物品自身的堆叠上限；</li>
 *   <li>4 个盔甲格是**装备槽**：只接受与该部位匹配的装备、每个格子只放 1 件，
 *       并在空置时显示原版的空槽剪影；</li>
 *   <li>鼠标停在盔甲格上会给出「只能放入装备」的悬浮提示，副手格显示「可放任何物品」。</li>
 * </ul>
 *
 * <p>提示由界面层在 {@code AbstractContainerScreen#render} 末尾统一触发（见
 * {@link #renderHoverTooltip}），因此必须晚于 {@code super.render(...)} 绘制，
 * 否则会被随后渲染的槽位与物品盖住。</p>
 */
public class PresetSlot extends Slot {
    /** 盔甲格的提示：「只能放入装备」那一行。 */
    private static final Component ARMOR_HINT = Component.translatable("gearswap.gui.slot_hint")
            .withStyle(ChatFormatting.YELLOW);
    /** 按住 Shift 时补充的详细说明。 */
    private static final Component ARMOR_HINT_DETAIL = Component.translatable("gearswap.gui.slot_hint.detail")
            .withStyle(ChatFormatting.GRAY);
    /** 副手格的提示：说明它不挑物品。 */
    private static final Component OFFHAND_HINT = Component.translatable("gearswap.gui.slot_hint.offhand")
            .withStyle(ChatFormatting.YELLOW);
    private static final Component OFFHAND_HINT_DETAIL = Component.translatable("gearswap.gui.slot_hint.offhand.detail")
            .withStyle(ChatFormatting.GRAY);

    private static final List<Component> LINES_ARMOR = List.of(ARMOR_HINT);
    private static final List<Component> LINES_ARMOR_DETAIL = List.of(ARMOR_HINT, ARMOR_HINT_DETAIL);
    private static final List<Component> LINES_OFFHAND = List.of(OFFHAND_HINT);
    private static final List<Component> LINES_OFFHAND_DETAIL = List.of(OFFHAND_HINT, OFFHAND_HINT_DETAIL);

    private final Player owner;
    /** 只有盔甲格有值；主手 / 副手格为 null，表示不限制物品。 */
    @Nullable
    private final EquipmentSlot armorSlot;
    /** 是否副手格（用来决定显示哪种提示）。 */
    private final boolean offhand;
    @Nullable
    private final ResourceLocation emptyIcon;

    public PresetSlot(Container container, int index, int x, int y, Player owner,
                      @Nullable EquipmentSlot armorSlot, boolean offhand, @Nullable ResourceLocation emptyIcon) {
        super(container, index, x, y);
        this.owner = owner;
        this.armorSlot = armorSlot;
        this.offhand = offhand;
        this.emptyIcon = emptyIcon;
    }

    /** 是不是「盔甲格」（严格限制部位的装备槽）。 */
    public boolean isArmor() {
        return this.armorSlot != null;
    }

    @Override
    public int getMaxStackSize() {
        // 只有盔甲格限制为 1 件；主手与副手格沿用物品自身的堆叠上限。
        return this.armorSlot == null ? super.getMaxStackSize() : 1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty() || this.armorSlot == null) {
            // 主手 / 副手格：任意物品都能放
            return true;
        }
        return this.armorSlot == this.owner.getEquipmentSlotForItem(stack);
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return this.emptyIcon == null ? super.getNoItemIcon() : Pair.of(InventoryMenu.BLOCK_ATLAS, this.emptyIcon);
    }

    /**
     * 悬浮提示。调用点必须在小部件全部画完之后（{@code super.render} 之后），
     * 否则提示会被后画的槽位与物品覆盖。主手格没有提示。
     */
    public void renderHoverTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        boolean detail = Screen.hasShiftDown();
        List<Component> lines;
        if (this.isArmor()) {
            lines = detail ? LINES_ARMOR_DETAIL : LINES_ARMOR;
        } else if (this.offhand) {
            lines = detail ? LINES_OFFHAND_DETAIL : LINES_OFFHAND;
        } else {
            return;
        }
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }
}
