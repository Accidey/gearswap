package com.xulai.gearswap;

/**
 * 客户端持有的界面状态（由服务端 {@code SyncPresetPayload} 推送）。
 *
 * <p>纯数据类，不引用任何客户端专属类型，因此两个物理端都能安全加载。</p>
 */
public final class GearClientState {
    private GearClientState() {}

    private static int activePreset = -1;

    /** 当前生效的预设下标，-1 表示没有。 */
    public static int getActivePreset() {
        return activePreset;
    }

    public static void setActivePreset(int preset) {
        activePreset = preset;
    }
}
