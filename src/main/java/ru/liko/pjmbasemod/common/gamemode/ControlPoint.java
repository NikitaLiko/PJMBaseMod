package ru.liko.pjmbasemod.common.gamemode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Модель контрольной точки для режимов захвата.
 */
public class ControlPoint {
    private final String id;
    private String displayName;
    // Removed GameMode as everything is now WAR
    private ResourceKey<Level> dimension;
    private Vec3 position; // Kept as "center" or "icon position"
    // Removed radius, replaced with min/max bounds
    private Vec3 min;
    private Vec3 max;
    // Removed Order, replaced with Graph system
    private Set<String> neighbors = new HashSet<>();
    
    private String ownerTeam;
    private String capturingTeam;
    private double captureProgress;
    
    // Configurable properties
    private int captureTimeSeconds = -1; // -1 means use global config
    private int ticketValue = 0; // Deprecated, kept for compatibility or resource generation
    private int minPlayersToCap = 1;
    private boolean safeZone = false;
    private int safeZoneGracePeriod = 5;

    public ControlPoint(String displayName, ResourceKey<Level> dimension, Vec3 position, double radius) {
        this(UUID.randomUUID().toString(), displayName, dimension, position, radius);
    }

    public ControlPoint(String id, String displayName, ResourceKey<Level> dimension, Vec3 position, double radius) {
        this.id = id;
        this.displayName = displayName;
        this.dimension = dimension;
        this.position = position;
        // Initialize bounds based on radius for compatibility
        this.min = position.subtract(radius, radius, radius);
        this.max = position.add(radius, radius, radius);
        this.ownerTeam = "";
        this.capturingTeam = "";
        this.captureProgress = 0.0d;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public Vec3 getMin() {
        return min;
    }

    public Vec3 getMax() {
        return max;
    }

    public void setBounds(Vec3 pos1, Vec3 pos2) {
        double minX = Math.min(pos1.x, pos2.x);
        double minY = Math.min(pos1.y, pos2.y);
        double minZ = Math.min(pos1.z, pos2.z);
        double maxX = Math.max(pos1.x, pos2.x);
        double maxY = Math.max(pos1.y, pos2.y);
        double maxZ = Math.max(pos1.z, pos2.z);
        
        this.min = new Vec3(minX, minY, minZ);
        this.max = new Vec3(maxX, maxY, maxZ);
        // Update center position to be the center of the box
        this.position = new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
    }

    public Set<String> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(String id) {
        this.neighbors.add(id);
    }

    public void removeNeighbor(String id) {
        this.neighbors.remove(id);
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    public String getCapturingTeam() {
        return capturingTeam;
    }

    public void setCapturingTeam(String capturingTeam) {
        this.capturingTeam = capturingTeam;
    }

    public double getCaptureProgress() {
        return captureProgress;
    }

    public void setCaptureProgress(double captureProgress) {
        this.captureProgress = captureProgress;
    }
    
    public int getCaptureTimeSeconds() {
        return captureTimeSeconds;
    }

    public void setCaptureTimeSeconds(int captureTimeSeconds) {
        this.captureTimeSeconds = captureTimeSeconds;
    }

    public int getTicketValue() {
        return ticketValue;
    }

    public void setTicketValue(int ticketValue) {
        this.ticketValue = ticketValue;
    }

    public int getMinPlayersToCap() {
        return minPlayersToCap;
    }

    public void setMinPlayersToCap(int minPlayersToCap) {
        this.minPlayersToCap = minPlayersToCap;
    }

    public boolean isSafeZone() {
        return safeZone;
    }

    public void setSafeZone(boolean safeZone) {
        this.safeZone = safeZone;
    }

    public int getSafeZoneGracePeriod() {
        return safeZoneGracePeriod;
    }

    public void setSafeZoneGracePeriod(int safeZoneGracePeriod) {
        this.safeZoneGracePeriod = safeZoneGracePeriod;
    }

    public boolean isInside(Vec3 point) {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z;
    }

    public BlockPos asBlockPos() {
        return BlockPos.containing(position);
    }

    public void resetCapture() {
        this.capturingTeam = "";
        this.captureProgress = 0.0d;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Name", displayName);
        tag.putString("Dimension", dimension.location().toString());
        tag.putDouble("PosX", position.x);
        tag.putDouble("PosY", position.y);
        tag.putDouble("PosZ", position.z);
        
        // Save AABB
        tag.putDouble("MinX", min.x);
        tag.putDouble("MinY", min.y);
        tag.putDouble("MinZ", min.z);
        tag.putDouble("MaxX", max.x);
        tag.putDouble("MaxY", max.y);
        tag.putDouble("MaxZ", max.z);
        
        ListTag neighborsTag = new ListTag();
        for (String neighborId : neighbors) {
            neighborsTag.add(StringTag.valueOf(neighborId));
        }
        tag.put("Neighbors", neighborsTag);

        tag.putString("Owner", ownerTeam == null ? "" : ownerTeam);
        tag.putString("Capturing", capturingTeam == null ? "" : capturingTeam);
        tag.putDouble("Progress", captureProgress);
        tag.putInt("CaptureTime", captureTimeSeconds);
        tag.putInt("TicketValue", ticketValue);
        tag.putInt("MinPlayers", minPlayersToCap);
        tag.putBoolean("SafeZone", safeZone);
        tag.putInt("SafeZoneGrace", safeZoneGracePeriod);
        return tag;
    }

    public static ControlPoint load(CompoundTag tag) {
        String id = tag.getString("Id");
        String name = tag.getString("Name");
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        Vec3 pos = new Vec3(tag.getDouble("PosX"), tag.getDouble("PosY"), tag.getDouble("PosZ"));
        
        double radius = tag.contains("Radius") ? tag.getDouble("Radius") : 10.0; // Legacy support

        ControlPoint point = new ControlPoint(id, name, dimension, pos, radius);
        
        if (tag.contains("MinX")) {
             point.min = new Vec3(tag.getDouble("MinX"), tag.getDouble("MinY"), tag.getDouble("MinZ"));
             point.max = new Vec3(tag.getDouble("MaxX"), tag.getDouble("MaxY"), tag.getDouble("MaxZ"));
        }
        
        if (tag.contains("Neighbors")) {
            ListTag neighborsTag = tag.getList("Neighbors", CompoundTag.TAG_STRING);
            for (int i = 0; i < neighborsTag.size(); i++) {
                point.addNeighbor(neighborsTag.getString(i));
            }
        }

        point.setOwnerTeam(tag.getString("Owner"));
        point.setCapturingTeam(tag.getString("Capturing"));
        point.setCaptureProgress(tag.getDouble("Progress"));
        
        if (tag.contains("CaptureTime")) point.setCaptureTimeSeconds(tag.getInt("CaptureTime"));
        if (tag.contains("TicketValue")) point.setTicketValue(tag.getInt("TicketValue"));
        if (tag.contains("MinPlayers")) point.setMinPlayersToCap(tag.getInt("MinPlayers"));
        if (tag.contains("SafeZone")) point.setSafeZone(tag.getBoolean("SafeZone"));
        if (tag.contains("SafeZoneGrace")) point.setSafeZoneGracePeriod(tag.getInt("SafeZoneGrace"));
        
        return point;
    }
}

