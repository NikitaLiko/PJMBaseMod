package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Packet for synchronizing team balance (player counts).
 * NeoForge 1.21.1 format.
 */
public record SyncTeamBalancePacket(
    String team1Name,
    int team1Balance,
    String team2Name,
    int team2Balance
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncTeamBalancePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_team_balance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTeamBalancePacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncTeamBalancePacket::team1Name,
            ByteBufCodecs.INT, SyncTeamBalancePacket::team1Balance,
            ByteBufCodecs.STRING_UTF8, SyncTeamBalancePacket::team2Name,
            ByteBufCodecs.INT, SyncTeamBalancePacket::team2Balance,
            SyncTeamBalancePacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTeamBalancePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncTeamBalance(packet);
            }
        });
    }

    // Legacy getters for compatibility
    public String getTeam1Name() { return team1Name; }
    public int getTeam1Balance() { return team1Balance; }
    public String getTeam2Name() { return team2Name; }
    public int getTeam2Balance() { return team2Balance; }
}
