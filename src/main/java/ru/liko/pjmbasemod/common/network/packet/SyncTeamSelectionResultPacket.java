package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

public record SyncTeamSelectionResultPacket(boolean success, String message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncTeamSelectionResultPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_team_selection_result"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTeamSelectionResultPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, SyncTeamSelectionResultPacket::success,
        ByteBufCodecs.STRING_UTF8, SyncTeamSelectionResultPacket::message,
        SyncTeamSelectionResultPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTeamSelectionResultPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncTeamSelectionResult(msg);
            }
        });
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
