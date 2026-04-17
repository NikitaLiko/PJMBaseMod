package ru.liko.pjmbasemod.common.gamemode;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Легковесная структура для синхронизации контрольных точек.
 */
public record ControlPointSnapshot(
        String id,
        String displayName,
        String ownerTeam,
        String capturingTeam,
        double captureProgress,
        ResourceLocation dimension,
        Vec3 position,
        Vec3 min,
        Vec3 max,
        int captureTimeSeconds,
        Set<String> neighbors) {

    public static ControlPointSnapshot from(ControlPoint point) {
        return new ControlPointSnapshot(
                point.getId(),
                point.getDisplayName(),
                point.getOwnerTeam(),
                point.getCapturingTeam(),
                point.getCaptureProgress(),
                point.getDimension().location(),
                point.getPosition(),
                point.getMin(),
                point.getMax(),
                point.getCaptureTimeSeconds(),
                new HashSet<>(point.getNeighbors())
        );
    }

    public static ControlPointSnapshot read(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String display = buf.readUtf();
        String owner = buf.readUtf();
        String capturing = buf.readUtf();
        double progress = buf.readDouble();
        ResourceLocation dimension = buf.readResourceLocation();
        Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 min = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 max = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int captureTime = buf.readInt();
        
        int neighborCount = buf.readVarInt();
        Set<String> neighbors = new HashSet<>(neighborCount);
        for (int i = 0; i < neighborCount; i++) {
            neighbors.add(buf.readUtf());
        }
        
        return new ControlPointSnapshot(id, display, owner, capturing, progress, dimension, position, min, max, captureTime, neighbors);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeUtf(displayName);
        buf.writeUtf(ownerTeam == null ? "" : ownerTeam);
        buf.writeUtf(capturingTeam == null ? "" : capturingTeam);
        buf.writeDouble(captureProgress);
        ResourceLocation dimensionId = dimension == null
                ? ResourceLocation.fromNamespaceAndPath("minecraft", "overworld")
                : dimension;
        Vec3 pos = position == null ? Vec3.ZERO : position;
        buf.writeResourceLocation(dimensionId);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        
        // Write AABB
        Vec3 safeMin = min == null ? pos : min;
        Vec3 safeMax = max == null ? pos : max;
        buf.writeDouble(safeMin.x);
        buf.writeDouble(safeMin.y);
        buf.writeDouble(safeMin.z);
        buf.writeDouble(safeMax.x);
        buf.writeDouble(safeMax.y);
        buf.writeDouble(safeMax.z);
        
        buf.writeInt(captureTimeSeconds);
        
        buf.writeVarInt(neighbors.size());
        for (String neighbor : neighbors) {
            buf.writeUtf(neighbor);
        }
    }
    
    // Helper for compatibility
    public double radius() {
        // Approximate radius from box
        if (min == null || max == null) return 10.0;
        return (max.x - min.x) / 2.0;
    }
    
    // Helper to maintain compatibility with existing code (order is now 0 or hash)
    public int order() {
        return 0;
    }
}

