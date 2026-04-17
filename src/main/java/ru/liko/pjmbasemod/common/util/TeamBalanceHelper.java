package ru.liko.pjmbasemod.common.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import ru.liko.pjmbasemod.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * Утилита для работы с балансом команд.
 * Проверяет количество игроков в командах и возможность присоединения к команде.
 */
public class TeamBalanceHelper {
    
    /**
     * Получить количество игроков в указанной команде
     * @param server Сервер Minecraft
     * @param teamName Название команды (из Config.TEAM_1_NAME или TEAM_2_NAME)
     * @return Количество игроков в команде
     */
    public static int getTeamPlayerCount(MinecraftServer server, String teamName) {
        if (server == null || teamName == null || teamName.isEmpty()) {
            return 0;
        }
        
        Scoreboard scoreboard = server.getScoreboard();
        
        // Подсчитываем количество игроков в команде
        // Проверяем всех игроков и их команды
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team playerTeam = scoreboard.getPlayersTeam(player.getScoreboardName());
            if (playerTeam != null && playerTeam.getName().equals(teamName)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Проверить, можно ли присоединиться к указанной команде с учетом баланса
     * @param server Сервер Minecraft
     * @param teamName Название команды, к которой хочет присоединиться игрок
     * @param balanceThreshold Максимальная разница в количестве игроков (из Config.TEAM_BALANCE_THRESHOLD)
     * @return true если можно присоединиться, false если команда переполнена или баланс нарушен
     */
    public static boolean canJoinTeam(MinecraftServer server, String teamName, int balanceThreshold) {
        if (server == null || teamName == null || teamName.isEmpty()) {
            return false;
        }
        
        // Если баланс отключен (threshold = 0), разрешаем присоединение
        if (balanceThreshold <= 0) {
            return true;
        }
        
        String team1Name = Config.getTeam1Name();
        String team2Name = Config.getTeam2Name();
        
        int team1Count = getTeamPlayerCount(server, team1Name);
        int team2Count = getTeamPlayerCount(server, team2Name);
        
        // Определяем, к какой команде хочет присоединиться игрок
        boolean joiningTeam1 = teamName.equalsIgnoreCase(team1Name);
        int targetTeamCount = joiningTeam1 ? team1Count : team2Count;
        int otherTeamCount = joiningTeam1 ? team2Count : team1Count;
        
        // Логика баланса:
        // Игрок может присоединиться к команде, если после его присоединения
        // эта команда не будет больше другой команды на величину больше balanceThreshold
        // 
        // Пример: threshold = 2
        // Team1 = 5, Team2 = 3
        // Если игрок хочет в Team1: 5+1 = 6, разница с Team2 = 6-3 = 3 > 2 -> НЕЛЬЗЯ
        // Если игрок хочет в Team2: 3+1 = 4, разница с Team1 = 5-4 = 1 <= 2 -> МОЖНО
        return (targetTeamCount + 1) <= (otherTeamCount + balanceThreshold);
    }
    
    /**
     * Получить информацию о балансе команд
     * @param server Сервер Minecraft
     * @return Map с количеством игроков в каждой команде: "team1" -> count1, "team2" -> count2
     */
    public static Map<String, Integer> getTeamBalanceInfo(MinecraftServer server) {
        Map<String, Integer> balanceInfo = new HashMap<>();
        
        if (server == null) {
            balanceInfo.put("team1", 0);
            balanceInfo.put("team2", 0);
            return balanceInfo;
        }
        
        String team1Name = Config.getTeam1Name();
        String team2Name = Config.getTeam2Name();
        
        int team1Count = getTeamPlayerCount(server, team1Name);
        int team2Count = getTeamPlayerCount(server, team2Name);
        
        balanceInfo.put("team1", team1Count);
        balanceInfo.put("team2", team2Count);
        
        return balanceInfo;
    }
    
    /**
     * Проверить, назначена ли команда игроку
     * @param player Игрок
     * @return true если команда назначена, false если нет
     */
    public static boolean hasTeam(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        
        String teamName = ScoreboardTeamHelper.getTeamName(player);
        return teamName != null && !teamName.isEmpty() && !teamName.equals(ScoreboardTeamHelper.NO_TEAM_PLACEHOLDER);
    }
}

