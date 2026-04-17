package ru.liko.pjmbasemod.common.dimension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Pjmbasemod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфигурация отдельного динамического измерения.
 * Каждое измерение хранит свой конфиг в config/pjmbasemod/dimensions/<name>.json
 */
public class DimensionConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIMENSIONS_DIR = FMLPaths.CONFIGDIR.get()
            .resolve(Pjmbasemod.MODID).resolve("dimensions");

    // --- Основные ---
    private String name;
    private String genType = "VOID";
    private String displayName = "";

    // --- Спавн ---
    private double spawnX = 0.5;
    private double spawnY = 100;
    private double spawnZ = 0.5;
    private float spawnYaw = 0;

    // --- Время и погода ---
    private int timeOfDay = -1;         // -1 = естественное, 0-24000 = фиксированное
    private String weather = "natural"; // natural, clear, rain, storm
    private boolean timeFrozen = false; // Заморозить время суток

    // --- Правила ---
    private boolean pvpEnabled = true;
    private boolean mobSpawning = true;
    private boolean allowBlockBreaking = true;
    private boolean allowBlockPlacing = true;
    private boolean allowExplosions = true;
    private boolean keepInventory = false;
    private boolean announceDeaths = true;

    // --- Анти-гриф ---
    private String antiGriefOverride = "global"; // global, enabled, disabled

    // --- Мировая граница ---
    private double worldBorderSize = 0; // 0 = наследовать от overworld
    private double worldBorderCenterX = 0;
    private double worldBorderCenterZ = 0;

    // --- Доступ ---
    private int requiredPermissionLevel = 0; // 0 = все, 2 = OP, 4 = console
    private String requiredTeam = "";        // Пустая строка = все команды

    public DimensionConfig() {}

    public DimensionConfig(String name, DynamicDimensionManager.GenType genType) {
        this.name = name;
        this.genType = genType.name();
        this.displayName = name;
    }

    // --- Сохранение/загрузка ---

    public static Path getConfigPath(String dimensionName) {
        return DIMENSIONS_DIR.resolve(dimensionName + ".json");
    }

    public void save() {
        try {
            if (!Files.exists(DIMENSIONS_DIR)) {
                Files.createDirectories(DIMENSIONS_DIR);
            }
            Path file = getConfigPath(name);
            Files.writeString(file, GSON.toJson(this));
            LOGGER.debug("Saved dimension config for '{}'", name);
        } catch (IOException e) {
            LOGGER.error("Failed to save dimension config for '{}'", name, e);
        }
    }

    public static DimensionConfig load(String dimensionName) {
        Path file = getConfigPath(dimensionName);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            String json = Files.readString(file);
            DimensionConfig config = GSON.fromJson(json, DimensionConfig.class);
            if (config != null) {
                config.name = dimensionName;
            }
            return config;
        } catch (Exception e) {
            LOGGER.error("Failed to load dimension config for '{}'", dimensionName, e);
            return null;
        }
    }

    public static boolean delete(String dimensionName) {
        try {
            Path file = getConfigPath(dimensionName);
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.error("Failed to delete dimension config for '{}'", dimensionName, e);
            return false;
        }
    }

    // --- Getters/Setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenType() { return genType; }
    public void setGenType(String genType) { this.genType = genType; }

    public String getDisplayName() { return displayName != null && !displayName.isEmpty() ? displayName : name; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public double getSpawnX() { return spawnX; }
    public void setSpawnX(double spawnX) { this.spawnX = spawnX; }

    public double getSpawnY() { return spawnY; }
    public void setSpawnY(double spawnY) { this.spawnY = spawnY; }

    public double getSpawnZ() { return spawnZ; }
    public void setSpawnZ(double spawnZ) { this.spawnZ = spawnZ; }

    public float getSpawnYaw() { return spawnYaw; }
    public void setSpawnYaw(float spawnYaw) { this.spawnYaw = spawnYaw; }

    public int getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(int timeOfDay) { this.timeOfDay = timeOfDay; }

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }

    public boolean isTimeFrozen() { return timeFrozen; }
    public void setTimeFrozen(boolean timeFrozen) { this.timeFrozen = timeFrozen; }

    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }

    public boolean isMobSpawning() { return mobSpawning; }
    public void setMobSpawning(boolean mobSpawning) { this.mobSpawning = mobSpawning; }

    public boolean isAllowBlockBreaking() { return allowBlockBreaking; }
    public void setAllowBlockBreaking(boolean allowBlockBreaking) { this.allowBlockBreaking = allowBlockBreaking; }

    public boolean isAllowBlockPlacing() { return allowBlockPlacing; }
    public void setAllowBlockPlacing(boolean allowBlockPlacing) { this.allowBlockPlacing = allowBlockPlacing; }

    public boolean isAllowExplosions() { return allowExplosions; }
    public void setAllowExplosions(boolean allowExplosions) { this.allowExplosions = allowExplosions; }

    public boolean isKeepInventory() { return keepInventory; }
    public void setKeepInventory(boolean keepInventory) { this.keepInventory = keepInventory; }

    public boolean isAnnounceDeaths() { return announceDeaths; }
    public void setAnnounceDeaths(boolean announceDeaths) { this.announceDeaths = announceDeaths; }

    public String getAntiGriefOverride() { return antiGriefOverride; }
    public void setAntiGriefOverride(String antiGriefOverride) { this.antiGriefOverride = antiGriefOverride; }

    public double getWorldBorderSize() { return worldBorderSize; }
    public void setWorldBorderSize(double worldBorderSize) { this.worldBorderSize = worldBorderSize; }

    public double getWorldBorderCenterX() { return worldBorderCenterX; }
    public void setWorldBorderCenterX(double worldBorderCenterX) { this.worldBorderCenterX = worldBorderCenterX; }

    public double getWorldBorderCenterZ() { return worldBorderCenterZ; }
    public void setWorldBorderCenterZ(double worldBorderCenterZ) { this.worldBorderCenterZ = worldBorderCenterZ; }

    public int getRequiredPermissionLevel() { return requiredPermissionLevel; }
    public void setRequiredPermissionLevel(int requiredPermissionLevel) { this.requiredPermissionLevel = requiredPermissionLevel; }

    public String getRequiredTeam() { return requiredTeam; }
    public void setRequiredTeam(String requiredTeam) { this.requiredTeam = requiredTeam; }

    /**
     * Возвращает true если анти-гриф включён для этого дименшона.
     * global = использовать глобальную настройку, enabled = всегда вкл, disabled = всегда выкл.
     */
    public boolean isAntiGriefEffective(boolean globalEnabled) {
        return switch (antiGriefOverride != null ? antiGriefOverride.toLowerCase() : "global") {
            case "enabled" -> true;
            case "disabled" -> false;
            default -> globalEnabled;
        };
    }
}
