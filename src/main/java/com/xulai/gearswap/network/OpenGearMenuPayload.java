package com.xulai.gearswap.network;

import java.util.function.Supplier;

import com.xulai.gearswap.GearPresetMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;

public record OpenGearMenuPayload() {
    public static final OpenGearMenuPayload INSTANCE = new OpenGearMenuPayload();

    public static void encode(OpenGearMenuPayload payload, FriendlyByteBuf buf) {}

    public static OpenGearMenuPayload decode(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public static void handle(OpenGearMenuPayload payload, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                player.openMenu(new SimpleMenuProvider(
                        (windowId, inventory, owner) -> new GearPresetMenu(windowId, inventory),
                        Component.translatable("container.gearswap.presets")));
            }
        });
        context.get().setPacketHandled(true);
    }
}
