package ru.liko.pjmbasemod.common.vehicle;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.common.util.SuperbWarfareVehicleHelper;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Центральная система автоспавна техники SuperbWarfare. Управляет жизненным циклом техники и очередью респавна.
 */
public final class VehicleSpawnSystem {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final VehicleSpawnSystem NO_OP = new VehicleSpawnSystem(null, null);

    private static VehicleSpawnSystem instance = NO_OP;

    private final MinecraftServer server;
    private final VehicleSpawnPointManager manager;

    private VehicleSpawnSystem(@Nullable MinecraftServer server, @Nullable VehicleSpawnPointManager manager) {
        this.server = server;
        this.manager = manager;
    }

    public static void initialize(MinecraftServer server, VehicleSpawnPointManager manager) {
        instance = new VehicleSpawnSystem(server, manager);
    }

    public static void reset() {
        instance = NO_OP;
    }

    public static VehicleSpawnSystem getInstance() {
        return instance;
    }

    public void tick() {
        if (!isOperational() || !SuperbWarfareVehicleHelper.isModPresent()) {
            return;
        }

        boolean dirty = false;
        for (VehicleSpawnPoint point : manager.getAllPoints()) {
            ServerLevel level = server.getLevel(point.getDimension());
            if (level == null) {
                continue;
            }

            UUID currentVehicleId = point.getCurrentVehicleId();
            if (currentVehicleId != null) {
                if (!SuperbWarfareVehicleHelper.isVehicleAlive(level, currentVehicleId)) {
                    handleVehicleDestroyed(point);
                    dirty = true;
                }
                continue;
            }

            if (!point.isComplete()) {
                continue;
            }

            if (!point.isRespawnScheduled()) {
                point.scheduleRespawn(true);
                dirty = true;
            }

            if (point.tickRespawn()) {
                boolean spawned = spawnVehicle(point, level);
                if (!spawned) {
                    point.scheduleRespawn(true);
                } else {
                    dirty = true;
                }
            }
        }

        if (dirty) {
            manager.save();
        }
    }

    public boolean forceSpawn(VehicleSpawnPoint point, ServerLevel level) {
        if (!isOperational() || level == null || !point.isComplete()) {
            return false;
        }

        if (point.getCurrentVehicleId() != null) {
            SuperbWarfareVehicleHelper.removeVehicle(level, point.getCurrentVehicleId());
            point.setCurrentVehicleId(null);
        }

        boolean spawned = spawnVehicle(point, level);
        if (!spawned) {
            point.scheduleRespawn(true);
        }
        manager.save();
        return spawned;
    }

    public void handleVehicleRemoval(UUID vehicleId) {
        if (!isOperational() || vehicleId == null) {
            return;
        }
        VehicleSpawnPoint point = manager.getByVehicleId(vehicleId);
        if (point != null) {
            handleVehicleDestroyed(point);
            manager.save();
        }
    }

    private void handleVehicleDestroyed(VehicleSpawnPoint point) {
        point.setCurrentVehicleId(null);
        point.scheduleRespawn(false);
    }

    private boolean spawnVehicle(VehicleSpawnPoint point, ServerLevel level) {
        if (!point.isComplete()) {
            return false;
        }

        return SuperbWarfareVehicleHelper.spawnVehicle(level, point)
            .map(uuid -> {
                point.setCurrentVehicleId(uuid);
                point.cancelRespawn();
                LOGGER.info("Заспавнена техника {} на точке {}", point.getVehicleId(), point.getName());
                return true;
            })
            .orElseGet(() -> {
                LOGGER.warn("Не удалось заспавнить технику {} на точке {}", point.getVehicleId(), point.getName());
                return false;
            });
    }

    private boolean isOperational() {
        return server != null && manager != null;
    }
}

