package ru.liko.pjmbasemod.common.network.packet;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;

/**
 * Packet for synchronizing player data from server to client.
 * NeoForge 1.21.1 format using record and StreamCodec.
 */
public record SyncPjmDataPacket(
    int entityId,
    String rankId,
    int rankPoints,
    String playerClassId,
    String teamId,
    String activeSkinId,
    List<String> activeItemIds
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncPjmDataPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_wrb_data"));

    // NeoForge 1.21.1: StreamCodec.composite supports max 6 parameters, use manual encode/decode
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPjmDataPacket> STREAM_CODEC = 
        StreamCodec.of(SyncPjmDataPacket::encode, SyncPjmDataPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncPjmDataPacket packet) {
        buf.writeInt(packet.entityId);
        buf.writeUtf(packet.rankId);
        buf.writeInt(packet.rankPoints);
        buf.writeUtf(packet.playerClassId);
        buf.writeUtf(packet.teamId);
        buf.writeUtf(packet.activeSkinId);
        buf.writeInt(packet.activeItemIds.size());
        for (String itemId : packet.activeItemIds) {
            buf.writeUtf(itemId);
        }
    }

    private static SyncPjmDataPacket decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        String rankId = buf.readUtf();
        int rankPoints = buf.readInt();
        String playerClassId = buf.readUtf();
        String teamId = buf.readUtf();
        String activeSkinId = buf.readUtf();
        int itemCount = buf.readInt();
        java.util.ArrayList<String> activeItemIds = new java.util.ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            activeItemIds.add(buf.readUtf());
        }
        return new SyncPjmDataPacket(entityId, rankId, rankPoints, playerClassId, teamId, activeSkinId, activeItemIds);
    }

    /**
     * Factory method for creating packet from player data.
     */
    public static SyncPjmDataPacket fromPlayerData(int entityId, PjmPlayerData data) {
        return new SyncPjmDataPacket(
            entityId,
            data.getRank().getPersistenceKey(),
            data.getRankPoints(),
            data.getPlayerClass().getId(),
            data.getTeam(),
            data.getActiveSkinId(),
            List.copyOf(data.getActiveItemIds())
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle packet on client side.
     */
    public static void handle(SyncPjmDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncPjmData(packet.entityId, packet.rankId, packet.rankPoints,
                    packet.playerClassId, packet.teamId, packet.activeSkinId, packet.activeItemIds);
            }
        });
    }
}
