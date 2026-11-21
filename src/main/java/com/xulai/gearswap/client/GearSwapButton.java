package com.xulai.gearswap.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import com.xulai.gearswap.ButtonPosition;
import com.xulai.gearswap.GearConfig;

/**
 * 背包界面左侧的入口按钮。
 *
 * <p>原版按钮在文本放不下时会让文字来回滚动（{@code renderScrollingString}），这里：</p>
 * <ul>
 *   <li>按钮长度按**当前语言文本的实际渲染宽度**算出来（中文时刚好放下中文，英文时按英文长度拉长），
 *       所以默认不需要滚动；文本因语言过长而超出长度上限时，再等比缩小到刚好放得下。</li>
 *   <li>覆写 {@link #renderString} 静态居中绘制，彻底去掉滚动动画。</li>
 *   <li>贴在背包面板**左侧外侧**，与面板的间隙和顶部按钮时期保持一致（{@link #GAP}）。</li>
 * </ul>
 */
public class GearSwapButton extends Button {
    /** 文本两侧留白。 */
    private static final int PADDING = 10;
    /** 按钮最大长度，超出就把文本等比缩小。 */
    private static final int MAX_WIDTH = 150;
    /** 与背包面板之间的间隙。 */
    private static final int GAP = 4;
    private static final int HEIGHT = 20;
    /** 原版字号的文本行高（像素）。 */
    private static final float TEXT_HEIGHT = 8.0F;
    /** 文本左右最小边距。 */
    private static final int TEXT_MARGIN = 3;
    private static final int SCREEN_MARGIN = 2;
    private static final int LINE_HEIGHT = 9;
    private static final int VERTICAL_PADDING = 8;
    private static final int MIN_VERTICAL_WIDTH = 14;
    private static final int BASELINE_OFFSET = 1;
    private static final int HELMET_SLOT_Y = 8;

    private final InventoryScreen screen;

    public GearSwapButton(InventoryScreen screen, OnPress onPress) {
        super(0, 0, measureWidth(), HEIGHT, Component.translatable("gearswap.button.open"), onPress, DEFAULT_NARRATION);
        this.screen = screen;
    }

    /** 按钮长度 = 文本渲染宽度 + 留白（上限 {@link #MAX_WIDTH}）。 */
    private static int measureWidth() {
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(Component.translatable("gearswap.button.open"));
        return Math.min(textWidth + PADDING, MAX_WIDTH);
    }

    private static boolean isSideways(ButtonPosition position) {
        return position == ButtonPosition.LEFT || position == ButtonPosition.RIGHT;
    }

    private static List<String> verticalLines(Component message) {
        String text = message.getString().trim();
        List<String> lines = new ArrayList<>();
        boolean han = text.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
        if (han) {
            text.codePoints()
                    .filter(codePoint -> !Character.isWhitespace(codePoint))
                    .forEach(codePoint -> lines.add(new String(Character.toChars(codePoint))));
        } else {
            for (String word : text.split("\\s+")) {
                if (!word.isEmpty()) {
                    lines.add(word);
                }
            }
        }
        return lines;
    }

    private ButtonPosition applyConfiguredPosition() {
        ButtonPosition position = GearConfig.buttonPosition();
        int left = this.screen.getGuiLeft();
        int top = this.screen.getGuiTop();
        int panelWidth = this.screen.getXSize();
        int x;
        int y;

        if (isSideways(position)) {
            List<String> lines = verticalLines(this.getMessage());
            Font font = Minecraft.getInstance().font;
            int width = MIN_VERTICAL_WIDTH;
            for (String line : lines) {
                width = Math.max(width, font.width(line) + TEXT_MARGIN * 2);
            }
            this.setWidth(width);
            this.setHeight(lines.size() * LINE_HEIGHT + VERTICAL_PADDING);
            y = top + HELMET_SLOT_Y;
            x = position == ButtonPosition.RIGHT ? left + panelWidth + GAP : left - GAP - this.getWidth();
        } else {
            this.setWidth(measureWidth());
            this.setHeight(HEIGHT);
            switch (position) {
                case TOP_LEFT -> {
                    x = left;
                    y = top - GAP - this.getHeight();
                }
                case TOP_CENTER -> {
                    x = left + (panelWidth - this.getWidth()) / 2;
                    y = top - GAP - this.getHeight();
                }
                case TOP_RIGHT -> {
                    x = left + panelWidth - this.getWidth();
                    y = top - GAP - this.getHeight();
                }
                default -> {
                    x = left - GAP - this.getWidth();
                    y = top;
                }
            }
        }

        this.setPosition(
                clamp(x, this.screen.width - this.getWidth() - SCREEN_MARGIN),
                clamp(y, this.screen.height - this.getHeight() - SCREEN_MARGIN)
        );
        return position;
    }

    private static int clamp(int value, int maximum) {
        return Math.max(SCREEN_MARGIN, Math.min(maximum, value));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.applyConfiguredPosition();
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    /** 静态居中绘制：不滚动；万一文本比按钮还长，等比缩小到刚好放得下。 */
    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        if (isSideways(GearConfig.buttonPosition())) {
            List<String> lines = verticalLines(this.getMessage());
            int y = this.getY() + (this.getHeight() - lines.size() * LINE_HEIGHT) / 2 + BASELINE_OFFSET;
            for (String line : lines) {
                graphics.drawString(font, line, this.getX() + (this.getWidth() - font.width(line)) / 2, y, color, false);
                y += LINE_HEIGHT;
            }
            return;
        }

        String text = this.getMessage().getString();
        int textWidth = font.width(text);
        int inner = Math.max(1, this.getWidth() - TEXT_MARGIN * 2);
        float scale = textWidth > inner ? (float) inner / (float) textWidth : 1.0F;

        float drawX = this.getX() + (this.getWidth() - textWidth * scale) / 2.0F;
        float drawY = this.getY() + (this.getHeight() - TEXT_HEIGHT * scale) / 2.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }
}
