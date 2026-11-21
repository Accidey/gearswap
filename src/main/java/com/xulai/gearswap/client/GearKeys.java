package com.xulai.gearswap.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import com.xulai.gearswap.GearSwap;

public final class GearKeys {
    private GearKeys() {}

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(GearSwap.MODID, "main"));

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
