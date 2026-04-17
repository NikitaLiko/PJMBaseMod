package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.gamemode.ControlPointSnapshot;

/**
 * Packet for syncing control point data.
 * NeoForge 1.21.1: record implementing CustomPacketPayload.
 */
public record SyncControlPointPacket(ControlPointSnapshot snapshot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncControlPointPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_control_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncControlPointPacket> STREAM_CODEC = 
        StreamCodec.of(SyncControlPointPacket::encode, SyncControlPointPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncControlPointPacket packet) {
        packet.snapshot.write(buf);
    }

    private static SyncControlPointPacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncControlPointPacket(ControlPointSnapshot.read(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncControlPointPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncControlPoint(packet.snapshot);
            }
        });
    }
}

