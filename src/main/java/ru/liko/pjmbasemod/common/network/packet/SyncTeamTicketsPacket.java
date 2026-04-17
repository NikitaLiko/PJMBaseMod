package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

public record SyncTeamTicketsPacket(String team1, int tickets1, String team2, int tickets2) implements CustomPacketPayload {

    public static final Type<SyncTeamTicketsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_team_tickets"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTeamTicketsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncTeamTicketsPacket::team1,
            ByteBufCodecs.INT, SyncTeamTicketsPacket::tickets1,
            ByteBufCodecs.STRING_UTF8, SyncTeamTicketsPacket::team2,
            ByteBufCodecs.INT, SyncTeamTicketsPacket::tickets2,
            SyncTeamTicketsPacket::new);

    @Override
    public Type<SyncTeamTicketsPacket> type() {
        return TYPE;
    }

    public static void handle(SyncTeamTicketsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncTeamTickets(payload.team1, payload.tickets1, payload.team2, payload.tickets2);
            }
        });
    }
}
