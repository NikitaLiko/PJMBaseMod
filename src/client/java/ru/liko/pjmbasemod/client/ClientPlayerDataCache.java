package ru.liko.pjmbasemod.client;

import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmRank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Клиентское хранилище данных игроков для отображения в табе.
 * Обновляется через SyncPjmDataPacket.
 */
public final class ClientPlayerDataCache {
    
    private static final Map<UUID, PlayerDataSnapshot> cache = new HashMap<>();
    
    private ClientPlayerDataCache() {}
    
    /**
     * Снимок данных игрока для кэширования
     */
    public static class PlayerDataSnapshot {
        private final PjmRank rank;
        private final PjmPlayerClass playerClass;
        private final String team;
        
        public PlayerDataSnapshot(PjmRank rank, PjmPlayerClass playerClass, String team) {
            // Если ранг null или NOT_ENLISTED, используем PRIVATE по умолчанию
            PjmRank finalRank = rank != null ? (rank == PjmRank.NOT_ENLISTED ? PjmRank.PRIVATE : rank) : PjmRank.PRIVATE;
            this.rank = finalRank;
            this.playerClass = playerClass != null ? playerClass : PjmPlayerClass.NONE;
            this.team = team != null ? team : "";
        }
        
        public PjmRank getRank() {
            return rank;
        }
        
        public PjmPlayerClass getPlayerClass() {
            return playerClass;
        }
        
        public String getTeam() {
            return team;
        }
    }
    
    /**
     * Обновить данные игрока в кэше
     */
    public static void update(UUID playerId, PjmRank rank, PjmPlayerClass playerClass, String team) {
        cache.put(playerId, new PlayerDataSnapshot(rank, playerClass, team));
    }
    
    /**
     * Получить данные игрока из кэша
     */
    public static PlayerDataSnapshot get(UUID playerId) {
        return cache.getOrDefault(playerId, new PlayerDataSnapshot(PjmRank.PRIVATE, PjmPlayerClass.NONE, ""));
    }
    
    /**
     * Удалить данные игрока из кэша (при выходе)
     */
    public static void remove(UUID playerId) {
        cache.remove(playerId);
    }
    
    /**
     * Очистить весь кэш
     */
    public static void clear() {
        cache.clear();
    }
}
