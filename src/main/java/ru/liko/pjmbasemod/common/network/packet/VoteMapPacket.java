package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.match.MatchManager;
import ru.liko.pjmbasemod.common.match.MatchState;

/**
 * Пакет голосования за карту (Client -> Server)
 */
public record VoteMapPacket(String mapId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VoteMapPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "vote_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoteMapPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            VoteMapPacket::mapId,
            VoteMapPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handler for server-side processing.
     */
    public static void handle(VoteMapPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Only allow voting during VOTING state
                if (MatchManager.get().getState() == MatchState.VOTING) {
                    MatchManager.get().voteForMap(player, packet.mapId());
                }
            }
        });
    }
}
