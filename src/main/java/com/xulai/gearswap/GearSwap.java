package com.xulai.gearswap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.xulai.gearswap.network.GearNetwork;
import com.xulai.gearswap.network.SyncPresetPayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Mod(GearSwap.MODID)
public class GearSwap {
    public static final String MODID = "gearswap";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Capability<GearPresetData> GEAR_PRESETS =
            CapabilityManager.get(new CapabilityToken<>() {});

    public GearSwap() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, GearConfig.SPEC);
        ModMenus.register(modEventBus);
        modEventBus.addListener(GearSwap::onRegisterCapabilities);
        modEventBus.addListener(GearSwap::onCommonSetup);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, GearSwap::onAttachCapabilities);
        MinecraftForge.EVENT_BUS.addListener(GearSwap::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(GearSwap::onPlayerLogin);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(GearPresetData.class);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(GearNetwork::register);
    }

    private static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player
                && !event.getObject().getCapability(GEAR_PRESETS).isPresent()) {
            GearPresetProvider provider = new GearPresetProvider();
            event.addCapability(ResourceLocation.fromNamespaceAndPath(MODID, "gear_presets"), provider);
            event.addListener(provider::invalidate);
        }
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        GearPresetData oldData = GearCapabilities.presets(event.getOriginal());
        GearPresetData newData = GearCapabilities.presets(event.getEntity());
        newData.deserializeNBT(oldData.serializeNBT());
        event.getOriginal().invalidateCaps();
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GearPresetData data = GearCapabilities.presets(player);
            GearNetwork.sendToPlayer(player, new SyncPresetPayload(data.getActive()));
        }
    }
}
