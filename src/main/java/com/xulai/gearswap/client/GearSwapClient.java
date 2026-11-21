package com.xulai.gearswap.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import com.xulai.gearswap.GearClientState;
import com.xulai.gearswap.GearPresetData;
import com.xulai.gearswap.GearSwap;
import com.xulai.gearswap.ModMenus;
import com.xulai.gearswap.PresetSwitcher;
import com.xulai.gearswap.network.OpenGearMenuPayload;
import com.xulai.gearswap.network.SwapPresetPayload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = GearSwap.MODID, dist = Dist.CLIENT)
public class GearSwapClient {
    private static int lastSelectedSlot;
    private static boolean restoreHotbar;

    public GearSwapClient(ModContainer container, IEventBus modEventBus) {
        modEventBus.addListener(GearSwapClient::registerKeyMappings);
        modEventBus.addListener(GearSwapClient::registerMenuScreens);
        NeoForge.EVENT_BUS.addListener(GearSwapClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(GearSwapClient::onScreenRenderForeground);
        NeoForge.EVENT_BUS.addListener(GearSwapClient::onKeyInput);
        NeoForge.EVENT_BUS.addListener(GearSwapClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(GearSwapClient::onRenderGui);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GearKeys.SELECTOR);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.GEAR_PRESETS.get(), GearPresetScreen::new);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            event.addListener(new GearSwapButton(screen,
                    button -> Minecraft.getInstance().getConnection().send(OpenGearMenuPayload.INSTANCE)));
        }
    }

    private static void onScreenRenderForeground(ScreenEvent.Render.Foreground event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen)) {
            return;
        }
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof GearSwapButton button && button.isMouseOver(mouseX, mouseY)) {
                event.getGuiGraphics().setTooltipForNextFrame(Minecraft.getInstance().font,
                        Component.translatable("gearswap.button.tooltip"),
                        (int) mouseX, (int) mouseY);
                break;
            }
        }
    }

    private static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null || minecraft.player == null || !GearKeys.SELECTOR.isDown()) {
            return;
        }
        int preset = event.getKey() - GLFW.GLFW_KEY_1;
        if (preset < 0 || preset >= GearPresetData.PRESET_COUNT) {
            return;
        }
        restoreHotbar = true;
        minecraft.getConnection().send(new SwapPresetPayload(preset));
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int selected = player.getInventory().getSelectedSlot();
        if (restoreHotbar && selected != lastSelectedSlot) {
            player.getInventory().setSelectedSlot(lastSelectedSlot);
            selected = lastSelectedSlot;
        }
        restoreHotbar = false;
        lastSelectedSlot = selected;
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null || minecraft.player == null || !GearKeys.SELECTOR.isDown()) {
            return;
        }
        renderSelector(event.getGuiGraphics(), minecraft.font);
    }

    private static void renderSelector(GuiGraphicsExtractor graphics, Font font) {
        int rows = GearPresetData.PRESET_COUNT;
        int width = 132;
        int height = 15 + rows * 13 + 4;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 58 - height;

        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x80100010);
        graphics.fill(x, y, x + width, y + height, 0xE0100010);

        graphics.text(font, Component.translatable("gearswap.hud.title",
                GearKeys.SELECTOR.getTranslatedKeyMessage()), x + 6, y + 5, 0xFF9E9E9E, false);

        int active = GearClientState.getActivePreset();
        for (int preset = 0; preset < rows; preset++) {
            int rowY = y + 17 + preset * 13;
            boolean isActive = preset == active;
            if (isActive) {
                graphics.fill(x + 3, rowY - 2, x + width - 3, rowY + 10, 0x30FFD24A);
            }
            drawKeycap(graphics, font, x + 6, rowY - 1, String.valueOf(preset + 1), isActive);
            graphics.text(font, Component.translatable(PresetSwitcher.presetNameKey(preset)),
                    x + 26, rowY, isActive ? 0xFFFFD24A : 0xFFFFFFFF, false);
        }
    }

    private static void drawKeycap(GuiGraphicsExtractor graphics, Font font, int x, int y, String key, boolean highlight) {
        int border = highlight ? 0xFF8A6D1F : 0xFF555555;
        int body = highlight ? 0xFFFFE9A8 : 0xFFC6C6C6;
        graphics.fill(x - 1, y - 1, x + 14, y + 13, border);
        graphics.fill(x, y, x + 13, y + 12, body);
        graphics.text(font, key, x + 7 - font.width(key) / 2, y + 2, 0xFF303030, false);
    }
}
