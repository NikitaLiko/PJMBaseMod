package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Packet for synchronizing player's class selection zone status.
 * NeoForge 1.21.1 format.
 */
public record SyncInZoneStatusPacket(
    boolean inZone
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncInZoneStatusPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_in_zone_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInZoneStatusPacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncInZoneStatusPacket::inZone,
            SyncInZoneStatusPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncInZoneStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncInZoneStatus(packet.inZone);
            }
        });
    }

    // Legacy getter for compatibility
    public boolean isInZone() {
        return inZone;
    }
}

