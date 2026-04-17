package ru.liko.pjmbasemod.common.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Представляет зону выбора класса - прямоугольную область в мире,
 * где игроки могут открывать меню выбора класса.
 */
public class ClassSelectionZone {
    private BlockPos pos1;
    private BlockPos pos2;
    private ResourceKey<Level> dimension;

    public ClassSelectionZone(BlockPos pos1, BlockPos pos2, ResourceKey<Level> dimension) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.dimension = dimension;
    }

    /**
     * Конструктор по умолчанию для создания пустой зоны
     */
    public ClassSelectionZone() {
        this.pos1 = null;
        this.pos2 = null;
        this.dimension = Level.OVERWORLD;
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    /**
     * Проверяет, является ли зона полностью определенной (обе позиции установлены)
     */
    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    /**
     * Проверяет, находится ли позиция внутри зоны
     */
    public boolean contains(Vec3 position, ResourceKey<Level> playerDimension) {
        if (!isComplete()) {
            return false;
        }

        if (!this.dimension.equals(playerDimension)) {
            return false;
        }

        // Используем AABB для более точной проверки
        AABB aabb = getAABB();
        boolean result = aabb.contains(position);
        
        // Детальное логирование для отладки (только если включен debug-режим)
        if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled() && 
            com.mojang.logging.LogUtils.getLogger().isDebugEnabled()) {
            com.mojang.logging.LogUtils.getLogger().debug(
                "Zone check: position={}, dimension={}, zone_dim={}, aabb={}, result={}",
                position, playerDimension.location(), this.dimension.location(), aabb, result
            );
        }
        
        return result;
    }

    /**
     * Проверяет, находится ли BlockPos внутри зоны
     */
    public boolean contains(BlockPos position, ResourceKey<Level> playerDimension) {
        return contains(Vec3.atCenterOf(position), playerDimension);
    }

    /**
     * Возвращает AABB (Axis-Aligned Bounding Box) для визуализации зоны
     */
    public AABB getAABB() {
        if (!isComplete()) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }

        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Очищает зону (удаляет обе позиции)
     */
    public void clear() {
        this.pos1 = null;
        this.pos2 = null;
    }

    /**
     * Сохраняет зону в NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        
        if (pos1 != null) {
            tag.putInt("pos1X", pos1.getX());
            tag.putInt("pos1Y", pos1.getY());
            tag.putInt("pos1Z", pos1.getZ());
        }
        
        if (pos2 != null) {
            tag.putInt("pos2X", pos2.getX());
            tag.putInt("pos2Y", pos2.getY());
            tag.putInt("pos2Z", pos2.getZ());
        }
        
        tag.putString("dimension", dimension.location().toString());
        
        return tag;
    }

    /**
     * Загружает зону из NBT
     */
    public static ClassSelectionZone load(CompoundTag tag) {
        ClassSelectionZone zone = new ClassSelectionZone();
        
        if (tag.contains("pos1X")) {
            zone.pos1 = new BlockPos(
                tag.getInt("pos1X"),
                tag.getInt("pos1Y"),
                tag.getInt("pos1Z")
            );
        }
        
        if (tag.contains("pos2X")) {
            zone.pos2 = new BlockPos(
                tag.getInt("pos2X"),
                tag.getInt("pos2Y"),
                tag.getInt("pos2Z")
            );
        }
        
        if (tag.contains("dimension")) {
            String dimensionStr = tag.getString("dimension");
            zone.dimension = ResourceKey.create(Registries.DIMENSION, 
                ResourceLocation.tryParse(dimensionStr));
        }
        
        return zone;
    }

    /**
     * Возвращает объём зоны в блоках
     */
    public long getVolume() {
        if (!isComplete()) {
            return 0;
        }

        int dx = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int dy = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int dz = Math.abs(pos2.getZ() - pos1.getZ()) + 1;

        return (long) dx * dy * dz;
    }

    @Override
    public String toString() {
        if (!isComplete()) {
            if (pos1 != null) {
                return String.format("Zone[pos1=%s, pos2=not set, dim=%s]", 
                    formatPos(pos1), dimension.location());
            }
            return "Zone[not set]";
        }

        return String.format("Zone[%s to %s in %s]", 
            formatPos(pos1), formatPos(pos2), dimension.location());
    }

    private String formatPos(BlockPos pos) {
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }
}

