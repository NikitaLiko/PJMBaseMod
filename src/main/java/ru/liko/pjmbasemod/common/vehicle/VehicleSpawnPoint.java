package ru.liko.pjmbasemod.common.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Описание точки автоспавна техники SuperbWarfare.
 * Хранит конфигурацию позиции, команды, техники и состояние запланированного респавна.
 */
public class VehicleSpawnPoint {
    private final String name;
    private BlockPos position;
    private float yaw;
    private ResourceKey<Level> dimension;
    private String team;
    private String vehicleId; // ResourceLocation формата namespace:entity_id
    private int respawnIntervalSeconds;
    private UUID currentVehicleId;
    private boolean respawnScheduled;
    private long respawnTicksRemaining;
    private CompoundTag vehicleNbt;

    public VehicleSpawnPoint(String name) {
        this.name = name;
        this.dimension = Level.OVERWORLD;
        this.team = "";
        this.vehicleId = "";
        this.respawnIntervalSeconds = 0;
        this.respawnScheduled = false;
        this.respawnTicksRemaining = 0;
    }

    public String getName() {
        return name;
    }

    public BlockPos getPosition() {
        return position;
    }

    public void setPosition(BlockPos position) {
        this.position = position;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public boolean hasVehicleNbt() {
        return vehicleNbt != null && !vehicleNbt.isEmpty();
    }

    public Optional<CompoundTag> getVehicleNbt() {
        return hasVehicleNbt() ? Optional.of(vehicleNbt) : Optional.empty();
    }

    public void setVehicleNbt(@Nullable CompoundTag tag) {
        this.vehicleNbt = tag != null ? tag.copy() : null;
    }

    public void clearVehicleNbt() {
        this.vehicleNbt = null;
    }

    public int getRespawnIntervalSeconds() {
        return respawnIntervalSeconds;
    }

    public void setRespawnIntervalSeconds(int respawnIntervalSeconds) {
        this.respawnIntervalSeconds = Math.max(0, respawnIntervalSeconds);
    }

    @Nullable
    public UUID getCurrentVehicleId() {
        return currentVehicleId;
    }

    public void setCurrentVehicleId(@Nullable UUID currentVehicleId) {
        this.currentVehicleId = currentVehicleId;
    }

    public boolean isRespawnScheduled() {
        return respawnScheduled;
    }

    public long getRespawnTicksRemaining() {
        return respawnTicksRemaining;
    }

    public void scheduleRespawn() {
        scheduleRespawn(false);
    }

    public void scheduleRespawn(boolean immediate) {
        respawnScheduled = true;
        if (immediate || respawnIntervalSeconds <= 0) {
            respawnTicksRemaining = 0;
        } else {
            respawnTicksRemaining = respawnIntervalSeconds * 20L;
        }
    }

    public void cancelRespawn() {
        respawnScheduled = false;
        respawnTicksRemaining = 0;
    }

    /**
     * Тик респавна. Возвращает true, если необходимо инициировать спавн техники.
     */
    public boolean tickRespawn() {
        if (!respawnScheduled) {
            return false;
        }
        if (respawnTicksRemaining > 0) {
            respawnTicksRemaining--;
            return false;
        }
        respawnScheduled = false;
        return true;
    }

    /**
     * Проверяет, полностью ли настроена точка.
     */
    public boolean isComplete() {
        return position != null && !team.isEmpty() && !vehicleId.isEmpty();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        if (position != null) {
            tag.putInt("posX", position.getX());
            tag.putInt("posY", position.getY());
            tag.putInt("posZ", position.getZ());
        }
        tag.putFloat("yaw", yaw);
        tag.putString("dimension", dimension.location().toString());
        tag.putString("team", team);
        tag.putString("vehicleId", vehicleId);
        tag.putInt("respawnInterval", respawnIntervalSeconds);
        if (currentVehicleId != null) {
            tag.putUUID("currentVehicleId", currentVehicleId);
        }
        tag.putBoolean("respawnScheduled", respawnScheduled);
        tag.putLong("respawnTicksRemaining", respawnTicksRemaining);
        if (vehicleNbt != null) {
            tag.put("vehicleNbt", vehicleNbt.copy());
        }
        return tag;
    }

    public static VehicleSpawnPoint load(String name, CompoundTag tag) {
        VehicleSpawnPoint point = new VehicleSpawnPoint(name);
        if (tag.contains("posX")) {
            point.position = new BlockPos(
                tag.getInt("posX"),
                tag.getInt("posY"),
                tag.getInt("posZ")
            );
        }
        point.yaw = tag.getFloat("yaw");
        if (tag.contains("dimension")) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
            if (dimensionId != null) {
                point.dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            }
        }
        point.team = tag.getString("team");
        point.vehicleId = tag.getString("vehicleId");
        point.respawnIntervalSeconds = tag.getInt("respawnInterval");
        if (tag.hasUUID("currentVehicleId")) {
            point.currentVehicleId = tag.getUUID("currentVehicleId");
        }
        point.respawnScheduled = tag.getBoolean("respawnScheduled");
        point.respawnTicksRemaining = tag.getLong("respawnTicksRemaining");
        if (tag.contains("vehicleNbt")) {
            point.vehicleNbt = tag.getCompound("vehicleNbt").copy();
        }
        return point;
    }

    @Override
    public String toString() {
        return "VehicleSpawnPoint{" +
            "name='" + name + '\'' +
            ", position=" + position +
            ", yaw=" + yaw +
            ", dimension=" + dimension.location() +
            ", team='" + team + '\'' +
            ", vehicleId='" + vehicleId + '\'' +
            ", respawnIntervalSeconds=" + respawnIntervalSeconds +
            ", currentVehicleId=" + currentVehicleId +
            ", respawnScheduled=" + respawnScheduled +
            ", respawnTicksRemaining=" + respawnTicksRemaining +
            ", hasVehicleNbt=" + hasVehicleNbt() +
            '}';
    }
}

