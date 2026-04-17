package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.chat.ChatManager;
import ru.liko.pjmbasemod.common.chat.ChatMode;
import ru.liko.pjmbasemod.common.network.PjmNetworking;

/**
 * Packet for changing chat mode (Client → Server).
 * NeoForge 1.21.1 format.
 */
public record ChangeChatModePacket(
    String targetMode,
    boolean isToggle
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChangeChatModePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "change_chat_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeChatModePacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChangeChatModePacket::targetMode,
            ByteBufCodecs.BOOL, ChangeChatModePacket::isToggle,
            ChangeChatModePacket::new
        );

    /** Singleton for toggle mode - used when just pressing the key */
    public static final ChangeChatModePacket TOGGLE_INSTANCE = new ChangeChatModePacket("", true);

    /**
     * Create packet for toggling to next mode.
     */
    public static ChangeChatModePacket createToggle() {
        return TOGGLE_INSTANCE;
    }

    /**
     * Create packet for setting specific mode.
     */
    public static ChangeChatModePacket setMode(ChatMode mode) {
        return new ChangeChatModePacket(mode.getId(), false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChangeChatModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) {
                ChatMode newMode;
                if (packet.isToggle()) {
                    newMode = ChatManager.toggleChatMode(sender);
                } else {
                    newMode = ChatMode.fromString(packet.targetMode);
                    ChatManager.setChatMode(sender, newMode);
                }

                PjmNetworking.sendToClient(SyncChatModePacket.create(newMode), sender);

                sender.displayClientMessage(
                    Component.literal("§aРежим чата изменен на: ").append(newMode.getDisplayName()),
                    true
                );
            }
        });
    }
}
