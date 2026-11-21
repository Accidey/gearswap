package com.xulai.gearswap.network;

import com.xulai.gearswap.GearSwap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GearNetwork {
    private GearNetwork() {}

    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(GearSwap.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int packetId;

    public static void register() {
        CHANNEL.registerMessage(packetId++, OpenGearMenuPayload.class,
                OpenGearMenuPayload::encode, OpenGearMenuPayload::decode, OpenGearMenuPayload::handle);
        CHANNEL.registerMessage(packetId++, SwapPresetPayload.class,
                SwapPresetPayload::encode, SwapPresetPayload::decode, SwapPresetPayload::handle);
        CHANNEL.registerMessage(packetId++, SyncPresetPayload.class,
                SyncPresetPayload::encode, SyncPresetPayload::decode, SyncPresetPayload::handle);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
