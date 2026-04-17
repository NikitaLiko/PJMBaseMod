package ru.liko.pjmbasemod.common.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Утилитный класс для работы с Curios API.
 * Предоставляет безопасные методы для работы со слотами Curios,
 * которые работают только если мод Curios установлен.
 */
public class CuriosHelper {
    private static final String CURIOS_MOD_ID = "curios";
    
    /**
     * Проверяет, установлен ли мод Curios API
     * @return true, если Curios API установлен
     */
    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }
    
    /**
     * Получает хендлер Curios для указанного игрока
     * @param entity Сущность (игрок)
     * @return Optional с хендлером Curios (как Object), или пустой Optional если Curios не установлен
     */
    public static Optional<Object> getCuriosHandler(LivingEntity entity) {
        if (!isCuriosLoaded()) {
            return Optional.empty();
        }
        
        try {
            // Используем рефлексию для безопасного доступа к Curios API
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object curiosHelper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);
            Object lazyOptional = curiosHelper.getClass().getMethod("getCuriosHandler", LivingEntity.class).invoke(curiosHelper, entity);
            
            // Проверяем, есть ли значение в LazyOptional
            boolean isPresent = (Boolean) lazyOptional.getClass().getMethod("isPresent").invoke(lazyOptional);
            if (isPresent) {
                // resolve() возвращает Optional<T>, нужно извлечь значение
                Object resolvedOptional = lazyOptional.getClass().getMethod("resolve").invoke(lazyOptional);
                if (resolvedOptional instanceof Optional<?> opt && opt.isPresent()) {
                    return Optional.of(opt.get());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Получает предмет из указанного слота Curios
     * @param entity Сущность (игрок)
     * @param slotType Тип слота (например, "ring", "necklace", "belt")
     * @param slotIndex Индекс слота (обычно 0)
     * @return ItemStack из слота, или пустой ItemStack если слот пуст или Curios не установлен
     */
    public static ItemStack getCuriosStack(LivingEntity entity, String slotType, int slotIndex) {
        if (!isCuriosLoaded()) {
            return ItemStack.EMPTY;
        }
        
        Optional<Object> handlerOpt = getCuriosHandler(entity);
        if (handlerOpt.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        try {
            Object handler = handlerOpt.get();
            Object stackHandlerOpt = handler.getClass().getMethod("getStacksHandler", String.class).invoke(handler, slotType);
            
            if (stackHandlerOpt instanceof Optional) {
                Optional<?> opt = (Optional<?>) stackHandlerOpt;
                if (opt.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                
                Object stackHandler = opt.get();
                // Получаем IDynamicStackHandler через getStacks()
                Object stacks = stackHandler.getClass().getMethod("getStacks").invoke(stackHandler);
                int slotCount = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
                if (slotIndex < 0 || slotIndex >= slotCount) {
                    return ItemStack.EMPTY;
                }
                
                return (ItemStack) stacks.getClass().getMethod("getStackInSlot", int.class).invoke(stacks, slotIndex);
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Устанавливает предмет в указанный слот Curios
     * @param entity Сущность (игрок)
     * @param slotType Тип слота (например, "ring", "necklace", "belt")
     * @param slotIndex Индекс слота (обычно 0)
     * @param stack Предмет для установки
     * @return true, если предмет успешно установлен, false в противном случае
     */
    public static boolean setCuriosStack(LivingEntity entity, String slotType, int slotIndex, ItemStack stack) {
        if (!isCuriosLoaded()) {
            return false;
        }
        
        Optional<Object> handlerOpt = getCuriosHandler(entity);
        if (handlerOpt.isEmpty()) {
            return false;
        }
        
        try {
            Object handler = handlerOpt.get();
            Object stackHandlerOpt = handler.getClass().getMethod("getStacksHandler", String.class).invoke(handler, slotType);
            
            if (stackHandlerOpt instanceof Optional) {
                Optional<?> opt = (Optional<?>) stackHandlerOpt;
                if (opt.isEmpty()) {
                    return false;
                }
                
                Object stackHandler = opt.get();
                // Получаем IDynamicStackHandler через getStacks()
                Object stacks = stackHandler.getClass().getMethod("getStacks").invoke(stackHandler);
                int slotCount = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
                if (slotIndex < 0 || slotIndex >= slotCount) {
                    return false;
                }
                
                stacks.getClass().getMethod("setStackInSlot", int.class, ItemStack.class).invoke(stacks, slotIndex, stack);
                return true;
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        
        return false;
    }
    
    /**
     * Проверяет, может ли предмет быть помещен в указанный слот Curios
     * @param entity Сущность (игрок)
     * @param slotType Тип слота
     * @param slotIndex Индекс слота
     * @param stack Предмет для проверки
     * @return true, если предмет может быть помещен в слот
     */
    public static boolean canEquipInCuriosSlot(LivingEntity entity, String slotType, int slotIndex, ItemStack stack) {
        if (!isCuriosLoaded()) {
            return false;
        }
        
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object curiosHelper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);
            Class<?> slotContextClass = Class.forName("top.theillusivec4.curios.api.SlotContext");
            Object slotContext = slotContextClass.getConstructor(String.class, LivingEntity.class, int.class)
                .newInstance(slotType, entity, slotIndex);
            
            return (Boolean) curiosHelper.getClass().getMethod("isStackValid", slotContextClass, ItemStack.class)
                .invoke(curiosHelper, slotContext, stack);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Получает количество доступных слотов указанного типа
     * @param entity Сущность (игрок)
     * @param slotType Тип слота
     * @return Количество доступных слотов, или 0 если Curios не установлен
     */
    public static int getCuriosSlotCount(LivingEntity entity, String slotType) {
        if (!isCuriosLoaded()) {
            return 0;
        }
        
        Optional<Object> handlerOpt = getCuriosHandler(entity);
        if (handlerOpt.isEmpty()) {
            return 0;
        }
        
        try {
            Object handler = handlerOpt.get();
            Object stackHandlerOpt = handler.getClass().getMethod("getStacksHandler", String.class).invoke(handler, slotType);
            
            if (stackHandlerOpt instanceof Optional) {
                Optional<?> opt = (Optional<?>) stackHandlerOpt;
                if (opt.isEmpty()) {
                    return 0;
                }
                
                Object stackHandler = opt.get();
                // Получаем IDynamicStackHandler через getStacks()
                Object stacks = stackHandler.getClass().getMethod("getStacks").invoke(stackHandler);
                return (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        
        return 0;
    }
    
    /**
     * Очищает все слоты Curios у сущности
     * @param entity Сущность (игрок)
     */
    public static void clearAllCuriosSlots(LivingEntity entity) {
        if (!isCuriosLoaded()) {
            return;
        }
        
        Optional<Object> handlerOpt = getCuriosHandler(entity);
        if (handlerOpt.isEmpty()) {
            return;
        }
        
        try {
            Object handler = handlerOpt.get();
            // Получаем все типы слотов
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
                        java.lang.reflect.Method setStackMethod = stacks.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
                        for (int i = 0; i < slotCount; i++) {
                            setStackMethod.invoke(stacks, i, ItemStack.EMPTY);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    /**
     * Получает все предметы из всех слотов указанного типа
     * @param entity Сущность (игрок)
     * @param slotType Тип слота
     * @return Массив ItemStack из всех слотов указанного типа
     */
    public static ItemStack[] getAllCuriosStacks(LivingEntity entity, String slotType) {
        if (!isCuriosLoaded()) {
            return new ItemStack[0];
        }
        
        Optional<Object> handlerOpt = getCuriosHandler(entity);
        if (handlerOpt.isEmpty()) {
            return new ItemStack[0];
        }
        
        try {
            Object handler = handlerOpt.get();
            Object stackHandlerOpt = handler.getClass().getMethod("getStacksHandler", String.class).invoke(handler, slotType);
            
            if (stackHandlerOpt instanceof Optional) {
                Optional<?> opt = (Optional<?>) stackHandlerOpt;
                if (opt.isEmpty()) {
                    return new ItemStack[0];
                }
                
                Object stackHandler = opt.get();
                // Получаем IDynamicStackHandler через getStacks()
                Object dynamicHandler = stackHandler.getClass().getMethod("getStacks").invoke(stackHandler);
                int slotCount = (Integer) dynamicHandler.getClass().getMethod("getSlots").invoke(dynamicHandler);
                ItemStack[] stacks = new ItemStack[slotCount];
                
                for (int i = 0; i < slotCount; i++) {
                    stacks[i] = (ItemStack) dynamicHandler.getClass().getMethod("getStackInSlot", int.class).invoke(dynamicHandler, i);
                }
                
                return stacks;
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        
        return new ItemStack[0];
    }
}

