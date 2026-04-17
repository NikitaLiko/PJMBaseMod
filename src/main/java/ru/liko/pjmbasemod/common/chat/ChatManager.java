package ru.liko.pjmbasemod.common.chat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import ru.liko.pjmbasemod.Config;

import java.util.*;

/**
 * Менеджер для управления режимами чата игроков
 * Хранит режим чата для каждого игрока и предоставляет методы для фильтрации получателей сообщений
 */
public class ChatManager {
    private static final Map<UUID, ChatMode> playerChatModes = new HashMap<>();

    /**
     * Получить режим чата игрока
     */
    public static ChatMode getChatMode(ServerPlayer player) {
        return playerChatModes.getOrDefault(player.getUUID(), getDefaultChatMode());
    }

    /**
     * Установить режим чата для игрока
     */
    public static void setChatMode(ServerPlayer player, ChatMode mode) {
        playerChatModes.put(player.getUUID(), mode);
    }

    /**
     * Получить режим чата по умолчанию из конфигурации
     */
    public static ChatMode getDefaultChatMode() {
        String defaultMode = Config.getDefaultChatMode();
        return ChatMode.fromString(defaultMode);
    }

    /**
     * Переключить режим чата на следующий
     */
    public static ChatMode toggleChatMode(ServerPlayer player) {
        ChatMode currentMode = getChatMode(player);
        ChatMode nextMode = currentMode.next();
        setChatMode(player, nextMode);
        return nextMode;
    }

    /**
     * Очистить режим чата при выходе игрока
     */
    public static void clearChatMode(UUID playerUuid) {
        playerChatModes.remove(playerUuid);
    }

    /**
     * Получить список игроков, которые должны получить сообщение
     * в зависимости от режима чата отправителя
     */
    public static List<ServerPlayer> getRecipients(ServerPlayer sender, List<ServerPlayer> allPlayers) {
        if (!Config.isChatSystemEnabled()) {
            // Если система чата выключена, все видят все сообщения
            return allPlayers;
        }

        ChatMode mode = getChatMode(sender);

        return switch (mode) {
            case LOCAL -> getLocalRecipients(sender, allPlayers);
            case GLOBAL -> allPlayers;
            case TEAM -> getTeamRecipients(sender, allPlayers);
        };
    }

    /**
     * Получить список игроков в радиусе для локального чата
     */
    private static List<ServerPlayer> getLocalRecipients(ServerPlayer sender, List<ServerPlayer> allPlayers) {
        double radius = Config.getLocalChatRadius();
        double radiusSquared = radius * radius;
        Vec3 senderPos = sender.position();

        List<ServerPlayer> recipients = new ArrayList<>();

        for (ServerPlayer player : allPlayers) {
            // Проверяем, что игрок в том же измерении
            if (!player.level().dimension().equals(sender.level().dimension())) {
                continue;
            }

            // Проверяем расстояние
            double distanceSquared = player.position().distanceToSqr(senderPos);
            if (distanceSquared <= radiusSquared) {
                recipients.add(player);
            }
        }

        return recipients;
    }

    /**
     * Получить список игроков из той же команды
     */
    private static List<ServerPlayer> getTeamRecipients(ServerPlayer sender, List<ServerPlayer> allPlayers) {
        String senderTeam = getSenderTeamName(sender);

        if (senderTeam == null || senderTeam.isEmpty()) {
            // Если отправитель не в команде, сообщение видит только он
            return List.of(sender);
        }

        List<ServerPlayer> recipients = new ArrayList<>();

        for (ServerPlayer player : allPlayers) {
            String playerTeam = getSenderTeamName(player);
            if (senderTeam.equals(playerTeam)) {
                recipients.add(player);
            }
        }

        return recipients;
    }

    /**
     * Получить название команды игрока
     */
    private static String getSenderTeamName(ServerPlayer player) {
        return ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper.getTeamName(player);
    }

    /**
     * Получить количество игроков в каждом режиме чата
     */
    public static Map<ChatMode, Integer> getChatModeStatistics() {
        Map<ChatMode, Integer> stats = new HashMap<>();
        for (ChatMode mode : ChatMode.values()) {
            stats.put(mode, 0);
        }

        for (ChatMode mode : playerChatModes.values()) {
            stats.put(mode, stats.get(mode) + 1);
        }

        return stats;
    }

    /**
     * Очистить все данные (для перезагрузки сервера)
     */
    public static void clearAll() {
        playerChatModes.clear();
    }
}
