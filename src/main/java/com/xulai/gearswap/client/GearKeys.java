package com.xulai.gearswap.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;

public final class GearKeys {
    private GearKeys() {}

    public static final String CATEGORY = "key.categories.gearswap";

    public static final KeyMapping SELECTOR =
            new KeyMapping("key.gearswap.selector", GLFW.GLFW_KEY_R, CATEGORY);

    public static String comboName(int preset) {
        String key = SELECTOR.getTranslatedKeyMessage().getString();
        if (key.length() > 2) {
            key = key.substring(0, 2);
        }
        return key + "+" + (preset + 1);
    }
}
