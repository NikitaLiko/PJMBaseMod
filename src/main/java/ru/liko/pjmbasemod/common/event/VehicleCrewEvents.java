package ru.liko.pjmbasemod.common.event;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.vehicle.VehicleCrewRestriction;

import java.util.Optional;

/**
 * Обработчик событий для ограничений посадки в технику SuperbWarfare по классу Экипаж.
 * Проверяет, может ли игрок сесть на определенное место в технике.
 * NeoForge 1.21.1: Updated event bus annotations
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class VehicleCrewEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_SEAT_INDEX = "SBWSeatIndex";
    
    private VehicleCrewEvents() {}
    
    /**
     * Перехватывает событие посадки в транспорт и проверяет класс игрока.
     * Используется высокий приоритет, чтобы перехватить событие до других модов.
     * 
     * ВАЖНО: В SuperbWarfare индекс сиденья определяется в методе addPassenger,
     * который вызывается ДО события EntityMountEvent. Поэтому мы используем
     * рефлексию для получения индекса из NBT данных игрока или через метод getSeatIndex.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityMount(EntityMountEvent event) {
        // Проверяем только на сервере
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // Проверяем, что это игрок садится в транспорт
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) {
            return;
        }
        
        Entity vehicle = event.getEntityBeingMounted();
        if (vehicle == null) {
            return;
        }
        
        // Проверяем, является ли это техникой SuperbWarfare
        if (!VehicleCrewRestriction.isSuperbWarfareVehicle(vehicle)) {
            return; // Не техника SuperbWarfare
        }
        
        // В SuperbWarfare метод addPassenger уже вызван и индекс сохранен в NBT
        // Определяем индекс сиденья через рефлексию или NBT
        int seatIndex = determineSeatIndex(player, vehicle);
        
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.debug("Игрок {} пытается сесть на место {} в технике {}", 
                player.getName().getString(), seatIndex, vehicle.getType());
        }
        
        // Проверяем, может ли игрок сесть на это место
        Optional<Component> errorMessage = VehicleCrewRestriction.canPlayerSitOnSeat(player, vehicle, seatIndex);
        
        if (errorMessage.isPresent()) {
            // Отменяем посадку
            event.setCanceled(true);
            
            // Выкидываем игрока из техники, если он уже был посажен
            if (player.getVehicle() == vehicle) {
                player.stopRiding();
            }
            
            // Отправляем сообщение игроку
            player.sendSystemMessage(errorMessage.get());
            
            LOGGER.info("Игрок {} не может сесть на место {} в технике {} - требуется класс Экипаж", 
                player.getName().getString(), seatIndex, vehicle.getType());
        }
    }
    
    /**
     * Определяет индекс сиденья, на которое пытается сесть игрок.
     * В SuperbWarfare индекс определяется в методе addPassenger и сохраняется в NBT.
     */
    private static int determineSeatIndex(ServerPlayer player, Entity vehicle) {
        // Сначала пытаемся получить индекс через рефлексию метода getSeatIndex
        // Это работает, если игрок уже посажен
        if (player.getVehicle() == vehicle) {
            try {
                Class<?> vehicleEntityClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
                java.lang.reflect.Method getSeatIndexMethod = vehicleEntityClass.getMethod("getSeatIndex", Entity.class);
                Object result = getSeatIndexMethod.invoke(vehicle, player);
                if (result instanceof Integer) {
                    int seatIndex = (Integer) result;
                    if (seatIndex >= 0) {
                        if (Config.isDebugLoggingEnabled()) {
                            LOGGER.debug("Индекс сиденья получен через getSeatIndex: {}", seatIndex);
                        }
                        return seatIndex;
                    }
                }
            } catch (Exception e) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.debug("Не удалось получить индекс через getSeatIndex", e);
                }
            }
        }
        
        // Пытаемся получить индекс из NBT данных игрока
        // В SuperbWarfare индекс сиденья сохраняется в NBT при посадке в методе addPassenger
        if (player.getPersistentData().contains(TAG_SEAT_INDEX, net.minecraft.nbt.Tag.TAG_INT)) {
            int seatIndex = player.getPersistentData().getInt(TAG_SEAT_INDEX);
            if (seatIndex >= 0) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.debug("Индекс сиденья получен из NBT: {}", seatIndex);
                }
                return seatIndex;
            }
        }
        
        // Если не удалось получить через рефлексию или NBT, используем рефлексию для получения orderedPassengers
        try {
            Class<?> vehicleEntityClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            java.lang.reflect.Field orderedPassengersField = vehicleEntityClass.getDeclaredField("orderedPassengers");
            orderedPassengersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Entity> orderedPassengers = (java.util.List<Entity>) orderedPassengersField.get(vehicle);
            
            if (orderedPassengers != null) {
                // Находим индекс игрока в списке
                int index = orderedPassengers.indexOf(player);
                if (index >= 0) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.debug("Индекс сиденья получен из orderedPassengers: {}", index);
                    }
                    return index;
                }
                
                // Если игрок еще не в списке, определяем по количеству занятых мест
                int seatIndex = 0;
                for (Entity passenger : orderedPassengers) {
                    if (passenger == null) {
                        break;
                    }
                    seatIndex++;
                }
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.debug("Индекс сиденья определен по количеству занятых мест: {}", seatIndex);
                }
                return seatIndex;
            }
        } catch (Exception e) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Не удалось получить индекс через orderedPassengers", e);
            }
        }
        
        // Fallback: определяем по количеству пассажиров
        int passengerCount = vehicle.getPassengers().size();
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.debug("Индекс сиденья определен по количеству пассажиров (fallback): {}", passengerCount);
        }
        return passengerCount;
    }
}

