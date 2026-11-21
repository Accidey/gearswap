package com.xulai.gearswap.client;

import com.xulai.gearswap.GearPresetData;
import com.xulai.gearswap.GearPresetMenu;
import com.xulai.gearswap.GearSlots;
import com.xulai.gearswap.GearSwap;
import com.xulai.gearswap.PresetSlot;
import com.xulai.gearswap.network.SwapPresetPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 装备预设界面。
 *
 * <p>底板（预设区 + 玩家背包区）全部由本模组自绘，配色与原版容器一致，不依赖原版 GUI 贴图。</p>
 */
public class GearPresetScreen extends AbstractContainerScreen<GearPresetMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GearSwap.MODID, "textures/gui/gear_presets.png");

    private static final int LABEL_COLOR = 0x808080;
    private static final int LABEL_COLOR_ACTIVE = 0x404040;
    private static final int ROW_TINT = 0x33FFD24A;

    public GearPresetScreen(GearPresetMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GearPresetMenu.IMAGE_WIDTH;
        this.imageHeight = GearPresetMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 整块底板（预设区 + 玩家背包区）均为本模组自绘，配色与原版容器一致
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 高亮当前生效的预设行
        int active = this.menu.getActivePreset();
        if (active >= 0 && active < GearPresetData.PRESET_COUNT) {
            int rowTop = y + GearPresetMenu.ROW_Y + active * GearPresetMenu.SLOT_SIZE;
            graphics.fill(x + 3, rowTop - 1, x + this.imageWidth - 3, rowTop + GearPresetMenu.SLOT_SIZE, ROW_TINT);
        }

        // 主手格为空时自绘剑形提示（副手格与盔甲格由原版画空槽剪影）
        for (int preset = 0; preset < GearPresetData.PRESET_COUNT; preset++) {
            int slotIndex = GearPresetData.index(preset, GearPresetData.SLOT_WEAPON);
            if (this.menu.getSlot(slotIndex).getItem().isEmpty()) {
                graphics.blit(TEXTURE,
                        x + GearPresetMenu.WEAPON_X,
                        y + GearPresetMenu.ROW_Y + preset * GearPresetMenu.SLOT_SIZE,
                        GearSlots.WEAPON_ICON_U, GearSlots.WEAPON_ICON_V, 16, 16);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);

        int active = this.menu.getActivePreset();
        for (int preset = 0; preset < GearPresetData.PRESET_COUNT; preset++) {
            boolean isActive = preset == active;
            int rowTop = GearPresetMenu.ROW_Y + preset * GearPresetMenu.SLOT_SIZE;
            // 右对齐：中/英文标签宽度不同，只有对齐右边缘才能保证与预设槽的间距恒定
            Component label = Component.translatable("gearswap.gui.preset", preset + 1);
            graphics.drawString(this.font, label,
                    GearPresetMenu.LABEL_RIGHT - this.font.width(label), rowTop + 5,
                    isActive ? LABEL_COLOR_ACTIVE : LABEL_COLOR, false);
            drawKeycap(graphics, GearPresetMenu.KEYCAP_X, rowTop + GearPresetMenu.KEYCAP_Y_OFFSET,
                    GearKeys.comboName(preset), isActive);
        }
    }

    private void drawKeycap(GuiGraphics graphics, int x, int y, String key, boolean highlight) {
        int width = GearPresetMenu.KEYCAP_WIDTH;
        int height = GearPresetMenu.KEYCAP_HEIGHT;
        int border = highlight ? 0xFF8A6D1F : 0xFF555555;
        int body = highlight ? 0xFFFFE9A8 : 0xFFC6C6C6;
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, border);
        graphics.fill(x, y, x + width, y + height, body);
        graphics.drawString(this.font, key, x + (width - this.font.width(key)) / 2, y + 2, 0x303030, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // 装备格（盔甲 / 副手）的悬浮提示：必须放在 super.render 之后，
        // 否则会被随后渲染的槽位和物品盖住。
        if (this.hoveredSlot instanceof PresetSlot slot) {
            slot.renderHoverTooltip(graphics, this.font, mouseX, mouseY);
        }
        int preset = rowAt(mouseX, mouseY);
        if (preset >= 0) {
            graphics.renderTooltip(this.font,
                    Component.translatable("gearswap.gui.row_hint", preset + 1, GearKeys.comboName(preset)),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int preset = rowAt(mouseX, mouseY);
        if (preset >= 0 && button == 0 && this.menu.getCarried().isEmpty()) {
            PacketDistributor.sendToServer(new SwapPresetPayload(preset));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 判断鼠标是否落在某一行预设的「标签 / 快捷键」空白区域上。 */
    private int rowAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - this.leftPos;
        int localY = (int) mouseY - this.topPos;
        if (localX < 8 || localX > GearPresetMenu.KEYCAP_X + GearPresetMenu.KEYCAP_WIDTH) {
            return -1;
        }
        // 槽位区域交给原版处理（只排除槽位本身，槽间与槽两侧的留白仍可点击换装）
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
