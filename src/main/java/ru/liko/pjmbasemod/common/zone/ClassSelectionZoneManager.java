package ru.liko.pjmbasemod.common.zone;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер для управления зонами выбора классов.
 * Сохраняет и загружает зоны из файла, предоставляет API для работы с зонами.
 */
public class ClassSelectionZoneManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ZONES_FILE_NAME = "class_selection_zones.dat";
    
    private final Map<String, ClassSelectionZone> zones = new HashMap<>();
    private final MinecraftServer server;

    public ClassSelectionZoneManager(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Получает зону по идентификатору
     */
    public ClassSelectionZone getZone(String zoneId) {
        return zones.computeIfAbsent(zoneId, k -> new ClassSelectionZone());
    }

    /**
     * Получает главную зону выбора классов (по умолчанию используется одна глобальная зона)
     */
    public ClassSelectionZone getMainZone() {
        return getZone("main");
    }

    /**
     * Сохраняет зону с указанным идентификатором
     */
    public void setZone(String zoneId, ClassSelectionZone zone) {
        zones.put(zoneId, zone);
        save();
    }

    /**
     * Удаляет зону с указанным идентификатором
     */
    public void removeZone(String zoneId) {
        zones.remove(zoneId);
        save();
    }

    /**
     * Очищает все зоны
     */
    public void clearAllZones() {
        zones.clear();
        save();
    }

    /**
     * Проверяет, находится ли игрок в какой-либо зоне выбора классов
     */
    public boolean isInAnyZone(Vec3 position, ResourceKey<Level> dimension) {
        for (Map.Entry<String, ClassSelectionZone> entry : zones.entrySet()) {
            ClassSelectionZone zone = entry.getValue();
            if (zone.contains(position, dimension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет, находится ли игрок в главной зоне выбора классов
     */
    public boolean isInMainZone(Vec3 position, ResourceKey<Level> dimension) {
        return getMainZone().contains(position, dimension);
    }

    /**
     * Находит зону, содержащую указанную позицию
     */
    public ClassSelectionZone findZone(Vec3 position, ResourceKey<Level> dimension) {
        for (ClassSelectionZone zone : zones.values()) {
            if (zone.contains(position, dimension)) {
                return zone;
            }
        }
        return null;
    }

    /**
     * Получает все зоны
     */
    public Map<String, ClassSelectionZone> getAllZones() {
        return new HashMap<>(zones);
    }

    /**
     * Сохраняет все зоны в файл
     */
    public void save() {
        try {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File zonesFile = new File(worldDir, ZONES_FILE_NAME);

            CompoundTag rootTag = new CompoundTag();
            CompoundTag zonesTag = new CompoundTag();

            for (Map.Entry<String, ClassSelectionZone> entry : zones.entrySet()) {
                zonesTag.put(entry.getKey(), entry.getValue().save());
            }

            rootTag.put("zones", zonesTag);
            rootTag.putInt("version", 1);

            NbtIo.writeCompressed(rootTag, zonesFile.toPath());
            if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                LOGGER.info("Saved {} class selection zone(s)", zones.size());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save class selection zones", e);
        }
    }

    /**
     * Загружает все зоны из файла
     */
    public void load() {
        try {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File zonesFile = new File(worldDir, ZONES_FILE_NAME);

            if (!zonesFile.exists()) {
                if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                    LOGGER.info("No class selection zones file found, starting fresh");
                }
                return;
            }

            CompoundTag rootTag = NbtIo.readCompressed(zonesFile.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            
            if (!rootTag.contains("zones")) {
                LOGGER.warn("Invalid zones file format");
                return;
            }

            CompoundTag zonesTag = rootTag.getCompound("zones");
            zones.clear();

            for (String key : zonesTag.getAllKeys()) {
                ClassSelectionZone zone = ClassSelectionZone.load(zonesTag.getCompound(key));
                zones.put(key, zone);
            }

            if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                LOGGER.info("Loaded {} class selection zone(s)", zones.size());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load class selection zones", e);
        }
    }

    /**
     * Получает статистику по зонам
     */
    public String getStatistics() {
        int totalZones = zones.size();
        int completeZones = (int) zones.values().stream().filter(ClassSelectionZone::isComplete).count();
        
        return String.format("Total zones: %d, Complete: %d, Incomplete: %d", 
            totalZones, completeZones, totalZones - completeZones);
    }
}

