package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.match.MatchState;

public record SyncMatchStatePacket(MatchState state, int timer) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMatchStatePacket> TYPE = new CustomPacketPayload.Type<>(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_match_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMatchStatePacket> STREAM_CODEC = StreamCodec.composite(
            // Reader for MatchState (Enum ordinal or string)
            // Using int for ordinal is simplest
            net.minecraft.network.codec.ByteBufCodecs.INT,
            s -> s.state().ordinal(),
            // Reader for timer
            net.minecraft.network.codec.ByteBufCodecs.INT,
            SyncMatchStatePacket::timer,
            // Constructor
            (ordinal, timer) -> new SyncMatchStatePacket(MatchState.values()[ordinal], timer));

    @Override
    public CustomPacketPayload.Type<SyncMatchStatePacket> type() {
        return TYPE;
    }

    public static void handle(SyncMatchStatePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncMatchState(payload.state, payload.timer);
            }
        });
    }
}
