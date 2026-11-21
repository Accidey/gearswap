package com.xulai.gearswap.network;

import com.xulai.gearswap.GearClientState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 服务端 -> 客户端：同步「当前生效的预设」下标，供 HUD 提示使用。 */
public record SyncPresetPayload(int active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPresetPayload> TYPE = GearNetwork.type("sync_preset");
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPresetPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncPresetPayload::active, SyncPresetPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> GearClientState.setActivePreset(this.active));
    }
}
