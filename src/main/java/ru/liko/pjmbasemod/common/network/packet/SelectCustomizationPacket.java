package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmPlayerDataProvider;
import ru.liko.pjmbasemod.common.customization.CustomizationManager;
import ru.liko.pjmbasemod.common.customization.CustomizationOption;
import ru.liko.pjmbasemod.common.customization.CustomizationType;

/**
 * Packet for selecting customization options (skin/item).
 * NeoForge 1.21.1: record implementing CustomPacketPayload.
 */
public record SelectCustomizationPacket(
    String optionId,
    boolean isSkin,
    boolean selected
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectCustomizationPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "select_customization"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectCustomizationPacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectCustomizationPacket::optionId,
            ByteBufCodecs.BOOL, SelectCustomizationPacket::isSkin,
            ByteBufCodecs.BOOL, SelectCustomizationPacket::selected,
            SelectCustomizationPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectCustomizationPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            CustomizationOption option = CustomizationManager.load().getOption(packet.optionId);
            if (option == null) return; // Invalid option

            PjmPlayerData data = PjmPlayerDataProvider.get(player);
            if (packet.isSkin && option.getType() == CustomizationType.SKIN) {
                data.setActiveSkinId(packet.optionId);
            } else if (!packet.isSkin && option.getType() == CustomizationType.ITEM) {
                if (packet.selected) {
                    data.addActiveItemId(packet.optionId);
                } else {
                    data.removeActiveItemId(packet.optionId);
                }
            }
            
            // Sync to the player themselves
            ru.liko.pjmbasemod.common.network.PjmNetworking.sendToClient(
                SyncPjmDataPacket.fromPlayerData(player.getId(), data), 
                player
            );
            
            // Sync to all players tracking this player (so they see the skin change)
            ru.liko.pjmbasemod.common.network.PjmNetworking.sendToTracking(
                SyncPjmDataPacket.fromPlayerData(player.getId(), data), 
                player
            );
        });
    }
}
