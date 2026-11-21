package com.xulai.gearswap.network;

import java.util.function.Supplier;

import com.xulai.gearswap.GearClientState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SyncPresetPayload(int active) {
    public static void encode(SyncPresetPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.active);
    }

    public static SyncPresetPayload decode(FriendlyByteBuf buf) {
        return new SyncPresetPayload(buf.readVarInt());
    }

    public static void handle(SyncPresetPayload payload, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> GearClientState.setActivePreset(payload.active));
        context.get().setPacketHandled(true);
    }
}
