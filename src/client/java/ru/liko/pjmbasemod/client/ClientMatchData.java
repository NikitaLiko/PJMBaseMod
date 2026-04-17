package ru.liko.pjmbasemod.client;

import ru.liko.pjmbasemod.common.match.MatchState;

public class ClientMatchData {
    private static MatchState currentState = MatchState.WAITING_FOR_PLAYERS;
    private static int timer = 0;
    private static String currentMapId = "";
    private static String currentMapDisplayName = "";
    private static String ticketTeam1 = "";
    private static int ticketCount1 = 0;
    private static String ticketTeam2 = "";
    private static int ticketCount2 = 0;

    public static void update(MatchState state, int time) {
        MatchState previous = currentState;
        currentState = state;
        timer = time;

        // Handle Screen Transitions
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null)
            return; // Don't open screens if not in game/connected

        if (previous != state) {
            // SHOWING_STATS и VOTING экраны теперь открываются через отдельные пакеты:
            // - OpenMatchStatsPacket открывает MatchStatsScreen с данными статистики
            // - OpenMapVotingPacket открывает MapVotingScreen
            // Здесь обрабатываем только закрытие экранов при возврате в лобби

            if (state == MatchState.VOTING) {
                // Fallback: если экран статистики всё ещё открыт — закрываем
                // (экран голосования откроется через OpenMapVotingPacket)
                if (mc.screen instanceof ru.liko.pjmbasemod.client.gui.screen.MatchStatsScreen) {
                    mc.setScreen(null);
                }
            } else if (state == MatchState.WAITING_FOR_PLAYERS && 
                       (previous == MatchState.ENDING || previous == MatchState.VOTING)) {
                // Закрываем экраны при возврате в лобби
                if (mc.screen instanceof ru.liko.pjmbasemod.client.gui.screen.MatchStatsScreen ||
                        mc.screen instanceof ru.liko.pjmbasemod.client.gui.screen.MapVotingScreen) {
                    mc.setScreen(null);
                }
            } else if (state == MatchState.IN_PROGRESS) {
                // Закрываем все match-related экраны при старте матча
                if (mc.screen instanceof ru.liko.pjmbasemod.client.gui.screen.MatchStatsScreen ||
                        mc.screen instanceof ru.liko.pjmbasemod.client.gui.screen.MapVotingScreen) {
                    mc.setScreen(null);
                }
            }
        }
    }

    public static MatchState getState() {
        return currentState;
    }

    public static int getTimer() {
        return timer;
    }

    public static void updateMapInfo(String mapId, String displayName) {
        currentMapId = mapId;
        currentMapDisplayName = displayName;
    }

    public static String getCurrentMapId() {
        return currentMapId;
    }

    public static String getCurrentMapDisplayName() {
        return currentMapDisplayName;
    }

    public static void updateTickets(String team1, int count1, String team2, int count2) {
        ticketTeam1 = team1;
        ticketCount1 = count1;
        ticketTeam2 = team2;
        ticketCount2 = count2;
    }

    public static String getTicketTeam1() {
        return ticketTeam1;
    }

    public static int getTicketCount1() {
        return ticketCount1;
    }

    public static String getTicketTeam2() {
        return ticketTeam2;
    }

    public static int getTicketCount2() {
        return ticketCount2;
    }
}
