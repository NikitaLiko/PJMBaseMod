package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Пакет синхронизации статистики смертей с клиентом.
 */
public record SyncDeathStatsPacket(String teamId, int playerDeaths, int teamDeaths) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncDeathStatsPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_death_stats"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDeathStatsPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SyncDeathStatsPacket::teamId,
        ByteBufCodecs.INT, SyncDeathStatsPacket::playerDeaths,
        ByteBufCodecs.INT, SyncDeathStatsPacket::teamDeaths,
        SyncDeathStatsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncDeathStatsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncDeathStats(packet.teamId(), packet.playerDeaths(), packet.teamDeaths());
            }
        });
    }
}
