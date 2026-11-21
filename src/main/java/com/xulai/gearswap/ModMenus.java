package com.xulai.gearswap;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 菜单类型注册。 */
public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, GearSwap.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<GearPresetMenu>> GEAR_PRESETS =
            MENUS.register("gear_presets",
                    () -> IMenuTypeExtension.create((windowId, playerInventory, extraData) ->
                            new GearPresetMenu(windowId, playerInventory)));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
