package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Пакет для открытия экрана лобби матча (Server → Client).
 * Пустой payload — экран берёт данные из ClientMatchData/ClientTeamConfig.
 */
public record OpenMatchLobbyPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenMatchLobbyPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "open_match_lobby"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMatchLobbyPacket> STREAM_CODEC =
        StreamCodec.unit(new OpenMatchLobbyPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMatchLobbyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
                if (proxy != null) {
                    proxy.handleOpenMatchLobby(packet);
                }
            }
        });
    }
}
