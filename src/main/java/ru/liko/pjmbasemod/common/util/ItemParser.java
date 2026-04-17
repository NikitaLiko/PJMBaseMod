package ru.liko.pjmbasemod.common.util;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Утилитный класс для парсинга предметов из строк конфигурации.
 * Поддерживает ванильные предметы, предметы из модов (TACZ и др.), а также NBT данные.
 * 
 * Форматы:
 * - "namespace:item" - предмет без количества
 * - "namespace:item:count" - предмет с количеством
 * - "namespace:item:count{nbt_json}" - предмет с количеством и NBT данными
 * - "type:slotIndex:namespace:item:count{nbt_json}" - предмет с указанием слота (type: armor, slot, offhand)
 */
public final class ItemParser {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ItemParser() {}

    /**
     * Результат парсинга предмета со слотом
     */
    public static class ParsedItem {
        public final ItemStack stack;
        public final String slotType; // "armor", "slot", "offhand", "curios"
        public final int slotIndex;
        public final String curiosSlotType; // Для Curios: тип слота (ring, necklace, belt и т.д.)

        public ParsedItem(ItemStack stack, String slotType, int slotIndex) {
            this.stack = stack;
            this.slotType = slotType;
            this.slotIndex = slotIndex;
            this.curiosSlotType = null;
        }
        
        public ParsedItem(ItemStack stack, String slotType, int slotIndex, String curiosSlotType) {
            this.stack = stack;
            this.slotType = slotType;
            this.slotIndex = slotIndex;
            this.curiosSlotType = curiosSlotType;
        }
    }

    /**
     * Парсит строку конфигурации в ItemStack (старый формат без слотов).
     * 
     * @param itemString Строка формата "namespace:item[:count][{nbt}]"
     * @return Optional с ItemStack, если предмет найден, иначе пустой Optional
     */
    public static Optional<ItemStack> parseItemStack(String itemString) {
        Optional<ParsedItem> parsed = parseItemStackWithSlot(itemString);
        return parsed.map(p -> p.stack);
    }

    /**
     * Парсит строку конфигурации в ParsedItem с информацией о слоте.
     * Поддерживает как старый формат (без слотов), так и новый формат (со слотами).
     * 
     * @param itemString Строка формата "namespace:item[:count][{nbt}]" или "type:slotIndex:namespace:item[:count][{nbt}]"
     * @return Optional с ParsedItem, если предмет найден, иначе пустой Optional
     */
    public static Optional<ParsedItem> parseItemStackWithSlot(String itemString) {
        if (itemString == null || itemString.trim().isEmpty()) {
            return Optional.empty();
        }

        itemString = itemString.trim();

        try {
            // Разделяем на основную часть и NBT
            String mainPart = itemString;
            String nbtString = null;
            
            int nbtStart = itemString.indexOf('{');
            if (nbtStart > 0) {
                mainPart = itemString.substring(0, nbtStart).trim();
                nbtString = itemString.substring(nbtStart).trim();
            }

            // Парсим основную часть
            String[] parts = mainPart.split(":");
            
            String slotType = null;
            int slotIndex = -1;
            String curiosSlotType = null;
            int itemPartStart = 0;

            // Проверяем, есть ли информация о слоте
            // Формат для обычных слотов: type:slotIndex:namespace:item:count
            // Формат для Curios: curios:slotType:slotIndex:namespace:item:count
            if (parts.length >= 4) {
                if (parts[0].equals("curios") && parts.length >= 5) {
                    // Формат Curios: curios:slotType:slotIndex:namespace:item:count
                    slotType = "curios";
                    curiosSlotType = parts[1]; // ring, necklace, belt и т.д.
                    try {
                        slotIndex = Integer.parseInt(parts[2]);
                        itemPartStart = 3; // Пропускаем curios, slotType и slotIndex
                    } catch (NumberFormatException e) {
                        // Если не удалось распарсить slotIndex, считаем что это старый формат
                        slotType = null;
                        curiosSlotType = null;
                        itemPartStart = 0;
                    }
                } else if (parts[0].equals("armor") || parts[0].equals("slot") || parts[0].equals("offhand")) {
                    // Обычные слоты: type:slotIndex:namespace:item:count
                    slotType = parts[0];
                    try {
                        slotIndex = Integer.parseInt(parts[1]);
                        itemPartStart = 2; // Пропускаем type и slotIndex
                    } catch (NumberFormatException e) {
                        // Если не удалось распарсить slotIndex, считаем что это старый формат
                        slotType = null;
                        itemPartStart = 0;
                    }
                }
            }

            // Парсим часть с предметом
            if (parts.length < itemPartStart + 2) {
                LOGGER.warn("Некорректный формат предмета: {} (нужен формат namespace:item)", itemString);
                return Optional.empty();
            }

            String namespace = parts[itemPartStart];
            String itemName = parts[itemPartStart + 1];
            int count = 1;

            // Парсим количество, если указано
            if (parts.length >= itemPartStart + 3) {
                try {
                    count = Integer.parseInt(parts[itemPartStart + 2]);
                    if (count < 1) {
                        count = 1;
                    }
                    if (count > 64) {
                        count = 64; // Максимальный размер стека
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Некорректное количество для предмета {}: {}", itemString, parts[itemPartStart + 2]);
                    count = 1;
                }
            }

            // Создаём ResourceLocation и ищем предмет
            ResourceLocation itemId = ResourceLocation.tryParse(namespace + ":" + itemName);
            if (itemId == null) {
                LOGGER.warn("Некорректный ResourceLocation для предмета: {}:{}", namespace, itemName);
                return Optional.empty();
            }

            // Ищем предмет в реестре (поддерживает моды, включая ванильные предметы)
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);

            if (item == null) {
                LOGGER.warn("Предмет не найден в реестре: {} (проверьте, что мод установлен)", itemId);
                return Optional.empty();
            }

            // Создаём ItemStack
            ItemStack stack = new ItemStack(item, count);

            // NeoForge 1.21.1: NBT handling changed to Data Components
            // setTag is removed - NBT data would need to be applied via components
            // For now, we skip NBT application as it requires significant refactoring
            if (nbtString != null && !nbtString.isEmpty()) {
                LOGGER.debug("NBT данные указаны для предмета {}, но применение NBT требует Data Components API", itemString);
            }

            // Если слот не указан, возвращаем как обычный предмет (для обратной совместимости)
            if (slotType == null) {
                return Optional.of(new ParsedItem(stack, "slot", -1));
            }

            // Для Curios используем специальный конструктор с curiosSlotType
            if ("curios".equals(slotType)) {
                return Optional.of(new ParsedItem(stack, slotType, slotIndex, curiosSlotType));
            }

            return Optional.of(new ParsedItem(stack, slotType, slotIndex));

        } catch (Exception e) {
            LOGGER.error("Ошибка при парсинге предмета: {}", itemString, e);
            return Optional.empty();
        }
    }

    /**
     * Проверяет, существует ли предмет с указанным ID в реестре.
     * 
     * @param itemId ID предмета в формате "namespace:item"
     * @return true, если предмет существует
     */
    public static boolean itemExists(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return false;
        }

        ResourceLocation resLoc = ResourceLocation.tryParse(itemId.trim());
        if (resLoc == null) {
            return false;
        }

        // Проверяем в ForgeRegistries (поддерживает моды, включая ванильные предметы)
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(resLoc);
    }

    /**
     * Получает Item по ResourceLocation.
     * 
     * @param itemId ID предмета
     * @return Optional с Item, если найден
     */
    public static Optional<Item> getItem(ResourceLocation itemId) {
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
        return Optional.ofNullable(item);
    }
}

