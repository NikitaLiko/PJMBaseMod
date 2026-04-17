package ru.liko.pjmbasemod.common.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.match.MatchManager;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class PjmVoiceChatPlugin implements VoicechatPlugin {

    public static final String PLUGIN_ID = Pjmbasemod.MODID;
    private static PjmVoiceChatPlugin INSTANCE;

    @Nullable
    public VoicechatServerApi serverApi;

    // Множество игроков, которые в данный момент зажали кнопку рации
    private final Set<UUID> broadcastingPlayers = ConcurrentHashMap.newKeySet();

    public PjmVoiceChatPlugin() {
        INSTANCE = this;
    }

    public static PjmVoiceChatPlugin get() {
        return INSTANCE;
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        this.serverApi = event.getVoicechat();
    }

    /**
     * Обработка пакета микрофона от любого игрока.
     */
    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (serverApi == null)
            return;

        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null)
            return;

        ServerPlayer sender = (ServerPlayer) senderConnection.getPlayer().getPlayer();
        if (sender == null)
            return;
        UUID senderId = sender.getUUID();

        // Если игрок зажал кнопку рации
        if (broadcastingPlayers.contains(senderId)) {
            String team = MatchManager.get().getPlayerTeam(senderId);
            if (team == null || team.isEmpty())
                return;

            // Уникальный channelId для радио (инвертируем биты UUID), чтобы не
            // конфликтовало с 3D локальным звуком
            UUID radioChannelId = new UUID(senderId.getMostSignificantBits(), ~senderId.getLeastSignificantBits());

            // Формируем StaticSoundPacket для рассылки тиммейтам
            StaticSoundPacket staticPacket = event.getPacket().staticSoundPacketBuilder()
                    .channelId(radioChannelId)
                    .build();

            // Ищем всех подключенных игроков к VoiceChat
            for (ServerPlayer player : sender.level().getServer().getPlayerList().getPlayers()) {
                if (player.getUUID().equals(senderId))
                    continue; // Самому себе не отправляем

                // Проверяем, в одной ли они команде
                String otherTeam = MatchManager.get().getPlayerTeam(player.getUUID());
                if (team.equals(otherTeam)) {
                    VoicechatConnection receiverConnection = serverApi.getConnectionOf(player.getUUID());
                    if (receiverConnection != null) {
                        try {
                            serverApi.sendStaticSoundPacketTo(receiverConnection, staticPacket);
                        } catch (Exception e) {
                            System.err.println("Failed to send radio packet: " + e.getMessage());
                        }
                    }
                }
            }
        }
        // Мы НЕ отменяем оригинальный event (event.cancel() нет),
        // поэтому локальный 3D звук также воспроизведётся для врагов и союзников
        // поблизости (как в Squad).
    }

    /**
     * Вызывается, когда игрок нажимает кнопку рации (включает передачу).
     */
    public void onPlayerStartRadio(ServerPlayer player) {
        if (broadcastingPlayers.add(player.getUUID())) {
            notifyTeammates(player, true);
        }
    }

    /**
     * Вызывается, когда игрок отпускает кнопку рации (выключает передачу).
     */
    public void onPlayerStopRadio(ServerPlayer player) {
        if (broadcastingPlayers.remove(player.getUUID())) {
            notifyTeammates(player, false);
        }
    }

    private void notifyTeammates(ServerPlayer sender, boolean isStart) {
        String team = MatchManager.get().getPlayerTeam(sender.getUUID());
        if (team == null || team.isEmpty())
            return;

        ru.liko.pjmbasemod.common.network.packet.RadioEventPacket packet = new ru.liko.pjmbasemod.common.network.packet.RadioEventPacket(
                isStart);

        for (ServerPlayer player : sender.level().getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(sender.getUUID()))
                continue;
            String otherTeam = MatchManager.get().getPlayerTeam(player.getUUID());
            if (team.equals(otherTeam)) {
                ru.liko.pjmbasemod.common.network.PjmNetworking.sendToClient(packet, player);
            }
        }
    }
}
