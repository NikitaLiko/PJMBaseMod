package ru.liko.pjmbasemod.client;

import ru.liko.pjmbasemod.common.KitDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Клиентский кэш данных о китах.
 * Получает данные от сервера через SyncKitsDataPacket.
 * Updated to support Ranked Kit Variations.
 */
public class ClientKitsCache {
    // Map: classId -> Map: teamName -> List of KitDefinition
    private static Map<String, Map<String, List<KitDefinition>>> kitsData = new HashMap<>();
    
    /**
     * Устанавливает данные о китах (вызывается при синхронизации с сервером)
     */
    public static void setKitsData(Map<String, Map<String, List<KitDefinition>>> data) {
        kitsData = data != null ? data : new HashMap<>();
    }
    
    /**
     * Получает список строк предметов для класса и команды (используется для дефолтного/первого кита)
     * Legacy support for simple item lookup.
     */
    public static List<String> getKitItemStrings(String classId, String teamName) {
        String normalizedTeam = normalizeTeamName(teamName);
        
        Map<String, List<KitDefinition>> classKits = kitsData.get(classId.toLowerCase());
        if (classKits == null) {
            return new ArrayList<>();
        }
        
        List<KitDefinition> kits = classKits.get(normalizedTeam);
        if (kits != null && !kits.isEmpty()) {
            // Return items from the first kit (usually default)
            return new ArrayList<>(kits.get(0).getItems());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Получает список всех китов для класса и команды
     */
    public static List<KitDefinition> getKits(String classId, String teamName) {
        String normalizedTeam = normalizeTeamName(teamName);
        Map<String, List<KitDefinition>> classKits = kitsData.get(classId.toLowerCase());
        
        if (classKits == null) {
            return new ArrayList<>();
        }
        
        List<KitDefinition> kits = classKits.get(normalizedTeam);
        return kits != null ? new ArrayList<>(kits) : new ArrayList<>();
    }
    
    /**
     * Нормализует название команды
     */
    private static String normalizeTeamName(String teamName) {
        if (teamName == null) {
            return "team1";
        }
        
        String lower = teamName.toLowerCase().trim();
        
        // Проверяем, является ли это Team2
        // Note: accessing ClientTeamConfig directly might be safer if available, 
        // but for now relying on string check/defaults.
        try {
            String team2Name = ClientTeamConfig.getTeam2Name().toLowerCase();
            if (lower.equals(team2Name) || lower.equals("team2") || lower.contains("2")) {
                return "team2";
            }
        } catch (Exception e) {
             if (lower.equals("team2")) return "team2";
        }
        
        return "team1";
    }
    
    /**
     * Очищает кэш
     */
    public static void clear() {
        kitsData.clear();
    }
    
    /**
     * Проверяет, есть ли данные в кэше
     */
    public static boolean hasData() {
        return !kitsData.isEmpty();
    }
}
