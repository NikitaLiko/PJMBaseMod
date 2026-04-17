package ru.liko.pjmbasemod.common.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфигурация Stats API.
 * Хранится в JSON файле: config/pjmbasemod/stats_api.json
 * Управляет подключением к внешнему бекенду для отправки игровой статистики.
 */
public class StatsApiConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILE_NAME = "stats_api.json";
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("pjmbasemod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigData config = new ConfigData();
    private static boolean initialized = false;

    /**
     * Структура конфигурации Stats API
     */
    public static class ConfigData {
        /** Включена ли отправка статистики */
        public boolean enabled = false;

        /** Базовый URL API сервера (без trailing slash) */
        public String apiUrl = "http://localhost:3000/api";

        /** API ключ для авторизации (Bearer token) */
        public String apiKey = "";

        /** Отправлять полную статистику при выходе игрока */
        public boolean sendOnLogout = true;

        /** Отправлять события убийств в реальном времени */
        public boolean sendOnKill = true;

        /** Отправлять итоги матча */
        public boolean sendOnMatchEnd = true;

        /** Таймаут HTTP запросов в секундах */
        public int timeoutSeconds = 5;

        /** Детальное логирование запросов/ответов */
        public boolean debugLogging = false;
    }

    public static Path getConfigFilePath() {
        return CONFIG_DIR.resolve(CONFIG_FILE_NAME);
    }

    /**
     * Инициализация конфигурации. Создаёт файл с дефолтными значениями если не существует.
     */
    public static void init() {
        if (initialized) return;

        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
        } catch (IOException e) {
            LOGGER.error("[StatsAPI] Failed to create config directory: {}", CONFIG_DIR, e);
        }

        Path configPath = getConfigFilePath();
        if (Files.exists(configPath)) {
            loadFromFile();
        } else {
            config = new ConfigData();
            saveToFile();
            LOGGER.info("[StatsAPI] Created default config at {}", configPath);
        }

        initialized = true;
        LOGGER.info("[StatsAPI] Config initialized. Enabled: {}, URL: {}", config.enabled, config.apiUrl);
    }

    public static void loadFromFile() {
        Path configPath = getConfigFilePath();
        try (Reader reader = Files.newBufferedReader(configPath)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                config = loaded;
                LOGGER.info("[StatsAPI] Config loaded from {}", configPath);
            }
        } catch (IOException e) {
            LOGGER.error("[StatsAPI] Failed to load config: {}", e.getMessage());
            config = new ConfigData();
        }
    }

    public static void saveToFile() {
        Path configPath = getConfigFilePath();
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            LOGGER.error("[StatsAPI] Failed to save config: {}", e.getMessage());
        }
    }

    public static boolean reload() {
        try {
            loadFromFile();
            return true;
        } catch (Exception e) {
            LOGGER.error("[StatsAPI] Failed to reload config: {}", e.getMessage());
            return false;
        }
    }

    // ============== GETTERS ==============

    public static boolean isEnabled() { return config.enabled; }
    public static String getApiUrl() { return config.apiUrl; }
    public static String getApiKey() { return config.apiKey; }
    public static boolean isSendOnLogout() { return config.sendOnLogout; }
    public static boolean isSendOnKill() { return config.sendOnKill; }
    public static boolean isSendOnMatchEnd() { return config.sendOnMatchEnd; }
    public static int getTimeoutSeconds() { return config.timeoutSeconds; }
    public static boolean isDebugLogging() { return config.debugLogging; }
}
