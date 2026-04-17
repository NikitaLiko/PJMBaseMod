package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

import java.util.List;

/**
 * Пакет для открытия экрана голосования за карту (Server → Client).
 * Содержит список доступных карт (id + displayName).
 */
public record OpenMapVotingPacket(List<MapEntry> maps) implements CustomPacketPayload {

    public record MapEntry(String mapId, String displayName) {
    }

    public static final CustomPacketPayload.Type<OpenMapVotingPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "open_map_voting"));

    private static final StreamCodec<RegistryFriendlyByteBuf, MapEntry> MAP_ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MapEntry::mapId,
            ByteBufCodecs.STRING_UTF8, MapEntry::displayName,
            MapEntry::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMapVotingPacket> STREAM_CODEC = StreamCodec.composite(
            MAP_ENTRY_CODEC.apply(ByteBufCodecs.list()), OpenMapVotingPacket::maps,
            OpenMapVotingPacket::new);

    public static OpenMapVotingPacket fromServer() {
        List<MapEntry> entries = new java.util.ArrayList<>();
        var allConfigs = ru.liko.pjmbasemod.common.map.config.MapConfigManager.getAllConfigs();
        List<String> rotation = ru.liko.pjmbasemod.common.map.MapRotationManager.getRotation();

        if (rotation.isEmpty()) {
            // Если ротация пуста, используем все доступные карты
            for (var entry : allConfigs.entrySet()) {
                entries.add(new MapEntry(entry.getKey(), entry.getValue().getDisplayName()));
            }
        } else {
            // Иначе используем только список карт из ротации
            for (String mapId : rotation) {
                var config = allConfigs.get(mapId);
                String displayName = config != null ? config.getDisplayName() : mapId;
                entries.add(new MapEntry(mapId, displayName));
            }
        }

        return new OpenMapVotingPacket(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMapVotingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
                if (proxy != null) {
                    proxy.handleOpenMapVoting(packet);
                }
            }
        });
    }
}
