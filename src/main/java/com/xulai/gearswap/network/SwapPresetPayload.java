package com.xulai.gearswap.network;

import com.xulai.gearswap.GearSwap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.xulai.gearswap.PresetSwitcher;

/** 客户端 -> 服务端：快捷键请求切换到第 preset 套预设。 */
public record SwapPresetPayload(int preset) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SwapPresetPayload> TYPE = GearNetwork.type("swap_preset");
    public static final StreamCodec<RegistryFriendlyByteBuf, SwapPresetPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SwapPresetPayload::preset, SwapPresetPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PresetSwitcher.swap(player, this.preset);
            } else {
                GearSwap.LOGGER.warn("Ignoring swap request from non-server player");
            }
        });
    }
}
