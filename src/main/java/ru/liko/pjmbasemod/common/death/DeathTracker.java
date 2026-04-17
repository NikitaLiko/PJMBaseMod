package ru.liko.pjmbasemod.common.death;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.SyncDeathStatsPacket;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmPlayerDataProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Серверный трекер смертей игроков и команд.
 * Отслеживает смерти по командам и по отдельным игрокам.
 */
public final class DeathTracker {

    private static final Map<String, Integer> teamDeaths = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> playerDeaths = new ConcurrentHashMap<>();

    private DeathTracker() {}

    /**
     * Регистрирует смерть игрока
     */
    public static void recordDeath(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Увеличиваем счётчик смертей игрока
        playerDeaths.merge(playerId, 1, Integer::sum);
        
        // Получаем команду игрока и увеличиваем счётчик команды
        // NeoForge 1.21.1: Use Data Attachments
        PjmPlayerData data = PjmPlayerDataProvider.get(player);
        String team = data.getTeam();
        if (team != null && !team.isEmpty()) {
            teamDeaths.merge(team, 1, Integer::sum);
        }
        
        // Синхронизируем статистику с клиентом
        syncToPlayer(player);
    }

    /**
     * Получить количество смертей игрока
     */
    public static int getPlayerDeaths(UUID playerId) {
        return playerDeaths.getOrDefault(playerId, 0);
    }

    /**
     * Получить количество смертей команды
     */
    public static int getTeamDeaths(String teamId) {
        if (teamId == null || teamId.isEmpty()) return 0;
        return teamDeaths.getOrDefault(teamId, 0);
    }

    /**
     * Получить смерти Team1
     */
    public static int getTeam1Deaths() {
        return getTeamDeaths(Config.getTeam1Name());
    }

    /**
     * Получить смерти Team2
     */
    public static int getTeam2Deaths() {
        return getTeamDeaths(Config.getTeam2Name());
    }

    /**
     * Сбросить всю статистику смертей
     */
    public static void resetAll() {
        teamDeaths.clear();
        playerDeaths.clear();
    }

    /**
     * Сбросить статистику смертей команды
     */
    public static void resetTeam(String teamId) {
        teamDeaths.remove(teamId);
    }

    /**
     * Сбросить статистику смертей игрока
     */
    public static void resetPlayer(UUID playerId) {
        playerDeaths.remove(playerId);
    }

    /**
     * Синхронизировать статистику с конкретным игроком
     */
    public static void syncToPlayer(ServerPlayer player) {
        // NeoForge 1.21.1: Use Data Attachments
        PjmPlayerData data = PjmPlayerDataProvider.get(player);
        String team = data.getTeam();
        int myDeaths = getPlayerDeaths(player.getUUID());
        int myTeamDeaths = getTeamDeaths(team);
        
        PjmNetworking.sendToClient(
            new SyncDeathStatsPacket(team, myDeaths, myTeamDeaths),
            player
        );
    }

    /**
     * Синхронизировать статистику со всеми игроками на сервере
     */
    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }
}
