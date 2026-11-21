package com.xulai.gearswap;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, GearSwap.MODID);

    public static final RegistryObject<MenuType<GearPresetMenu>> GEAR_PRESETS =
            MENUS.register("gear_presets",
                    () -> IForgeMenuType.create((windowId, playerInventory, extraData) ->
                            new GearPresetMenu(windowId, playerInventory)));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
