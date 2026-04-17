package ru.liko.pjmbasemod.common.util;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.common.KitsConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилитный класс для управления предметами классов через команды.
 * Использует KitsConfig для хранения китов в внешнем JSON файле.
 */
public final class ClassItemManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClassItemManager() {}

    /**
     * Добавляет предмет в комплект класса
     * 
     * @param classId ID класса (assault, machine_gunner, etc.)
     * @param teamName Название команды
     * @param itemStack Предмет для добавления
     * @param count Количество (если null, используется количество из ItemStack)
     * @return true, если предмет успешно добавлен
     */
    public static boolean addItemToClass(String classId, String teamName, ItemStack itemStack, Integer count) {
        if (itemStack == null || itemStack.isEmpty()) {
            LOGGER.warn("Попытка добавить пустой предмет в класс {} для команды {}", classId, teamName);
            return false;
        }

        // Формируем строку предмета
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId == null) {
            LOGGER.error("Не удалось получить ID предмета: {}", itemStack.getItem());
            return false;
        }

        int itemCount = count != null ? count : itemStack.getCount();
        String itemString = itemId.toString();
        
        // Добавляем количество, если оно отличается от 1 или от максимального стека
        if (itemCount > 1 || itemCount != itemStack.getMaxStackSize()) {
            itemString += ":" + itemCount;
        }
        
        // Добавляем NBT данные, если они есть
        net.minecraft.nbt.Tag nbtTag = itemStack.saveOptional(net.minecraft.core.RegistryAccess.EMPTY);
        CompoundTag nbt = nbtTag instanceof CompoundTag ? (CompoundTag) nbtTag : null;
        if (nbt != null && !nbt.isEmpty()) {
            String nbtString = nbt.toString();
            itemString += nbtString;
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Добавлен предмет с NBT данными: {} -> {}", itemId, nbtString);
            }
        }

        // Добавляем в KitsConfig
        KitsConfig.addItemToKit(classId, teamName, itemString);
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("Добавлен предмет {} в класс {} для команды {}", itemString, classId, teamName);
        }
        return true;
    }

    /**
     * Удаляет предмет из комплекта класса
     * 
     * @param classId ID класса
     * @param teamName Название команды
     * @param itemString Строка предмета для удаления (или часть строки)
     * @return true, если предмет успешно удален
     */
    public static boolean removeItemFromClass(String classId, String teamName, String itemString) {
        boolean removed = KitsConfig.removeItemFromKit(classId, teamName, itemString);
        
        if (removed) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("Удален предмет {} из класса {} для команды {}", itemString, classId, teamName);
            }
            return true;
        } else {
            LOGGER.warn("Предмет {} не найден в списке класса {} для команды {}", itemString, classId, teamName);
            return false;
        }
    }

    /**
     * Получает список предметов класса.
     * Делегирует в KitsConfig.
     */
    public static List<String> getClassItems(String classId, String teamName) {
        return KitsConfig.getKitItemStrings(classId, teamName);
    }

    /**
     * Сохраняет весь инвентарь игрока как именованный комплект класса (вариацию)
     * 
     * @param classId ID класса
     * @param teamName Название команды
     * @param kitId ID кита (уникальный для класса/команды)
     * @param displayName Отображаемое имя кита
     * @param minRank Минимальное звание для доступа
     * @param player Игрок
     */
    public static boolean saveInventoryAsKitDefinition(String classId, String teamName, String kitId, String displayName, ru.liko.pjmbasemod.common.player.PjmRank minRank, net.minecraft.server.level.ServerPlayer player) {
        List<String> savedItems = new ArrayList<>();
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();

        // Сначала сохраняем броню (слоты 0-3: ботинки, поножи, нагрудник, шлем)
        for (int i = 0; i < 4; i++) {
            ItemStack armorStack = inventory.armor.get(i);
            if (!armorStack.isEmpty()) {
                String itemString = formatItemStringWithSlot("armor", i, armorStack);
                savedItems.add(itemString);
            }
        }

        // Затем сохраняем предметы в горячей панели (слоты 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty()) {
                String itemString = formatItemStringWithSlot("slot", i, stack);
                savedItems.add(itemString);
            }
        }

        // Затем сохраняем остальные предметы инвентаря (слоты 9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty()) {
                String itemString = formatItemStringWithSlot("slot", i, stack);
                savedItems.add(itemString);
            }
        }

        // Сохраняем предмет в руке (offhand)
        ItemStack offhandStack = inventory.offhand.get(0);
        if (!offhandStack.isEmpty()) {
            String itemString = formatItemStringWithSlot("offhand", 0, offhandStack);
            savedItems.add(itemString);
        }

        // Сохраняем предметы из слотов Curios (если Curios установлен)
        if (ru.liko.pjmbasemod.common.util.CuriosHelper.isCuriosLoaded()) {
             // ... (existing logic for curios - omitted for brevity, keeping it simple or reusing helper if possible, 
             // but since I'm overwriting, I should probably copy the logic or extract it. 
             // To be safe and avoid code duplication I'll just copy the block from the existing method since I can't easily extract it right now without more edits)
            java.util.Optional<?> handlerOpt = ru.liko.pjmbasemod.common.util.CuriosHelper.getCuriosHandler(player);
            if (handlerOpt.isPresent()) {
                Object handler = handlerOpt.get();
                try {
                    java.lang.reflect.Method getCuriosMethod = handler.getClass().getMethod("getCurios");
                    Object curiosMap = getCuriosMethod.invoke(handler);
                    if (curiosMap instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, ?> curios = (java.util.Map<String, ?>) curiosMap;
                        for (String slotType : curios.keySet()) {
                            java.lang.reflect.Method getStacksHandlerMethod = handler.getClass().getMethod("getStacksHandler", String.class);
                            java.util.Optional<?> stackHandlerOpt = (java.util.Optional<?>) getStacksHandlerMethod.invoke(handler, slotType);
                            if (stackHandlerOpt.isPresent()) {
                                Object stackHandler = stackHandlerOpt.get();
                                Object stacks = stackHandler.getClass().getMethod("getStacks").invoke(stackHandler);
                                java.lang.reflect.Method getSlotsMethod = stacks.getClass().getMethod("getSlots");
                                int slotCount = (Integer) getSlotsMethod.invoke(stacks);
                                for (int i = 0; i < slotCount; i++) {
                                    java.lang.reflect.Method getStackInSlotMethod = stacks.getClass().getMethod("getStackInSlot", int.class);
                                    ItemStack curiosStack = (ItemStack) getStackInSlotMethod.invoke(stacks, i);
                                    if (!curiosStack.isEmpty()) {
                                        String itemString = formatItemStringWithCuriosSlot(slotType, i, curiosStack);
                                        savedItems.add(itemString);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Ошибка при сохранении предметов из слотов Curios: {}", e.getMessage());
                }
            }
        }

        // Создаем и сохраняем KitDefinition
        ru.liko.pjmbasemod.common.KitDefinition kit = new ru.liko.pjmbasemod.common.KitDefinition(
            kitId, displayName, minRank, savedItems
        );
        KitsConfig.saveKit(classId, teamName, kit);
        
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("Сохранен комплект {} ({}) для {} {}: {} предметов", kitId, displayName, classId, teamName, savedItems.size());
        }
        return true;
    }

    /**
     * Сохраняет весь инвентарь игрока как комплект класса
     * Сохраняет позиции всех предметов, включая броню
     * 
     * @param classId ID класса (assault, machine_gunner, etc.)
     * @param teamName Название команды
     * @param player Игрок, инвентарь которого нужно сохранить
     * @return true, если инвентарь успешно сохранен
     */
    public static boolean saveInventoryAsKit(String classId, String teamName, net.minecraft.server.level.ServerPlayer player) {
        List<String> savedItems = new ArrayList<>();
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();

        // Сначала сохраняем броню (слоты 0-3: ботинки, поножи, нагрудник, шлем)
        for (int i = 0; i < 4; i++) {
            ItemStack armorStack = inventory.armor.get(i);
            if (!armorStack.isEmpty()) {
                String itemString = formatItemStringWithSlot("armor", i, armorStack);
                savedItems.add(itemString);
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.debug("Сохранена броня в слот {}: {}", i, itemString);
                }
            }
        }

        // Затем сохраняем предметы в горячей панели (слоты 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty()) {
                String itemString = formatItemStringWithSlot("slot", i, stack);
                savedItems.add(itemString);
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.debug("Сохранен предмет в слот {}: {}", i, itemString);
                }
            }
        }

        // Затем сохраняем остальные предметы инвентаря (слоты 9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty()) {
                String itemString = formatItemStringWithSlot("slot", i, stack);
                savedItems.add(itemString);
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.debug("Сохранен предмет в слот {}: {}", i, itemString);
                }
            }
        }

        // Сохраняем предмет в руке (offhand)
        ItemStack offhandStack = inventory.offhand.get(0);
        if (!offhandStack.isEmpty()) {
            String itemString = formatItemStringWithSlot("offhand", 0, offhandStack);
            savedItems.add(itemString);
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.debug("Сохранен предмет в offhand: {}", itemString);
            }
        }

        // Сохраняем предметы из слотов Curios (если Curios установлен)
        if (ru.liko.pjmbasemod.common.util.CuriosHelper.isCuriosLoaded()) {
            java.util.Optional<?> handlerOpt = ru.liko.pjmbasemod.common.util.CuriosHelper.getCuriosHandler(player);
            if (handlerOpt.isPresent()) {
                Object handler = handlerOpt.get();
                try {
                    // Используем рефлексию для безопасного доступа к методам Curios API
                    java.lang.reflect.Method getCuriosMethod = handler.getClass().getMethod("getCurios");
                    Object curiosMap = getCuriosMethod.invoke(handler);
                    if (curiosMap instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, ?> curios = (java.util.Map<String, ?>) curiosMap;
                        for (String slotType : curios.keySet()) {
                            java.lang.reflect.Method getStacksHandlerMethod = handler.getClass().getMethod("getStacksHandler", String.class);
                            java.util.Optional<?> stackHandlerOpt = (java.util.Optional<?>) getStacksHandlerMethod.invoke(handler, slotType);
                            if (stackHandlerOpt.isPresent()) {
                                Object stackHandler = stackHandlerOpt.get();
                                // Получаем IDynamicStackHandler через getStacks()
                                Object stacks = stackHandler.getClass().getMethod("getStacks").invoke(stackHandler);
                                java.lang.reflect.Method getSlotsMethod = stacks.getClass().getMethod("getSlots");
                                int slotCount = (Integer) getSlotsMethod.invoke(stacks);
                                for (int i = 0; i < slotCount; i++) {
                                    java.lang.reflect.Method getStackInSlotMethod = stacks.getClass().getMethod("getStackInSlot", int.class);
                                    ItemStack curiosStack = (ItemStack) getStackInSlotMethod.invoke(stacks, i);
                                    if (!curiosStack.isEmpty()) {
                                        String itemString = formatItemStringWithCuriosSlot(slotType, i, curiosStack);
                                        savedItems.add(itemString);
                                        if (Config.isDebugLoggingEnabled()) {
                                            LOGGER.debug("Сохранен предмет в Curios слот {}:{}: {}", slotType, i, itemString);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Ошибка при сохранении предметов из слотов Curios: {}", e.getMessage());
                }
            }
        }

        // Сохраняем в KitsConfig
        KitsConfig.setKit(classId, teamName, savedItems);
        
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("Сохранен комплект класса {} для команды {}: {} предметов", classId, teamName, savedItems.size());
        }
        return true;
    }

    /**
     * Форматирует строку предмета с информацией о слоте
     * Формат: "type:slotIndex:itemId:count{nbt}"
     */
    private static String formatItemStringWithSlot(String slotType, int slotIndex, ItemStack itemStack) {
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId == null) {
            LOGGER.error("Не удалось получить ID предмета: {}", itemStack.getItem());
            return "";
        }

        StringBuilder itemString = new StringBuilder();
        itemString.append(slotType).append(":").append(slotIndex).append(":");
        itemString.append(itemId.toString());

        int count = itemStack.getCount();
        if (count > 1 || count != itemStack.getMaxStackSize()) {
            itemString.append(":").append(count);
        }

        // Добавляем NBT данные, если они есть
        net.minecraft.nbt.Tag nbtTag = itemStack.saveOptional(net.minecraft.core.RegistryAccess.EMPTY);
        CompoundTag nbt = nbtTag instanceof CompoundTag ? (CompoundTag) nbtTag : null;
        if (nbt != null && !nbt.isEmpty()) {
            itemString.append(nbt.toString());
        }

        return itemString.toString();
    }
    
    /**
     * Форматирует строку предмета для слота Curios
     * Формат: "curios:slotType:slotIndex:itemId:count{nbt}"
     */
    private static String formatItemStringWithCuriosSlot(String curiosSlotType, int slotIndex, ItemStack itemStack) {
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId == null) {
            LOGGER.error("Не удалось получить ID предмета: {}", itemStack.getItem());
            return "";
        }

        StringBuilder itemString = new StringBuilder();
        itemString.append("curios:").append(curiosSlotType).append(":").append(slotIndex).append(":");
        itemString.append(itemId.toString());

        int count = itemStack.getCount();
        if (count > 1 || count != itemStack.getMaxStackSize()) {
            itemString.append(":").append(count);
        }

        // Добавляем NBT данные, если они есть
        net.minecraft.nbt.Tag nbtTag = itemStack.saveOptional(net.minecraft.core.RegistryAccess.EMPTY);
        CompoundTag nbt = nbtTag instanceof CompoundTag ? (CompoundTag) nbtTag : null;
        if (nbt != null && !nbt.isEmpty()) {
            itemString.append(nbt.toString());
        }

        return itemString.toString();
    }
}

