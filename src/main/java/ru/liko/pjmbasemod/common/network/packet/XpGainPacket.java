package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Packet for notifying client about XP gain.
 * NeoForge 1.21.1 format.
 */
public record XpGainPacket(
    int amount,
    String reason
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<XpGainPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "xp_gain"));

    public static final StreamCodec<RegistryFriendlyByteBuf, XpGainPacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.INT, XpGainPacket::amount,
            ByteBufCodecs.STRING_UTF8, XpGainPacket::reason,
            XpGainPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(XpGainPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleXpGain(packet.amount, packet.reason);
            }
        });
    }
}
