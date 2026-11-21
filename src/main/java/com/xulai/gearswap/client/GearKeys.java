package com.xulai.gearswap.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;

/**
 * 快捷键定义。
 *
 * <p>换装用「组合键」：按住 {@link #SELECTOR}（默认 R）再按数字键 1/2/3，切换第 1/2/3 套预设；
 * 选择键可在「选项 -> 控制」里重新绑定。</p>
 */
public final class GearKeys {
    private GearKeys() {}

    public static final String CATEGORY = "key.categories.gearswap";

    /** 预设选择键（按住当修饰键用），默认 R。 */
    public static final KeyMapping SELECTOR =
            new KeyMapping("key.gearswap.selector", GLFW.GLFW_KEY_R, CATEGORY);

    /** 组合键的显示文本，例如 "R+1"。 */
    public static String comboName(int preset) {
        String key = SELECTOR.getTranslatedKeyMessage().getString();
        if (key.length() > 2) {
            key = key.substring(0, 2);
        }
        return key + "+" + (preset + 1);
    }
}
