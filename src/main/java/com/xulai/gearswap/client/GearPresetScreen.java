package com.xulai.gearswap.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.xulai.gearswap.GearPresetData;
import com.xulai.gearswap.GearPresetMenu;
import com.xulai.gearswap.GearSlots;
import com.xulai.gearswap.GearSwap;
import com.xulai.gearswap.PresetSlot;
import com.xulai.gearswap.network.SwapPresetPayload;

public class GearPresetScreen extends AbstractContainerScreen<GearPresetMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(GearSwap.MODID, "textures/gui/gear_presets.png");

    private static final int LABEL_COLOR = 0xFF808080;
    private static final int LABEL_COLOR_ACTIVE = 0xFF404040;
    private static final int ROW_TINT = 0x33FFD24A;
    private static final int KEYCAP_TEXT = 0xFF303030;

    public GearPresetScreen(GearPresetMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GearPresetMenu.IMAGE_WIDTH, GearPresetMenu.IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        int active = this.menu.getActivePreset();
        if (active >= 0 && active < GearPresetData.PRESET_COUNT) {
            int rowTop = y + GearPresetMenu.ROW_Y + active * GearPresetMenu.SLOT_SIZE;
            graphics.fill(x + 3, rowTop - 1, x + this.imageWidth - 3, rowTop + GearPresetMenu.SLOT_SIZE, ROW_TINT);
        }

        for (int preset = 0; preset < GearPresetData.PRESET_COUNT; preset++) {
            int slotIndex = GearPresetData.index(preset, GearPresetData.SLOT_WEAPON);
            if (this.menu.getSlot(slotIndex).getItem().isEmpty()) {
                graphics.blit(RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x + GearPresetMenu.WEAPON_X,
                        y + GearPresetMenu.ROW_Y + preset * GearPresetMenu.SLOT_SIZE,
                        GearSlots.WEAPON_ICON_U, GearSlots.WEAPON_ICON_V, 16, 16, 256, 256);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);

        int active = this.menu.getActivePreset();
        for (int preset = 0; preset < GearPresetData.PRESET_COUNT; preset++) {
            boolean isActive = preset == active;
            int rowTop = GearPresetMenu.ROW_Y + preset * GearPresetMenu.SLOT_SIZE;
            Component label = Component.translatable("gearswap.gui.preset", preset + 1);
            graphics.text(this.font, label,
                    GearPresetMenu.LABEL_RIGHT - this.font.width(label), rowTop + 5,
                    isActive ? LABEL_COLOR_ACTIVE : LABEL_COLOR, false);
            drawKeycap(graphics, GearPresetMenu.KEYCAP_X, rowTop + GearPresetMenu.KEYCAP_Y_OFFSET,
                    GearKeys.comboName(preset), isActive);
        }
    }

    private void drawKeycap(GuiGraphicsExtractor graphics, int x, int y, String key, boolean highlight) {
        int width = GearPresetMenu.KEYCAP_WIDTH;
        int height = GearPresetMenu.KEYCAP_HEIGHT;
        int border = highlight ? 0xFF8A6D1F : 0xFF555555;
        int body = highlight ? 0xFFFFE9A8 : 0xFFC6C6C6;
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, border);
        graphics.fill(x, y, x + width, y + height, body);
        graphics.text(this.font, key, x + (width - this.font.width(key)) / 2, y + 2, KEYCAP_TEXT, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot instanceof PresetSlot slot) {
            slot.renderHoverTooltip(graphics, this.font, mouseX, mouseY);
        } else {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
        int preset = rowAt(mouseX, mouseY);
        if (preset >= 0) {
            graphics.setTooltipForNextFrame(this.font,
                    Component.translatable("gearswap.gui.row_hint", preset + 1, GearKeys.comboName(preset)),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int preset = rowAt(event.x(), event.y());
        if (preset >= 0 && event.button() == 0 && this.menu.getCarried().isEmpty()) {
            Minecraft.getInstance().getConnection().send(new SwapPresetPayload(preset));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int rowAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - this.leftPos;
        int localY = (int) mouseY - this.topPos;
        if (localX < 8 || localX > GearPresetMenu.KEYCAP_X + GearPresetMenu.KEYCAP_WIDTH) {
            return -1;
        }
        if (localX >= GearPresetMenu.PRESET_LEFT && localX < GearPresetMenu.PRESET_RIGHT) {
            return -1;
        }
        for (int preset = 0; preset < GearPresetData.PRESET_COUNT; preset++) {
            int rowTop = GearPresetMenu.ROW_Y + preset * GearPresetMenu.SLOT_SIZE;
            if (localY >= rowTop - 2 && localY <= rowTop + GearPresetMenu.SLOT_SIZE + 2) {
                return preset;
            }
        }
        return -1;
    }
}
