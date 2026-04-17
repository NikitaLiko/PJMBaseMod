package ru.liko.pjmbasemod.common.vehicle;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер точек автоспавна техники SuperbWarfare. Отвечает за загрузку/сохранение и поиск точек.
 */
public class VehicleSpawnPointManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "Pjmbasemod_vehicle_spawns.dat";

    private final MinecraftServer server;
    private final Map<String, VehicleSpawnPoint> spawnPoints = new HashMap<>();

    public VehicleSpawnPointManager(MinecraftServer server) {
        this.server = server;
    }

    public Collection<VehicleSpawnPoint> getAllPoints() {
        return spawnPoints.values();
    }

    public Map<String, VehicleSpawnPoint> getPointsSnapshot() {
        return new HashMap<>(spawnPoints);
    }

    public VehicleSpawnPoint getOrCreate(String name) {
        return spawnPoints.computeIfAbsent(name, VehicleSpawnPoint::new);
    }

    public void remove(String name) {
        if (spawnPoints.remove(name) != null) {
            save();
        }
    }

    public void clear() {
        spawnPoints.clear();
        save();
    }

    public void set(String name, VehicleSpawnPoint point) {
        spawnPoints.put(name, point);
        save();
    }

    @Nullable
    public VehicleSpawnPoint get(String name) {
        return spawnPoints.get(name);
    }

    @Nullable
    public VehicleSpawnPoint getByVehicleId(UUID vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        return spawnPoints.values()
            .stream()
            .filter(point -> vehicleId.equals(point.getCurrentVehicleId()))
            .findFirst()
            .orElse(null);
    }

    public void save() {
        try {
            File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
            File dataFile = new File(worldDir, FILE_NAME);

            CompoundTag rootTag = new CompoundTag();
            CompoundTag pointsTag = new CompoundTag();
            for (Map.Entry<String, VehicleSpawnPoint> entry : spawnPoints.entrySet()) {
                pointsTag.put(entry.getKey(), entry.getValue().save());
            }
            rootTag.put("points", pointsTag);
            rootTag.putInt("version", 1);
            NbtIo.writeCompressed(rootTag, dataFile.toPath());

            if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                LOGGER.info("Saved {} vehicle spawn point(s)", spawnPoints.size());
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to save vehicle spawn points", ex);
        }
    }

    public void load() {
        try {
            File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
            File dataFile = new File(worldDir, FILE_NAME);

            if (!dataFile.exists()) {
                if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                    LOGGER.info("Vehicle spawn point file not found, starting clean");
                }
                return;
            }

            CompoundTag rootTag = NbtIo.readCompressed(dataFile.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            if (!rootTag.contains("points")) {
                LOGGER.warn("Invalid vehicle spawn point data format");
                return;
            }

            spawnPoints.clear();
            CompoundTag pointsTag = rootTag.getCompound("points");
            for (String key : pointsTag.getAllKeys()) {
                VehicleSpawnPoint point = VehicleSpawnPoint.load(key, pointsTag.getCompound(key));
                spawnPoints.put(key, point);
            }

            if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                LOGGER.info("Loaded {} vehicle spawn point(s)", spawnPoints.size());
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to load vehicle spawn points", ex);
        }
    }

    public String getStatistics() {
        int total = spawnPoints.size();
        long configured = spawnPoints.values().stream().filter(VehicleSpawnPoint::isComplete).count();
        return String.format("Points: %d, Configured: %d, Incomplete: %d", total, configured, total - configured);
    }
}

