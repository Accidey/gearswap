package com.xulai.gearswap;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GearConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUTTON_POSITION = BUILDER
                .comment(
                        "背包界面按钮位置",
                        "Inventory screen button position",
                        "可选值：LEFT（面板左侧）、TOP_LEFT（面板左上）、TOP_CENTER（面板正上方居中）、TOP_RIGHT（面板右上）、RIGHT（面板右侧）。",
                        "Available values: LEFT, TOP_LEFT, TOP_CENTER, TOP_RIGHT, RIGHT.",
                        "Default: LEFT / 默认：LEFT"
                )
                .defineEnum("buttonPosition", ButtonPosition.LEFT);
    }

    public static final ModConfigSpec.EnumValue<ButtonPosition> BUTTON_POSITION;

    public static final ModConfigSpec SPEC = BUILDER.build();

    private GearConfig() {}

    public static ButtonPosition buttonPosition() {
        if (!SPEC.isLoaded()) {
            return ButtonPosition.LEFT;
        }
        return BUTTON_POSITION.get();
    }
}
