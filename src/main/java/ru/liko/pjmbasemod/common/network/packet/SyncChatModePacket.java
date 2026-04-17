package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.chat.ChatMode;

/**
 * Packet for synchronizing chat mode with client.
 * NeoForge 1.21.1 format.
 */
public record SyncChatModePacket(
    String chatModeId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncChatModePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_chat_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncChatModePacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncChatModePacket::chatModeId,
            SyncChatModePacket::new
        );

    /**
     * Factory method for creating packet from ChatMode.
     */
    public static SyncChatModePacket create(ChatMode mode) {
        return new SyncChatModePacket(mode.getId());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncChatModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncChatMode(ChatMode.fromString(packet.chatModeId));
            }
        });
    }
}
