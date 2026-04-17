package ru.liko.pjmbasemod.common.map.config;

import java.util.List;

/**
 * Конфигурация карты, загружаемая из JSON.
 */
public class MapConfig {
    private String mapId;
    private String displayName;
    private String texture;
    private String dimension; // e.g. "minecraft:overworld", "minecraft:the_nether", "pjmbasemod:battleground"
    private boolean dynamicDimension = false; // true = автосоздать дименшон через DynamicDimensionManager
    private String genType = "VOID"; // VOID, FLAT, NORMAL — тип генерации для динамического дименшона
    private int roundTimeSeconds;
    private List<TeamConfig> teams;
    private List<CapturePointConfig> capturePoints;
    private WeatherConfig weather;

    public String getMapId() {
        return mapId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTexture() {
        return texture;
    }

    /**
     * Возвращает ID измерения для карты.
     * Формат: "namespace:path", например "minecraft:overworld".
     * Если не указано — используется Overworld.
     */
    public String getDimension() {
        return dimension;
    }

    public int getRoundTimeSeconds() {
        return roundTimeSeconds;
    }

    public List<TeamConfig> getTeams() {
        return teams;
    }

    public List<CapturePointConfig> getCapturePoints() {
        return capturePoints;
    }

    public WeatherConfig getWeather() {
        return weather;
    }

    /**
     * Если true — дименшон карты будет автосоздан через DynamicDimensionManager при старте матча.
     */
    public boolean isDynamicDimension() {
        return dynamicDimension;
    }

    /**
     * Тип генерации для динамического дименшона (VOID, FLAT, NORMAL).
     */
    public String getGenType() {
        return genType != null ? genType : "VOID";
    }

    /**
     * Возвращает короткое имя дименшона (без namespace) для DynamicDimensionManager.
     * Например: "pjmbasemod:battleground" → "battleground".
     * Для ванильных дименшонов возвращает null.
     */
    public String getDynamicDimensionName() {
        if (dimension == null || dimension.isEmpty()) return null;
        String[] parts = dimension.split(":");
        if (parts.length == 2 && parts[0].equals(ru.liko.pjmbasemod.Pjmbasemod.MODID)) {
            return parts[1];
        }
        return null;
    }

    // --- Inner Classes ---

    public static class TeamConfig {
        private String teamId; // "us", "ru"
        private double[] spawnPos; // [x, y, z]
        private float spawnYaw;

        public String getTeamId() {
            return teamId;
        }

        public double[] getSpawnPos() {
            return spawnPos;
        }

        public float getSpawnYaw() {
            return spawnYaw;
        }
    }

    public static class CapturePointConfig {
        private String id;
        private String name;
        private double[] pos; // [x, y, z]
        private double radius;
        private int captureTime;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double[] getPos() {
            return pos;
        }

        public double getRadius() {
            return radius;
        }

        public int getCaptureTime() {
            return captureTime;
        }
    }

    public static class WeatherConfig {
        private String type; // "clear", "rain", "storm"
        private long timeOfDay; // 0-24000

        public String getType() {
            return type;
        }

        public long getTimeOfDay() {
            return timeOfDay;
        }
    }
}
