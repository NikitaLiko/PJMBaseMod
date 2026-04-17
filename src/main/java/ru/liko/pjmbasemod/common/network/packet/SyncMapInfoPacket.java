package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

public record SyncMapInfoPacket(String mapId, String mapDisplayName) implements CustomPacketPayload {

    public static final Type<SyncMapInfoPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_map_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMapInfoPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncMapInfoPacket::mapId,
            ByteBufCodecs.STRING_UTF8, SyncMapInfoPacket::mapDisplayName,
            SyncMapInfoPacket::new);

    @Override
    public Type<SyncMapInfoPacket> type() {
        return TYPE;
    }

    public static void handle(SyncMapInfoPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncMapInfo(payload.mapId, payload.mapDisplayName);
            }
        });
    }
}
