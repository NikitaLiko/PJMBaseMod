package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

import java.util.ArrayList;
import java.util.List;

/**
 * Пакет для открытия экрана статистики матча (Server → Client).
 * Содержит данные о победителе и статистику всех игроков.
 */
public record OpenMatchStatsPacket(
    String winnerTeam,
    String reason,
    int matchDurationSeconds,
    List<PlayerStatsEntry> playerStats
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenMatchStatsPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "open_match_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMatchStatsPacket> STREAM_CODEC =
        StreamCodec.of(OpenMatchStatsPacket::encode, OpenMatchStatsPacket::decode);

    /**
     * Запись статистики одного игрока.
     */
    public record PlayerStatsEntry(
        String playerName,
        String team,
        int kills,
        int deaths,
        int assists,
        int capturePoints,
        int score
    ) {
        public float getKDRatio() {
            return deaths == 0 ? kills : (float) kills / deaths;
        }
    }

    private static void encode(RegistryFriendlyByteBuf buf, OpenMatchStatsPacket packet) {
        buf.writeUtf(packet.winnerTeam);
        buf.writeUtf(packet.reason);
        buf.writeVarInt(packet.matchDurationSeconds);

        // Encode player stats list
        buf.writeVarInt(packet.playerStats.size());
        for (PlayerStatsEntry entry : packet.playerStats) {
            buf.writeUtf(entry.playerName);
            buf.writeUtf(entry.team);
            buf.writeVarInt(entry.kills);
            buf.writeVarInt(entry.deaths);
            buf.writeVarInt(entry.assists);
            buf.writeVarInt(entry.capturePoints);
            buf.writeVarInt(entry.score);
        }
    }

    private static OpenMatchStatsPacket decode(RegistryFriendlyByteBuf buf) {
        String winnerTeam = buf.readUtf();
        String reason = buf.readUtf();
        int matchDuration = buf.readVarInt();

        int size = buf.readVarInt();
        List<PlayerStatsEntry> stats = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stats.add(new PlayerStatsEntry(
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
            ));
        }

        return new OpenMatchStatsPacket(winnerTeam, reason, matchDuration, stats);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMatchStatsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
                if (proxy != null) {
                    proxy.handleOpenMatchStats(packet);
                }
            }
        });
    }

    // Compatibility getters
    public String getWinnerTeam() {
        return winnerTeam;
    }

    public String getReason() {
        return reason;
    }

    public int getMatchDurationSeconds() {
        return matchDurationSeconds;
    }

    public List<PlayerStatsEntry> getPlayerStats() {
        return playerStats;
    }
}
