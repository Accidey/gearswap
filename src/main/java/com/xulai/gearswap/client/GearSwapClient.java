package com.xulai.gearswap.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.xulai.gearswap.GearClientState;
import com.xulai.gearswap.GearPresetData;
import com.xulai.gearswap.GearSwap;
import com.xulai.gearswap.ModMenus;
import com.xulai.gearswap.PresetSwitcher;
import com.xulai.gearswap.network.OpenGearMenuPayload;
import com.xulai.gearswap.network.SwapPresetPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.network.PacketDistributor;

/** 客户端入口：注册快捷键与预设界面、往原版背包界面挂入口按钮、处理组合键换装与 HUD 提示。 */
@Mod(value = GearSwap.MODID, dist = Dist.CLIENT)
public class GearSwapClient {
    /** 上一条 tick 的快捷栏槽位，用于还原被原版切走的槽位。 */
    private static int lastSelectedSlot;
    /** 本 tick 是否用组合键触发了换装（需要顺带还原快捷栏）。 */
    private static boolean restoreHotbar;

    public GearSwapClient(ModContainer container, IEventBus modEventBus) {
        // 模组总线
        modEventBus.addListener(GearSwapClient::registerKeyMappings);
        modEventBus.addListener(GearSwapClient::registerMenuScreens);
        // 游戏总线
        NeoForge.EVENT_BUS.addListener(GearSwapClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(GearSwapClient::onScreenRenderPost);
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

    /** 背包界面初始化完成后，在右上角加一个入口按钮。 */
    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            event.addListener(new GearSwapButton(screen,
                    button -> PacketDistributor.sendToServer(OpenGearMenuPayload.INSTANCE)));
        }
    }

    /** 按钮的悬停提示放在所有内容之后绘制，避免被物品槽盖住。 */
    private static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen)) {
            return;
        }
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof GearSwapButton button && button.isMouseOver(mouseX, mouseY)) {
                event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                        Component.translatable("gearswap.button.tooltip"),
                        (int) mouseX, (int) mouseY);
                break;
            }
        }
    }

    /**
     * 组合键换装：按住选择键（默认 R）再按数字键 1/2/3。
     *
     * <p>数字键同时是原版快捷栏切换键，而 {@code InputEvent.Key} 不可取消，
     * 所以这里只做记录，等本 tick 结束时再把快捷栏槽位还原回去。</p>
     */
    private static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null || !GearKeys.SELECTOR.isDown()) {
            return;
        }
        int preset = event.getKey() - GLFW.GLFW_KEY_1;
        if (preset < 0 || preset >= GearPresetData.PRESET_COUNT) {
            return;
        }
        restoreHotbar = true;
        PacketDistributor.sendToServer(new SwapPresetPayload(preset));
    }

    /** 还原被原版快捷栏逻辑切走的槽位（组合键只用来换装）。 */
    private static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int selected = player.getInventory().selected;
        if (restoreHotbar && selected != lastSelectedSlot) {
            player.getInventory().selected = lastSelectedSlot;
            selected = lastSelectedSlot;
        }
        restoreHotbar = false;
        lastSelectedSlot = selected;
    }

    /** 按住选择键时显示可切换的预设列表。 */
    private static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null || !GearKeys.SELECTOR.isDown()) {
            return;
        }
        renderSelector(event.getGuiGraphics(), minecraft.font);
    }

    private static void renderSelector(GuiGraphics graphics, Font font) {
        int rows = GearPresetData.PRESET_COUNT;
        int width = 132;
        int height = 15 + rows * 13 + 4;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 58 - height;

        // 原版提示框风格：暗底 + 半透明深紫描边
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x80100010);
        graphics.fill(x, y, x + width, y + height, 0xE0100010);

        graphics.drawString(font, Component.translatable("gearswap.hud.title",
                GearKeys.SELECTOR.getTranslatedKeyMessage()), x + 6, y + 5, 0xFF9E9E9E, false);

        int active = GearClientState.getActivePreset();
        for (int preset = 0; preset < rows; preset++) {
            int rowY = y + 17 + preset * 13;
            boolean isActive = preset == active;
            if (isActive) {
                graphics.fill(x + 3, rowY - 2, x + width - 3, rowY + 10, 0x30FFD24A);
            }
            drawKeycap(graphics, font, x + 6, rowY - 1, String.valueOf(preset + 1), isActive);
            graphics.drawString(font, Component.translatable(PresetSwitcher.presetNameKey(preset)),
                    x + 26, rowY, isActive ? 0xFFFFD24A : 0xFFFFFFFF, false);
        }
    }

    private static void drawKeycap(GuiGraphics graphics, Font font, int x, int y, String key, boolean highlight) {
        int border = highlight ? 0xFF8A6D1F : 0xFF555555;
        int body = highlight ? 0xFFFFE9A8 : 0xFFC6C6C6;
        graphics.fill(x - 1, y - 1, x + 14, y + 13, border);
        graphics.fill(x, y, x + 13, y + 12, body);
        graphics.drawString(font, key, x + 7 - font.width(key) / 2, y + 2, 0xFF303030, false);
    }
}
