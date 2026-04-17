package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.player.PjmRank;

/**
 * Packet for notifying client about rank promotion/demotion.
 * NeoForge 1.21.1 format.
 */
public record RankUpdatePacket(
    String rankId,
    boolean promotion
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RankUpdatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "rank_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RankUpdatePacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RankUpdatePacket::rankId,
            ByteBufCodecs.BOOL, RankUpdatePacket::promotion,
            RankUpdatePacket::new
        );

    /**
     * Factory method for creating packet from PjmRank.
     */
    public static RankUpdatePacket create(PjmRank rank, boolean promotion) {
        return new RankUpdatePacket(rank.getId(), promotion);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RankUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleRankUpdate(packet.rankId, packet.promotion);
            }
        });
    }
}
