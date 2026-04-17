package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

public record RadioEventPacket(boolean isStart) implements CustomPacketPayload {

    public static final Type<RadioEventPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "radio_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RadioEventPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RadioEventPacket::isStart,
            RadioEventPacket::new);

    @Override
    public Type<RadioEventPacket> type() {
        return TYPE;
    }

    public static void handle(RadioEventPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleRadioEvent(payload.isStart());
            }
        });
    }
}
