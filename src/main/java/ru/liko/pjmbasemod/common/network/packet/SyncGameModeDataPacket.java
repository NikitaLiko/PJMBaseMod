package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.gamemode.ControlPointSnapshot;

import java.util.ArrayList;
import java.util.List;

public record SyncGameModeDataPacket(List<ControlPointSnapshot> snapshots) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncGameModeDataPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_game_mode_data"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGameModeDataPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeVarInt(packet.snapshots().size());
            packet.snapshots().forEach(snapshot -> snapshot.write(buf));
        },
        buf -> {
            int size = buf.readVarInt();
            List<ControlPointSnapshot> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(ControlPointSnapshot.read(buf));
            }
            return new SyncGameModeDataPacket(list);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncGameModeDataPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncGameModeData(packet.snapshots());
            }
        });
    }
}
