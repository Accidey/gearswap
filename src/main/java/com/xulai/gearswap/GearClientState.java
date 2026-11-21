package com.xulai.gearswap;

public final class GearClientState {
    private GearClientState() {}

    private static int activePreset = -1;

    public static int getActivePreset() {
        return activePreset;
    }

    public static void setActivePreset(int preset) {
        activePreset = preset;
    }
}
