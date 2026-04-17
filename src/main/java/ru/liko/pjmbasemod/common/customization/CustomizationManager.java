package ru.liko.pjmbasemod.common.customization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NeoForge 1.21.1: Migrated from SavedData to JSON storage in config folder.
 */
public class CustomizationManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_FILE = Paths.get("config", "pjmbasemod", "customization.json");
    private static CustomizationManager CLIENT_INSTANCE;
    private static CustomizationManager SERVER_INSTANCE;

    private final Map<String, CustomizationOption> options = new HashMap<>();

    public CustomizationManager() {
    }

    /**
     * Загружает данные из JSON файла в config/pjmbasemod/customization.json
     */
    public static CustomizationManager load() {
        if (SERVER_INSTANCE != null) {
            return SERVER_INSTANCE;
        }

        CustomizationManager manager = new CustomizationManager();

        if (!Files.exists(CONFIG_FILE)) {
            LOGGER.info("Customization config file not found at {}, starting empty", CONFIG_FILE);
            SERVER_INSTANCE = manager;
            return manager;
        }

        try {
            String content = Files.readString(CONFIG_FILE);
            JsonObject json = new Gson().fromJson(content, JsonObject.class);
            JsonArray optionsArray = json.getAsJsonArray("options");

            if (optionsArray != null) {
                for (int i = 0; i < optionsArray.size(); i++) {
                    JsonObject optionJson = optionsArray.get(i).getAsJsonObject();
                    try {
                        CustomizationOption option = CustomizationOption.fromJson(optionJson);
                        manager.options.put(option.getId(), option);
                        LOGGER.debug("Loaded customization option: {}", option.getId());
                    } catch (Exception e) {
                        LOGGER.error("Failed to load customization option from JSON", e);
                    }
                }
            }

            LOGGER.info("Loaded {} customization options from {}", manager.options.size(), CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.error("Failed to load customization config from {}", CONFIG_FILE, e);
        }

        SERVER_INSTANCE = manager;
        return manager;
    }

    /**
     * Сохраняет текущие данные в JSON файл
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            JsonObject json = new JsonObject();
            JsonArray optionsArray = new JsonArray();

            for (CustomizationOption option : options.values()) {
                optionsArray.add(option.toJson());
            }

            json.add("options", optionsArray);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(CONFIG_FILE, gson.toJson(json));

            LOGGER.info("Saved {} customization options to {}", options.size(), CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.error("Failed to save customization config to {}", CONFIG_FILE, e);
        }
    }

    /**
     * Сбрасывает синглтон серверного экземпляра (например при перезагрузке)
     */
    public static void resetServerInstance() {
        SERVER_INSTANCE = null;
    }

    public static CustomizationManager getClientInstance() {
        if (CLIENT_INSTANCE == null) {
            CLIENT_INSTANCE = new CustomizationManager();
        }
        return CLIENT_INSTANCE;
    }

    public static void resetClientInstance() {
        CLIENT_INSTANCE = null;
    }

    public void addOption(CustomizationOption option) {
        options.put(option.getId(), option);
        save();
    }

    public void removeOption(String id) {
        options.remove(id);
        save();
    }

    public CustomizationOption getOption(String id) {
        return options.get(id);
    }

    public List<CustomizationOption> getOptionsByType(CustomizationType type) {
        List<CustomizationOption> result = new ArrayList<>();
        for (CustomizationOption option : options.values()) {
            if (option.getType() == type) {
                result.add(option);
            }
        }
        return result;
    }

    public List<CustomizationOption> getAllOptions() {
        return new ArrayList<>(options.values());
    }

    public void clear() {
        options.clear();
        save();
    }
}
