package com.xulai.gearswap;

import net.minecraft.world.entity.player.Player;

public final class GearCapabilities {
    private GearCapabilities() {}

    public static GearPresetData presets(Player player) {
        return player.getCapability(GearSwap.GEAR_PRESETS)
                .orElseThrow(() -> new IllegalStateException("Player is missing gear preset data"));
    }
}
