package ru.liko.pjmbasemod.common.network.packet;

import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.customization.CustomizationOption;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet for syncing customization options from server to client.
 * NeoForge 1.21.1: record implementing CustomPacketPayload.
 */
public record SyncCustomizationOptionsPacket(List<CustomizationOption> options) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncCustomizationOptionsPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_customization_options"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCustomizationOptionsPacket> STREAM_CODEC = 
        StreamCodec.of(SyncCustomizationOptionsPacket::encode, SyncCustomizationOptionsPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncCustomizationOptionsPacket packet) {
        buf.writeInt(packet.options.size());
        for (CustomizationOption option : packet.options) {
            buf.writeNbt(option.serialize());
        }
    }

    private static SyncCustomizationOptionsPacket decode(RegistryFriendlyByteBuf buf) {
        List<CustomizationOption> options = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            options.add(CustomizationOption.deserialize(buf.readNbt()));
        }
        return new SyncCustomizationOptionsPacket(options);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncCustomizationOptionsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            LOGGER.info("[PJM] Client received {} customization options from server", packet.options.size());
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncCustomizationOptions(packet.options);
            }
        });
    }
}
