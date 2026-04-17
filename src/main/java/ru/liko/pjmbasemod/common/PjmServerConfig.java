package ru.liko.pjmbasemod.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Серверная конфигурация PJM мода.
 * Хранится в JSON файле: config/pjmbasemod/pjm_config.json
 */
public class PjmServerConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILE_NAME = "pjm_config.json";
    private static final String OLD_CONFIG_FILE_NAME = "wrb_config.json";
    private static final Path NEW_CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("pjmbasemod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigData config = new ConfigData();
    private static boolean initialized = false;

    /**
     * Структура конфигурации
     */
    public static class ConfigData {
        // Teams
        public TeamsConfig teams = new TeamsConfig();

        // Class Limits
        public ClassLimitsConfig classLimits = new ClassLimitsConfig();

        // Kit Cooldown
        public int kitCooldownSeconds = 60;

        // Capture System
        public CaptureConfig capture = new CaptureConfig();

        // Anti-Grief
        public AntiGriefConfig antiGrief = new AntiGriefConfig();

        // MilSim
        public MilSimConfig milsim = new MilSimConfig();

        // Squad HUD
        public SquadHudConfig squadHud = new SquadHudConfig();

        // Chat
        public ChatConfig chat = new ChatConfig();

        // Debug
        public boolean enableDebugLogging = false;

        // Logout Spawn (телепортация при выходе)
        public LogoutSpawnConfig logoutSpawnTeam1 = new LogoutSpawnConfig();
        public LogoutSpawnConfig logoutSpawnTeam2 = new LogoutSpawnConfig();

        // Vehicle Crew
        public VehicleCrewConfig vehicleCrew = new VehicleCrewConfig();

        // Server Join
        public ServerJoinConfig serverJoin = new ServerJoinConfig();
    }

    public static class TeamsConfig {
        public String team1Name = "vsrf";
        public String team2Name = "nato";
        public int balanceThreshold = 1;
        public List<String> team1JoinCommands = new ArrayList<>();
        public List<String> team2JoinCommands = new ArrayList<>();
    }

    public static class ClassLimitsConfig {
        public int assault = 0;
        public int machineGunner = 0;
        public int medic = 0;
        public int antiTank = 0;
        public int engineer = 0;
        public int crew = 6;
        public int sniper = 2;
        public int sso = 1;
        public int uavOperator = 2;
        public int scout = 2;
        public int ewSpecialist = 2;
        public int spn = 2;
    }

    public static class CaptureConfig {
        public boolean enabled = true;
        public double defaultPointRadius = 10.0;
        public int captureTimeSeconds = 60;
        public int spawnCooldownSeconds = 60;
    }

    public static class AntiGriefConfig {
        public boolean enabled = true;
        public boolean preventItemDrop = false;
        public boolean preventItemPickup = false;
        public boolean preventBlockInteraction = true;

        // Ограничение глубины копания (0 = отключено)
        public int maxDigDepth = 3;

        // Логирование разрушения блоков (как CoreProtect)
        public boolean enableBlockLogging = true;

        public List<String> breakableBlocks = Arrays.asList(
                "minecraft:grass_block",
                "minecraft:dirt",
                "minecraft:stone",
                "minecraft:cobblestone");
        public List<String> placeableBlocks = Arrays.asList(
                "minecraft:grass_block",
                "minecraft:dirt",
                "minecraft:stone",
                "minecraft:cobblestone");

        // Блоки, которые можно ломать только определённым инструментом
        // Формат: "block_id" -> "tool_id" (например "minecraft:stone" ->
        // "minecraft:iron_pickaxe")
        // Можно указать тип инструмента: "pickaxe", "axe", "shovel", "hoe"
        public java.util.Map<String, String> toolRequiredBlocks = new java.util.HashMap<>();
        public List<String> interactableBlocks = Arrays.asList(
                "minecraft:crafting_table",
                "minecraft:furnace",
                "minecraft:blast_furnace",
                "minecraft:smoker",
                "minecraft:anvil",
                "minecraft:chipped_anvil",
                "minecraft:damaged_anvil",
                "minecraft:enchanting_table",
                "minecraft:grindstone",
                "minecraft:smithing_table",
                "minecraft:stonecutter",
                "minecraft:loom",
                "minecraft:cartography_table",
                "minecraft:fletching_table",
                "minecraft:lever",
                "minecraft:stone_button",
                "minecraft:oak_button",
                "minecraft:spruce_button",
                "minecraft:birch_button",
                "minecraft:jungle_button",
                "minecraft:acacia_button",
                "minecraft:dark_oak_button",
                "minecraft:mangrove_button",
                "minecraft:cherry_button",
                "minecraft:bamboo_button",
                "minecraft:crimson_button",
                "minecraft:warped_button",
                "minecraft:polished_blackstone_button");
    }

    public static class MilSimConfig {
        public boolean disableHunger = true;
        public boolean disableArmor = true;
        public boolean blackDeathScreen = true;
        public boolean muteSoundsOnDeath = true;
        public boolean enableCameraHeadBob = true;
    }

    public static class SquadHudConfig {
        public boolean enableSquadPlayerList = true;
        public boolean enableWeaponInfo = true;
        public boolean enableItemSwitchPanel = true;
        public int itemSwitchDisplayTime = 2500;
    }

    public static class ChatConfig {
        public boolean enabled = true;
        public double localChatRadius = 50.0;
        public String defaultChatMode = "LOCAL";
        public boolean showChatModeInHud = true;
    }

    public static class LogoutSpawnConfig {
        public boolean enabled = true;
        public double x = 0.0;
        public double y = 64.0;
        public double z = 0.0;
        public String dimension = "minecraft:overworld";
    }

    public static class VehicleCrewConfig {
        public List<String> restrictedVehicles = new ArrayList<>();
        public List<Integer> restrictedSeats = Arrays.asList(0, 1);
    }

    public static class ServerJoinConfig {
        public String joinDimension = "lobby";
    }

    /**
     * Получает конфиг спавна для указанной команды
     */
    public static LogoutSpawnConfig getLogoutSpawnForTeam(String teamName) {
        String team1 = config.teams.team1Name.toLowerCase();
        if (teamName != null && teamName.toLowerCase().equals(team1)) {
            return config.logoutSpawnTeam1;
        }
        return config.logoutSpawnTeam2;
    }

    /**
     * Получает путь к файлу конфига (config/pjmbasemod/pjm_config.json)
     */
    public static Path getConfigFilePath() {
        return NEW_CONFIG_DIR.resolve(CONFIG_FILE_NAME);
    }

    /**
     * Инициализирует конфигурацию.
     * Автоматически мигрирует из старого расположения (корень сервера) в новое
     * (config/pjmbasemod/).
     */
    public static void init() {
        if (initialized) {
            return;
        }

        try {
            if (!Files.exists(NEW_CONFIG_DIR)) {
                Files.createDirectories(NEW_CONFIG_DIR);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory: {}", NEW_CONFIG_DIR, e);
        }

        Path newPath = getConfigFilePath();
        Path oldWrbInDir = NEW_CONFIG_DIR.resolve(OLD_CONFIG_FILE_NAME);
        Path oldWrbInRoot = FMLPaths.GAMEDIR.get().resolve(OLD_CONFIG_FILE_NAME);

        // Миграция: wrb_config.json → pjm_config.json
        if (!Files.exists(newPath)) {
            // Сначала проверяем в config/pjmbasemod/wrb_config.json
            Path sourceToMigrate = null;
            if (Files.exists(oldWrbInDir)) {
                sourceToMigrate = oldWrbInDir;
            } else if (Files.exists(oldWrbInRoot)) {
                sourceToMigrate = oldWrbInRoot;
            }
            if (sourceToMigrate != null) {
                try {
                    Files.move(sourceToMigrate, newPath);
                    LOGGER.info("Migrated config from {} to {}", sourceToMigrate, newPath);
                } catch (IOException e) {
                    LOGGER.warn("Failed to migrate config, copying instead", e);
                    try {
                        Files.copy(sourceToMigrate, newPath);
                    } catch (IOException ex) {
                        LOGGER.error("Failed to copy config", ex);
                    }
                }
            }
        }

        if (Files.exists(newPath)) {
            loadFromFile();
        } else {
            config = new ConfigData();
            saveToFile();
        }

        initialized = true;
        LOGGER.info("PjmServerConfig инициализирован. Файл: {}", newPath);
    }

    /**
     * Загружает конфиг из файла
     */
    public static void loadFromFile() {
        Path configPath = getConfigFilePath();

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Type type = new TypeToken<ConfigData>() {
            }.getType();
            ConfigData loaded = GSON.fromJson(reader, type);

            if (loaded != null) {
                config = loaded;
                LOGGER.info("Конфиг загружен из {}", configPath);
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка загрузки конфига: {}", e.getMessage());
            config = new ConfigData();
        }
    }

    /**
     * Сохраняет конфиг в файл
     */
    public static void saveToFile() {
        Path configPath = getConfigFilePath();

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(config, writer);
            LOGGER.info("Конфиг сохранен в {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Ошибка сохранения конфига: {}", e.getMessage());
        }
    }

    /**
     * Перезагружает конфиг из файла
     */
    public static boolean reload() {
        try {
            loadFromFile();
            return true;
        } catch (Exception e) {
            LOGGER.error("Ошибка перезагрузки конфига: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Универсальный setter через Consumer для изменения полей конфига из команд.
     */
    public static void setConfigValue(java.util.function.Consumer<ConfigData> modifier) {
        modifier.accept(config);
    }

    // ============== GETTERS ==============

    // Teams
    public static String getTeam1Name() {
        return config.teams.team1Name;
    }

    public static String getTeam2Name() {
        return config.teams.team2Name;
    }

    public static int getTeamBalanceThreshold() {
        return config.teams.balanceThreshold;
    }

    public static List<String> getTeam1JoinCommands() {
        return config.teams.team1JoinCommands;
    }

    public static List<String> getTeam2JoinCommands() {
        return config.teams.team2JoinCommands;
    }

    // Class Limits
    public static int getClassLimit(String classId) {
        return switch (classId.toLowerCase()) {
            case "assault" -> config.classLimits.assault;
            case "machine_gunner" -> config.classLimits.machineGunner;
            case "medic" -> config.classLimits.medic;
            case "anti_tank" -> config.classLimits.antiTank;
            case "engineer" -> config.classLimits.engineer;
            case "crew" -> config.classLimits.crew;
            case "sniper" -> config.classLimits.sniper;
            case "sso" -> config.classLimits.sso;
            case "uav_operator" -> config.classLimits.uavOperator;
            case "scout" -> config.classLimits.scout;
            case "ew_specialist" -> config.classLimits.ewSpecialist;
            case "spn" -> config.classLimits.spn;
            default -> 0;
        };
    }

    // Kit Cooldown
    public static int getKitCooldownSeconds() {
        return config.kitCooldownSeconds;
    }

    // Capture
    public static boolean isCaptureSystemEnabled() {
        return config.capture.enabled;
    }

    public static double getCapturePointDefaultRadius() {
        return config.capture.defaultPointRadius;
    }

    public static int getCaptureTimeSeconds() {
        return config.capture.captureTimeSeconds;
    }

    public static int getSpawnCooldownSeconds() {
        return config.capture.spawnCooldownSeconds;
    }

    // Anti-Grief
    public static boolean isAntiGriefEnabled() {
        return config.antiGrief.enabled;
    }

    public static boolean isPreventItemDrop() {
        return config.antiGrief.preventItemDrop;
    }

    public static boolean isPreventItemPickup() {
        return config.antiGrief.preventItemPickup;
    }

    public static boolean isPreventBlockInteraction() {
        return config.antiGrief.preventBlockInteraction;
    }

    public static int getMaxDigDepth() {
        return config.antiGrief.maxDigDepth;
    }

    public static boolean isBlockLoggingEnabled() {
        return config.antiGrief.enableBlockLogging;
    }

    public static List<String> getBreakableBlocks() {
        return config.antiGrief.breakableBlocks;
    }

    public static List<String> getPlaceableBlocks() {
        return config.antiGrief.placeableBlocks;
    }

    public static List<String> getInteractableBlocks() {
        return config.antiGrief.interactableBlocks;
    }

    public static java.util.Map<String, String> getToolRequiredBlocks() {
        return config.antiGrief.toolRequiredBlocks;
    }

    // MilSim
    public static boolean isDisableHunger() {
        return config.milsim.disableHunger;
    }

    public static boolean isDisableArmor() {
        return config.milsim.disableArmor;
    }

    public static boolean isBlackDeathScreen() {
        return config.milsim.blackDeathScreen;
    }

    public static boolean isMuteSoundsOnDeath() {
        return config.milsim.muteSoundsOnDeath;
    }

    public static boolean isEnableCameraHeadBob() {
        return config.milsim.enableCameraHeadBob;
    }

    // Squad HUD
    public static boolean isEnableSquadPlayerList() {
        return config.squadHud.enableSquadPlayerList;
    }

    public static boolean isEnableWeaponInfo() {
        return config.squadHud.enableWeaponInfo;
    }

    public static boolean isEnableItemSwitchPanel() {
        return config.squadHud.enableItemSwitchPanel;
    }

    public static int getItemSwitchDisplayTime() {
        return config.squadHud.itemSwitchDisplayTime;
    }

    // Chat
    public static boolean isChatSystemEnabled() {
        return config.chat.enabled;
    }

    public static double getLocalChatRadius() {
        return config.chat.localChatRadius;
    }

    public static String getDefaultChatMode() {
        return config.chat.defaultChatMode;
    }

    public static boolean isShowChatModeInHud() {
        return config.chat.showChatModeInHud;
    }

    // Debug
    public static boolean isDebugLoggingEnabled() {
        return config.enableDebugLogging;
    }

    // Logout Spawn Team 1
    public static boolean isLogoutSpawnTeam1Enabled() {
        return config.logoutSpawnTeam1.enabled;
    }

    public static double getLogoutSpawnTeam1X() {
        return config.logoutSpawnTeam1.x;
    }

    public static double getLogoutSpawnTeam1Y() {
        return config.logoutSpawnTeam1.y;
    }

    public static double getLogoutSpawnTeam1Z() {
        return config.logoutSpawnTeam1.z;
    }

    public static String getLogoutSpawnTeam1Dimension() {
        return config.logoutSpawnTeam1.dimension;
    }

    // Logout Spawn Team 2
    public static boolean isLogoutSpawnTeam2Enabled() {
        return config.logoutSpawnTeam2.enabled;
    }

    public static double getLogoutSpawnTeam2X() {
        return config.logoutSpawnTeam2.x;
    }

    public static double getLogoutSpawnTeam2Y() {
        return config.logoutSpawnTeam2.y;
    }

    public static double getLogoutSpawnTeam2Z() {
        return config.logoutSpawnTeam2.z;
    }

    public static String getLogoutSpawnTeam2Dimension() {
        return config.logoutSpawnTeam2.dimension;
    }

    // Vehicle Crew
    public static List<String> getCrewRestrictedVehicles() {
        return config.vehicleCrew.restrictedVehicles;
    }

    public static List<Integer> getCrewRestrictedSeats() {
        return config.vehicleCrew.restrictedSeats;
    }

    // Server Join
    public static String getServerJoinDimension() {
        return config.serverJoin.joinDimension;
    }

    // ============== SETTERS ==============

    public static void setTeam1Name(String name) {
        config.teams.team1Name = name;
        saveToFile();
    }

    public static void setTeam2Name(String name) {
        config.teams.team2Name = name;
        saveToFile();
    }
}
