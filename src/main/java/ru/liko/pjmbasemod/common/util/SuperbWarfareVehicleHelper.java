package ru.liko.pjmbasemod.common.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.common.vehicle.VehicleSpawnPoint;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Утилиты для интеграции с SuperbWarfare.
 * Использует стандартные механизмы Minecraft для спавна техники.
 */
public final class SuperbWarfareVehicleHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "superbwarfare";

    private SuperbWarfareVehicleHelper() {}

    public static boolean isModPresent() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Спавнит технику SuperbWarfare по конфигурации точки. Возвращает UUID сущности.
     */
    public static Optional<UUID> spawnVehicle(ServerLevel level, VehicleSpawnPoint point) {
        if (!isModPresent()) {
            return Optional.empty();
        }

        try {
            String vehicleId = point.getVehicleId();
            if (vehicleId == null || vehicleId.isEmpty()) {
                LOGGER.warn("Неверный идентификатор техники SuperbWarfare: {}", vehicleId);
                return Optional.empty();
            }

            // Парсим ResourceLocation из vehicleId
            ResourceLocation entityId = ResourceLocation.tryParse(vehicleId);
            if (entityId == null) {
                LOGGER.warn("Не удалось распарсить ResourceLocation из: {}", vehicleId);
                return Optional.empty();
            }

            // Получаем EntityType из реестра
            EntityType<?> entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(entityId);
            if (entityType == null) {
                LOGGER.warn("Техника SuperbWarfare {} не найдена в реестре", vehicleId);
                return Optional.empty();
            }

            // Создаем сущность
            Entity entity = entityType.create(level);
            if (entity == null) {
                LOGGER.warn("Не удалось создать сущность техники {}", vehicleId);
                return Optional.empty();
            }

            // Загружаем NBT данные, если они есть (ДО установки позиции)
            Optional<CompoundTag> nbtOpt = point.getVehicleNbt();
            if (nbtOpt.isPresent()) {
                CompoundTag nbt = nbtOpt.get();
                // Для SuperbWarfare техники NBT из BlockEntityData содержит:
                // - "Entity" - полные данные сущности (включая инвентарь, здоровье и т.д.)
                // - "EntityType" - тип сущности (уже использован выше)
                if (nbt.contains("Entity")) {
                    // Загружаем полные данные сущности из тега "Entity"
                    CompoundTag entityNbt = nbt.getCompound("Entity");
                    // Сохраняем позицию и поворот из точки спавна, чтобы они не перезаписались
                    BlockPos spawnPos = point.getPosition();
                    float spawnYaw = point.getYaw();
                    
                    // Загружаем NBT (может перезаписать позицию)
                    entity.load(entityNbt);
                    
                    // Восстанавливаем позицию и поворот из точки спавна
                    double x = spawnPos.getX() + 0.5D;
                    double y = spawnPos.getY() + 1.0D;
                    double z = spawnPos.getZ() + 0.5D;
                    entity.setPos(x, y, z);
                    entity.setYRot(spawnYaw);
                    
                    LOGGER.debug("Загружены NBT данные техники из тега Entity ({} байт), позиция восстановлена", entityNbt.size());
                } else {
                    // Если структура другая, пробуем загрузить напрямую
                    entity.load(nbt);
                    LOGGER.debug("Загружены NBT данные техники напрямую ({} байт)", nbt.size());
                }
            }

            // Устанавливаем позицию и поворот (если NBT не был загружен)
            if (!nbtOpt.isPresent() || !nbtOpt.get().contains("Entity")) {
                BlockPos pos = point.getPosition();
                double x = pos.getX() + 0.5D;
                double y = pos.getY() + 1.0D;
                double z = pos.getZ() + 0.5D;
                entity.setPos(x, y, z);
                entity.setYRot(point.getYaw());
            }

            // Для VehicleEntity устанавливаем serverYaw через рефлексию
            if (isVehicleEntity(entity)) {
                setServerYaw(entity, point.getYaw());
            }

            // Спавним сущность
            level.addFreshEntity(entity);

            UUID uuid = entity.getUUID();
            LOGGER.debug("Заспавнена техника SuperbWarfare {} с UUID {}", vehicleId, uuid);
            return Optional.of(uuid);
        } catch (Exception ex) {
            LOGGER.error("Не удалось заспавнить технику SuperbWarfare", ex);
            return Optional.empty();
        }
    }

    /**
     * Проверяет, существует ли еще сущность техники по UUID.
     */
    public static boolean isVehicleAlive(Level level, @Nullable UUID uuid) {
        if (!isModPresent() || uuid == null || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        try {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity == null) {
                return false;
            }
            // Проверяем, что сущность не удалена
            return entity.isAlive() && !entity.isRemoved();
        } catch (Exception ex) {
            LOGGER.error("Ошибка проверки состояния техники SuperbWarfare", ex);
            return false;
        }
    }

    /**
     * Удаляет технику по UUID, если она присутствует.
     */
    public static void removeVehicle(Level level, @Nullable UUID uuid) {
        if (!isModPresent() || uuid == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity != null) {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        } catch (Exception ex) {
            LOGGER.error("Не удалось удалить технику SuperbWarfare", ex);
        }
    }

    /**
     * Извлекает UUID техники из сущности, если это техника SuperbWarfare.
     */
    public static Optional<UUID> extractVehicleUUID(Entity entity) {
        if (!isModPresent() || entity == null) {
            return Optional.empty();
        }
        try {
            if (isVehicleEntity(entity)) {
                return Optional.of(entity.getUUID());
            }
        } catch (Exception ex) {
            LOGGER.error("Не удалось определить UUID техники SuperbWarfare", ex);
        }
        return Optional.empty();
    }

    /**
     * Проверяет, является ли сущность техникой SuperbWarfare.
     */
    private static boolean isVehicleEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        try {
            // Проверяем, что сущность является VehicleEntity через проверку класса
            String className = entity.getClass().getName();
            return className.contains("superbwarfare") && 
                   (className.contains("VehicleEntity") || 
                    entity.getClass().getSuperclass() != null && 
                    entity.getClass().getSuperclass().getName().contains("VehicleEntity"));
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Устанавливает serverYaw для VehicleEntity через рефлексию.
     */
    private static void setServerYaw(Entity entity, float yaw) {
        try {
            // Пытаемся найти метод setServerYaw
            java.lang.reflect.Method method = entity.getClass().getMethod("setServerYaw", float.class);
            method.invoke(entity, yaw);
        } catch (NoSuchMethodException e) {
            // Метод может отсутствовать, это нормально
        } catch (Exception ex) {
            LOGGER.debug("Не удалось установить serverYaw для техники", ex);
        }
    }
}

