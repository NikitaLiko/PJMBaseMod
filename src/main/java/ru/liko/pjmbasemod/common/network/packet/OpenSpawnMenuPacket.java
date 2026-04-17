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
 * Пакет для открытия экрана выбора точки спавна (Server → Client).
 * Содержит список доступных точек спавна для игрока.
 */
public record OpenSpawnMenuPacket(
    List<SpawnPoint> spawnPoints,
    int respawnCooldown
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenSpawnMenuPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "open_spawn_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSpawnMenuPacket> STREAM_CODEC =
        StreamCodec.of(OpenSpawnMenuPacket::encode, OpenSpawnMenuPacket::decode);

    /**
     * Точка спавна.
     */
    public record SpawnPoint(
        String id,
        String displayName,
        String type, // "team_base", "rally_point", "fob", "squad_leader"
        boolean available
    ) {}

    private static void encode(RegistryFriendlyByteBuf buf, OpenSpawnMenuPacket packet) {
        buf.writeVarInt(packet.spawnPoints.size());
        for (SpawnPoint sp : packet.spawnPoints) {
            buf.writeUtf(sp.id);
            buf.writeUtf(sp.displayName);
            buf.writeUtf(sp.type);
            buf.writeBoolean(sp.available);
        }
        buf.writeVarInt(packet.respawnCooldown);
    }

    private static OpenSpawnMenuPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<SpawnPoint> points = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            points.add(new SpawnPoint(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean()
            ));
        }
        int cooldown = buf.readVarInt();
        return new OpenSpawnMenuPacket(points, cooldown);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenSpawnMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
                if (proxy != null) {
                    proxy.handleOpenSpawnMenu(packet);
                }
            }
        });
    }

    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    public int getRespawnCooldown() {
        return respawnCooldown;
    }
}
