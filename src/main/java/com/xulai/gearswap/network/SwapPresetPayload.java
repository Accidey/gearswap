package com.xulai.gearswap.network;

import java.util.function.Supplier;

import com.xulai.gearswap.GearSwap;
import com.xulai.gearswap.PresetSwitcher;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record SwapPresetPayload(int preset) {
    public static void encode(SwapPresetPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.preset);
    }

    public static SwapPresetPayload decode(FriendlyByteBuf buf) {
        return new SwapPresetPayload(buf.readVarInt());
    }

    public static void handle(SwapPresetPayload payload, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                PresetSwitcher.swap(player, payload.preset);
            } else {
                GearSwap.LOGGER.warn("Ignoring swap request from non-server player");
            }
        });
        context.get().setPacketHandled(true);
    }
}
