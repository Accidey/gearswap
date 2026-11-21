package com.xulai.gearswap.network;

import com.xulai.gearswap.GearPresetMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 客户端 -> 服务端：请求打开装备预设界面。 */
public record OpenGearMenuPayload() implements CustomPacketPayload {
    public static final OpenGearMenuPayload INSTANCE = new OpenGearMenuPayload();
    public static final CustomPacketPayload.Type<OpenGearMenuPayload> TYPE = GearNetwork.type("open_menu");
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGearMenuPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                player.openMenu(new SimpleMenuProvider(
                        (windowId, inventory, owner) -> new GearPresetMenu(windowId, inventory),
                        Component.translatable("container.gearswap.presets")));
            }
        });
    }
}
