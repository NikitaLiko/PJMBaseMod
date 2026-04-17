package ru.liko.pjmbasemod.common.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.map.config.MapConfigManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Управляет ротацией карт.
 * Если голосов нет — выбирает следующую карту из списка ротации.
 * Не повторяет текущую карту подряд (если возможно).
 * Конфиг: config/pjmbasemod/map_rotation.json
 */
public class MapRotationManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROTATION_FILE = FMLPaths.CONFIGDIR.get()
            .resolve(Pjmbasemod.MODID).resolve("map_rotation.json");

    private static List<String> rotation = new ArrayList<>();
    private static int currentIndex = 0;

    /**
     * Загружает ротацию с диска. Если файла нет — создаёт дефолтный из доступных карт.
     */
    public static void load() {
        if (!Files.exists(ROTATION_FILE)) {
            // Создаём ротацию из всех загруженных карт
            rotation = new ArrayList<>(MapConfigManager.getAvailableMapIds());
            if (rotation.isEmpty()) {
                rotation.add("default");
            }
            save();
            LOGGER.info("Created default map rotation with {} map(s)", rotation.size());
            return;
        }

        try {
            String json = Files.readString(ROTATION_FILE);
            List<String> loaded = GSON.fromJson(json, new TypeToken<List<String>>(){}.getType());
            if (loaded != null && !loaded.isEmpty()) {
                rotation = loaded;
                LOGGER.info("Loaded map rotation: {} map(s)", rotation.size());
            } else {
                rotation = new ArrayList<>(MapConfigManager.getAvailableMapIds());
                if (rotation.isEmpty()) rotation.add("default");
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load map rotation", e);
            rotation = new ArrayList<>(MapConfigManager.getAvailableMapIds());
            if (rotation.isEmpty()) rotation.add("default");
        }

        currentIndex = 0;
    }

    /**
     * Сохраняет ротацию на диск.
     */
    public static void save() {
        try {
            Path parent = ROTATION_FILE.getParent();
            if (!Files.exists(parent)) Files.createDirectories(parent);
            Files.writeString(ROTATION_FILE, GSON.toJson(rotation));
        } catch (IOException e) {
            LOGGER.error("Failed to save map rotation", e);
        }
    }

    /**
     * Возвращает следующую карту из ротации, избегая повторения текущей.
     * @param currentMapId текущая карта (чтобы не повторять)
     * @return mapId следующей карты
     */
    public static String getNextMap(String currentMapId) {
        if (rotation.isEmpty()) {
            return currentMapId != null ? currentMapId : "default";
        }

        // Если в ротации всего одна карта — возвращаем её
        if (rotation.size() == 1) {
            return rotation.get(0);
        }

        // Двигаем индекс вперёд
        currentIndex = (currentIndex + 1) % rotation.size();
        String nextMap = rotation.get(currentIndex);

        // Если это та же карта — ещё раз вперёд
        if (nextMap.equals(currentMapId)) {
            currentIndex = (currentIndex + 1) % rotation.size();
            nextMap = rotation.get(currentIndex);
        }

        return nextMap;
    }

    /**
     * Возвращает текущий список ротации (только для чтения).
     */
    public static List<String> getRotation() {
        return List.copyOf(rotation);
    }

    /**
     * Устанавливает новую ротацию и сохраняет.
     */
    public static void setRotation(List<String> newRotation) {
        rotation = new ArrayList<>(newRotation);
        currentIndex = 0;
        save();
    }

    /**
     * Добавляет карту в ротацию.
     */
    public static void addMap(String mapId) {
        if (!rotation.contains(mapId)) {
            rotation.add(mapId);
            save();
        }
    }

    /**
     * Удаляет карту из ротации.
     */
    public static boolean removeMap(String mapId) {
        boolean removed = rotation.remove(mapId);
        if (removed) {
            if (currentIndex >= rotation.size()) currentIndex = 0;
            save();
        }
        return removed;
    }
}
