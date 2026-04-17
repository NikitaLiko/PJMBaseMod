package ru.liko.pjmbasemod.common.map.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import ru.liko.pjmbasemod.Pjmbasemod;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Управляет загрузкой конфигураций карт.
 */
public class MapConfigManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, MapConfig> loadedConfigs = new HashMap<>();
    private static final String CONFIG_DIR = "maps";

    public static void loadAll() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(Pjmbasemod.MODID).resolve(CONFIG_DIR);
        File dir = configPath.toFile();

        if (!dir.exists()) {
            dir.mkdirs();
            createDefaultConfig(configPath);
        }

        loadedConfigs.clear();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null)
            return;

        for (File file : files) {
            // Пропускаем файлы начинающиеся с _ (примеры)
            if (file.getName().startsWith("_")) continue;

            try (Reader reader = new FileReader(file)) {
                MapConfig config = GSON.fromJson(reader, MapConfig.class);
                if (config == null) {
                    LOGGER.warn("Map config file is empty or invalid JSON: {}", file.getName());
                    continue;
                }
                if (config.getMapId() == null || config.getMapId().isEmpty()) {
                    LOGGER.warn("Map config missing 'mapId' field: {}", file.getName());
                    continue;
                }
                // Валидация обязательных полей
                if (config.getDisplayName() == null || config.getDisplayName().isEmpty()) {
                    LOGGER.warn("Map '{}' has no displayName, using mapId as fallback", config.getMapId());
                }
                if (config.getRoundTimeSeconds() <= 0) {
                    LOGGER.warn("Map '{}' has invalid roundTimeSeconds ({}), using default 1200",
                            config.getMapId(), config.getRoundTimeSeconds());
                }
                if (config.getTeams() == null || config.getTeams().isEmpty()) {
                    LOGGER.warn("Map '{}' has no team spawn configurations!", config.getMapId());
                }
                loadedConfigs.put(config.getMapId(), config);
                LOGGER.info("Loaded map config: {} ({})", config.getMapId(),
                        config.getDisplayName() != null ? config.getDisplayName() : "no name");
            } catch (com.google.gson.JsonSyntaxException e) {
                LOGGER.error("Invalid JSON syntax in map config '{}': {}", file.getName(), e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Failed to load map config: " + file.getName(), e);
            }
        }
    }

    private static void createDefaultConfig(Path dir) {
        String defaultJson = """
            {
                "mapId": "example_map",
                "displayName": "Example Map",
                "texture": "pjmbasemod:textures/gui/maps/example.png",
                "dimension": "pjmbasemod:battleground",
                "dynamicDimension": true,
                "genType": "VOID",
                "roundTimeSeconds": 1200,
                "teams": [
                    {
                        "teamId": "us",
                        "spawnPos": [100.5, 65.0, 200.5],
                        "spawnYaw": 0.0
                    },
                    {
                        "teamId": "ru",
                        "spawnPos": [-100.5, 65.0, 200.5],
                        "spawnYaw": 180.0
                    }
                ],
                "capturePoints": [
                    {
                        "id": "alpha",
                        "name": "Alpha",
                        "pos": [0.0, 65.0, 200.0],
                        "radius": 10.0,
                        "captureTime": 30
                    },
                    {
                        "id": "bravo",
                        "name": "Bravo",
                        "pos": [0.0, 65.0, 100.0],
                        "radius": 10.0,
                        "captureTime": 30
                    }
                ],
                "weather": {
                    "type": "clear",
                    "timeOfDay": 6000
                }
            }
            """;

        Path exampleFile = dir.resolve("_example_map.json");
        try {
            Files.writeString(exampleFile, defaultJson);
            LOGGER.info("Created example map config: {}", exampleFile);
        } catch (Exception e) {
            LOGGER.error("Failed to create default map config: {}", e.getMessage());
        }
    }

    public static MapConfig getConfig(String mapId) {
        return loadedConfigs.get(mapId);
    }

    public static Map<String, MapConfig> getAllConfigs() {
        return new HashMap<>(loadedConfigs);
    }

    public static java.util.Set<String> getAvailableMapIds() {
        return loadedConfigs.keySet();
    }

    public static void reloadConfigs() {
        loadAll();
    }
}
