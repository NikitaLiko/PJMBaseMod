package ru.liko.pjmbasemod.common.vehicle;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmPlayerDataProvider;

import java.util.List;
import java.util.Optional;

/**
 * Система ограничений для сидений техники SuperbWarfare - проверяет, может ли игрок сесть на определенное место.
 * Только игроки с классом CREW могут садиться на водительское и наводческое места в указанной технике.
 */
public final class VehicleCrewRestriction {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_SEAT_INDEX = "SeatIndex";
    
    private VehicleCrewRestriction() {}
    
    /**
     * Проверяет, является ли сущность техникой SuperbWarfare.
     */
    public static boolean isSuperbWarfareVehicle(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        // Проверяем через рефлексию, является ли сущность VehicleEntity из SuperbWarfare
        try {
            Class<?> vehicleEntityClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            return vehicleEntityClass.isInstance(entity);
        } catch (ClassNotFoundException e) {
            // SuperbWarfare не установлен
            return false;
        }
    }
    
    /**
     * Проверяет, может ли игрок сесть на указанное место в технике.
     * @param player Игрок, пытающийся сесть
     * @param vehicle Техника SuperbWarfare
     * @param seatIndex Индекс сиденья (0 = водитель, 1 = наводчик, 2+ = другие места экипажа)
     * @return Optional с сообщением об ошибке, если посадка запрещена, или empty если разрешена
     */
    public static Optional<Component> canPlayerSitOnSeat(ServerPlayer player, Entity vehicle, int seatIndex) {
        // Проверяем, является ли это техникой SuperbWarfare
        if (!isSuperbWarfareVehicle(vehicle)) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Сущность {} не является техникой SuperbWarfare", vehicle.getType());
            }
            return Optional.empty();
        }
        
        // Получаем ID техники через ResourceLocation
        ResourceLocation vehicleId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        if (vehicleId == null) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Не удалось получить ID техники для {}", vehicle);
            }
            return Optional.empty();
        }
        
        String vehicleIdString = vehicleId.toString();
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.debug("Проверка посадки: игрок {}, техника {}, место {}", 
                player.getName().getString(), vehicleIdString, seatIndex);
        }
        
        // Проверяем, есть ли эта техника в списке ограниченных
        List<String> restrictedVehicles = Config.getCrewRestrictedVehicles();
        boolean isRestrictedVehicle = false;
        
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.debug("Список ограниченных техник: {}", restrictedVehicles);
        }
        
        for (String restrictedId : restrictedVehicles) {
            if (matchesVehicleId(vehicleIdString, restrictedId)) {
                isRestrictedVehicle = true;
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.debug("Техника {} найдена в списке ограничений (шаблон: {})", vehicleIdString, restrictedId);
                }
                break;
            }
        }
        
        if (!isRestrictedVehicle) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Техника {} не в списке ограничений", vehicleIdString);
            }
            return Optional.empty(); // Техника не в списке ограничений
        }
        
        // Проверяем, требует ли это сиденье класс CREW
        List<Integer> restrictedSeats = Config.getCrewRestrictedSeats();
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.debug("Ограниченные места: {}, место игрока: {}", restrictedSeats, seatIndex);
        }
        
        if (!restrictedSeats.contains(seatIndex)) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Место {} не требует класс CREW", seatIndex);
            }
            return Optional.empty(); // Это сиденье не требует класс CREW (например, десантное отделение)
        }
        
        // Проверяем класс игрока
        PjmPlayerData playerData = PjmPlayerDataProvider.get(player);
        
        if (playerData == null) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Не удалось получить данные игрока {}", player.getName().getString());
            }
            return Optional.of(Component.translatable("wrb.vehicle.crew.required"));
        }
        
        PjmPlayerClass playerClass = playerData.getPlayerClass();
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.debug("Класс игрока {}: {}", player.getName().getString(), playerClass);
        }
        
        if (playerClass != PjmPlayerClass.CREW) {
            LOGGER.info("Игрок {} (класс: {}) пытается сесть на место {} в технике {}, требуется класс CREW", 
                player.getName().getString(), playerClass, seatIndex, vehicleIdString);
            return Optional.of(Component.translatable("wrb.vehicle.crew.required"));
        }
        
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.debug("Игрок {} с классом CREW может сесть на место {} в технике {}", 
                player.getName().getString(), seatIndex, vehicleIdString);
        }
        
        return Optional.empty(); // Разрешено
    }
    
    /**
     * Получает индекс сиденья игрока в технике SuperbWarfare.
     */
    public static Optional<Integer> getPlayerSeatIndex(ServerPlayer player, Entity vehicle) {
        if (!isSuperbWarfareVehicle(vehicle) || !player.getVehicle().equals(vehicle)) {
            return Optional.empty();
        }
        
        try {
            // Используем рефлексию для вызова метода getSeatIndex из VehicleEntity
            Class<?> vehicleEntityClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            java.lang.reflect.Method getSeatIndexMethod = vehicleEntityClass.getMethod("getSeatIndex", Entity.class);
            Object result = getSeatIndexMethod.invoke(vehicle, player);
            if (result instanceof Integer) {
                int seatIndex = (Integer) result;
                if (seatIndex >= 0) {
                    return Optional.of(seatIndex);
                }
            }
        } catch (Exception e) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Не удалось получить индекс сиденья для игрока через рефлексию", e);
            }
        }
        
        // Fallback: пытаемся получить из NBT данных (как в SuperbWarfare)
        if (player.getPersistentData().contains(TAG_SEAT_INDEX, net.minecraft.nbt.Tag.TAG_INT)) {
            int seatIndex = player.getPersistentData().getInt(TAG_SEAT_INDEX);
            if (seatIndex >= 0) {
                return Optional.of(seatIndex);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Проверяет, соответствует ли ID техники шаблону из конфига.
     * Поддерживает форматы: "superbwarfare:vehicle_name", "vehicle_name"
     */
    private static boolean matchesVehicleId(String vehicleId, String pattern) {
        if (vehicleId == null || pattern == null) {
            return false;
        }
        
        // Нормализуем оба ID
        String normalizedVehicle = normalizeVehicleId(vehicleId);
        String normalizedPattern = normalizeVehicleId(pattern);
        
        // Точное совпадение
        return normalizedVehicle.equals(normalizedPattern);
    }
    
    /**
     * Нормализует ID техники, убирая префикс "superbwarfare:" если есть.
     */
    private static String normalizeVehicleId(String id) {
        if (id == null) {
            return "";
        }
        if (id.startsWith("superbwarfare:")) {
            return id.substring(15); // длина "superbwarfare:"
        }
        return id;
    }
}

