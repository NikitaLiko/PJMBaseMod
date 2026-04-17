package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.voice.PjmVoiceChatPlugin;

public record RadioSwitchPacket(boolean isPressed) implements CustomPacketPayload {

    public static final Type<RadioSwitchPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "radio_switch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RadioSwitchPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RadioSwitchPacket::isPressed,
            RadioSwitchPacket::new);

    @Override
    public Type<RadioSwitchPacket> type() {
        return TYPE;
    }

    public static void handle(RadioSwitchPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                if (payload.isPressed()) {
                    PjmVoiceChatPlugin.get().onPlayerStartRadio(serverPlayer);
                } else {
                    PjmVoiceChatPlugin.get().onPlayerStopRadio(serverPlayer);
                }
            }
        });
    }
}
