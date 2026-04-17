package ru.liko.pjmbasemod.common.match;

/**
 * Состояния матча:
 * - WAITING_FOR_PLAYERS: Ожидание игроков в лобби (или на сервере).
 * - STARTING: Обратный отсчет до начала игры.
 * - IN_PROGRESS: Матч идет.
 * - SHOWING_STATS: Показ статистики после окончания матча.
 * - VOTING: Голосование за следующую карту.
 * - ENDING: Переход к следующему матчу (техническая фаза).
 */
public enum MatchState {
    WAITING_FOR_PLAYERS,
    STARTING,
    IN_PROGRESS,
    SHOWING_STATS,
    VOTING,
    ENDING;

    public boolean isGameActive() {
        return this == IN_PROGRESS;
    }
}
