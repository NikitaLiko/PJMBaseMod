package ru.liko.pjmbasemod.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Клиентская конфигурация команд.
 * Хранит синхронизированные названия команд с сервера.
 */
public class ClientTeamConfig {
    private static String team1Id = "team1";
    private static String team1DisplayName = "team1";
    private static String team2Id = "team2";
    private static String team2DisplayName = "team2";
    private static int balanceThreshold = 1;
    
    // Маппинг ID команды -> Отображаемое имя
    private static final Map<String, String> teamIdToDisplayName = new HashMap<>();

    /**
     * Устанавливает названия команд (вызывается при синхронизации с сервером)
     */
    public static void setTeamNames(String team1, String team2) {
        team1Id = team1;
        team1DisplayName = team1;
        team2Id = team2;
        team2DisplayName = team2;
        updateMapping();
    }
    
    /**
     * Устанавливает все конфигурации команд с ID и отображаемыми именами
     */
    public static void setTeamConfig(String team1Id, String team1Display, String team2Id, String team2Display, int threshold) {
        ClientTeamConfig.team1Id = team1Id;
        ClientTeamConfig.team1DisplayName = team1Display;
        ClientTeamConfig.team2Id = team2Id;
        ClientTeamConfig.team2DisplayName = team2Display;
        balanceThreshold = threshold;
        updateMapping();
    }
    
    private static void updateMapping() {
        teamIdToDisplayName.clear();
        teamIdToDisplayName.put(team1Id.toLowerCase(), team1DisplayName);
        teamIdToDisplayName.put(team2Id.toLowerCase(), team2DisplayName);
    }
    
    /**
     * Получает порог баланса команд
     */
    public static int getBalanceThreshold() {
        return balanceThreshold;
    }

    /**
     * Получает ID первой команды
     */
    public static String getTeam1Name() {
        return team1Id;
    }

    /**
     * Получает ID второй команды
     */
    public static String getTeam2Name() {
        return team2Id;
    }
    
    /**
     * Получает отображаемое имя команды по её ID
     */
    public static String getDisplayName(String teamId) {
        if (teamId == null) return teamId;
        return teamIdToDisplayName.getOrDefault(teamId.toLowerCase(), teamId);
    }

    /**
     * Проверяет, принадлежит ли команда ко второй фракции
     */
    public static boolean isTeam2(String teamName) {
        if (teamName == null) {
            return false;
        }
        return teamName.equalsIgnoreCase(team2Id);
    }
    
    /**
     * Сбрасывает конфигурацию команд к значениям по умолчанию
     */
    public static void reset() {
        team1Id = "team1";
        team1DisplayName = "team1";
        team2Id = "team2";
        team2DisplayName = "team2";
        balanceThreshold = 1;
        updateMapping();
    }
}

