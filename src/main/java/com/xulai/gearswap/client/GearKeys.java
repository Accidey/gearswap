package com.xulai.gearswap.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import com.xulai.gearswap.GearSwap;

public final class GearKeys {
    private GearKeys() {}

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(GearSwap.MODID, "main"));

    // 26.3 起 LWJGL 的 glfw 不再作为编译期依赖暴露，键码统一走 InputConstants。
    public static final KeyMapping SELECTOR =
            new KeyMapping("key.gearswap.selector", InputConstants.KEY_R, CATEGORY);

    public static String comboName(int preset) {
        String key = SELECTOR.getTranslatedKeyMessage().getString();
        if (key.length() > 2) {
            key = key.substring(0, 2);
        }
        return key + "+" + (preset + 1);
    }
}
