package ru.liko.pjmbasemod.common.gamemode;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Менеджер контрольных точек. Хранит данные в SavedData в overworld.
 * NeoForge 1.21.1: Updated SavedData API with Factory pattern.
 */
public class ControlPointManager extends SavedData {
    private static final String DATA_NAME = "Pjmbasemod_control_points";

    private final Map<String, ControlPoint> points = new LinkedHashMap<>();

    public ControlPointManager() {
    }

    // NeoForge 1.21.1: SavedData uses Factory pattern
    private static final SavedData.Factory<ControlPointManager> FACTORY = new SavedData.Factory<>(
        ControlPointManager::new,
        (tag, provider) -> load(tag),
        null // No data fixer
    );

    public static ControlPointManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void addOrUpdate(ControlPoint point) {
        points.put(point.getId(), point);
        setDirty();
    }

    public boolean remove(String id) {
        boolean removed = points.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public Optional<ControlPoint> get(String id) {
        return Optional.ofNullable(points.get(id));
    }

    public Collection<ControlPoint> getAll() {
        return Collections.unmodifiableCollection(points.values());
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public void clear() {
        points.clear();
        setDirty();
    }

    public void resetOwnership(String defaultOwner) {
        for (ControlPoint point : points.values()) {
            point.setOwnerTeam(defaultOwner == null ? "" : defaultOwner);
            point.resetCapture();
        }
        setDirty();
    }

    public void markDirty() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag listTag = new ListTag();
        for (ControlPoint point : points.values()) {
            listTag.add(point.save());
        }
        tag.put("Points", listTag);
        return tag;
    }

    private static ControlPointManager load(CompoundTag tag) {
        ControlPointManager manager = new ControlPointManager();
        if (tag.contains("Points")) {
            ListTag list = tag.getList("Points", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag pointTag = list.getCompound(i);
                ControlPoint point = ControlPoint.load(pointTag);
                manager.addOrUpdate(point);
            }
        }
        manager.setDirty(false);
        return manager;
    }
}

