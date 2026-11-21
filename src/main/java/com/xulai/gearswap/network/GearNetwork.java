package com.xulai.gearswap.network;

import com.xulai.gearswap.GearSwap;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 网络包注册与工具方法。 */
public final class GearNetwork {
    private GearNetwork() {}

    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GearSwap.MODID, path));
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(OpenGearMenuPayload.TYPE, OpenGearMenuPayload.STREAM_CODEC,
                (payload, context) -> payload.handle(context));
        registrar.playToServer(SwapPresetPayload.TYPE, SwapPresetPayload.STREAM_CODEC,
                (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncPresetPayload.TYPE, SyncPresetPayload.STREAM_CODEC,
                (payload, context) -> payload.handle(context));
    }
}
