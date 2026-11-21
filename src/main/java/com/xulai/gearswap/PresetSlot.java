package com.xulai.gearswap;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PresetSlot extends Slot {
    private static final Component ARMOR_HINT = Component.translatable("gearswap.gui.slot_hint")
            .withStyle(ChatFormatting.YELLOW);
    private static final Component ARMOR_HINT_DETAIL = Component.translatable("gearswap.gui.slot_hint.detail")
            .withStyle(ChatFormatting.GRAY);
    private static final Component OFFHAND_HINT = Component.translatable("gearswap.gui.slot_hint.offhand")
            .withStyle(ChatFormatting.YELLOW);
    private static final Component OFFHAND_HINT_DETAIL = Component.translatable("gearswap.gui.slot_hint.offhand.detail")
            .withStyle(ChatFormatting.GRAY);

    private static final List<Component> LINES_ARMOR = List.of(ARMOR_HINT);
    private static final List<Component> LINES_ARMOR_DETAIL = List.of(ARMOR_HINT, ARMOR_HINT_DETAIL);
    private static final List<Component> LINES_OFFHAND = List.of(OFFHAND_HINT);
    private static final List<Component> LINES_OFFHAND_DETAIL = List.of(OFFHAND_HINT, OFFHAND_HINT_DETAIL);

    private final Player owner;
    @Nullable
    private final EquipmentSlot armorSlot;
    private final boolean offhand;
    @Nullable
    private final Identifier emptyIcon;

    public PresetSlot(Container container, int index, int x, int y, Player owner,
                      @Nullable EquipmentSlot armorSlot, boolean offhand, @Nullable Identifier emptyIcon) {
        super(container, index, x, y);
        this.owner = owner;
        this.armorSlot = armorSlot;
        this.offhand = offhand;
        this.emptyIcon = emptyIcon;
    }

    public boolean isArmor() {
        return this.armorSlot != null;
    }

    @Override
    public int getMaxStackSize() {
        return this.armorSlot == null ? super.getMaxStackSize() : 1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty() || this.armorSlot == null) {
            return true;
        }
        return this.armorSlot == this.owner.getEquipmentSlotForItem(stack);
    }

    @Override
    @Nullable
    public Identifier getNoItemIcon() {
        return this.emptyIcon;
    }

    public void renderHoverTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        boolean detail = Minecraft.getInstance().hasShiftDown();
        List<Component> lines;
        if (this.isArmor()) {
            lines = detail ? LINES_ARMOR_DETAIL : LINES_ARMOR;
        } else if (this.offhand) {
            lines = detail ? LINES_OFFHAND_DETAIL : LINES_OFFHAND;
        } else {
            return;
        }
        graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
    }
}
