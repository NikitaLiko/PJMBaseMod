package ru.liko.pjmbasemod.client.death;

/**
 * Клиентское хранилище статистики смертей.
 * Синхронизируется с сервера через SyncDeathStatsPacket.
 */
public final class ClientDeathStats {

    private static String currentTeam = "";
    private static int playerDeaths = 0;
    private static int teamDeaths = 0;

    private ClientDeathStats() {}

    /**
     * Обновить статистику (вызывается при получении пакета с сервера)
     */
    public static void update(String teamId, int myDeaths, int myTeamDeaths) {
        currentTeam = teamId != null ? teamId : "";
        playerDeaths = myDeaths;
        teamDeaths = myTeamDeaths;
    }

    /**
     * Получить текущую команду игрока
     */
    public static String getCurrentTeam() {
        return currentTeam;
    }

    /**
     * Получить количество смертей игрока
     */
    public static int getPlayerDeaths() {
        return playerDeaths;
    }

    /**
     * Получить количество смертей команды игрока
     */
    public static int getTeamDeaths() {
        return teamDeaths;
    }

    /**
     * Проверить, состоит ли игрок в команде
     */
    public static boolean hasTeam() {
        return currentTeam != null && !currentTeam.isEmpty();
    }

    /**
     * Сбросить статистику (при выходе из мира)
     */
    public static void reset() {
        currentTeam = "";
        playerDeaths = 0;
        teamDeaths = 0;
    }
}
