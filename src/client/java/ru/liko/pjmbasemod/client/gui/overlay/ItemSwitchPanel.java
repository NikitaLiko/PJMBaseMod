package ru.liko.pjmbasemod.client.gui.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ru.liko.pjmbasemod.Config;

/**
 * Боковая панель для отображения специальных слотов при переключении предметов
 * Появляется справа от экрана при нажатии клавиш 1-4
 */
@OnlyIn(Dist.CLIENT)
public class ItemSwitchPanel {
    
    // Цвета в стиле Squad
    private static final int COLOR_BACKGROUND = 0xC0000000; // Полупрозрачный черный фон
    private static final int COLOR_BORDER = 0xFFFFFFFF; // Белая рамка
    private static final int COLOR_TEXT = 0xFFFFFFFF; // Белый текст
    private static final int COLOR_SELECTED = 0xFF2E86DE; // Синий цвет для выбранного слота
    private static final int COLOR_KEY = 0xFF00FF00; // Зеленый цвет для клавиш
    
    // Параметры отрисовки
    private static final int PANEL_WIDTH = 80;
    private static final int SLOT_HEIGHT = 60;
    private static final int PADDING = 8;
    private static final int NUM_SLOTS = 4;
    
    // Анимация
    private static float animationProgress = 0.0f;
    private static boolean isVisible = false;
    private static long showTime = 0;
    private static int selectedSlot = -1;
    
    // Время отображения в миллисекундах (из конфигурации)
    private static final long FADE_OUT_DURATION = 300; // 300ms анимация исчезновения
    
    private static long getDisplayDuration() {
        return Config.getItemSwitchDisplayTime();
    }
    
    /**
     * Показать панель с выбранным слотом
     */
    public static void show(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= NUM_SLOTS) {
            return;
        }
        
        isVisible = true;
        selectedSlot = slotIndex;
        showTime = System.currentTimeMillis();
        animationProgress = 0.0f;
    }
    
    /**
     * Скрыть панель
     */
    public static void hide() {
        isVisible = false;
        selectedSlot = -1;
    }
    
    /**
     * Обновить состояние анимации
     */
    public static void update() {
        if (!isVisible) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - showTime;
        long displayDuration = getDisplayDuration();
        
        // Проверяем, нужно ли скрыть панель
        if (elapsed >= displayDuration) {
            // Запускаем анимацию исчезновения
            long fadeOutElapsed = elapsed - displayDuration;
            if (fadeOutElapsed >= FADE_OUT_DURATION) {
                hide();
                return;
            }
            
            // Интерполируем прозрачность для исчезновения
            animationProgress = 1.0f - (fadeOutElapsed / (float) FADE_OUT_DURATION);
        } else {
            // Анимация появления
            if (animationProgress < 1.0f) {
                animationProgress = Math.min(1.0f, elapsed / 200.0f); // 200ms анимация появления
            }
        }
    }
    
    /**
     * Рендерит панель переключения предметов
     */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight, ItemStack[] slots) {
        if (!isVisible) {
            return;
        }
        
        update();
        
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        
        // Вычисляем позицию панели
        int panelHeight = NUM_SLOTS * SLOT_HEIGHT + (NUM_SLOTS - 1) * 4 + PADDING * 2;
        int panelX = screenWidth - PANEL_WIDTH - 10;
        int panelY = screenHeight / 2 - panelHeight / 2;
        
        // Применяем прозрачность через альфа-канал
        int alpha = (int) (animationProgress * 255);
        
        // Рендерим панель с предметами
        renderPanel(graphics, font, panelX, panelY, PANEL_WIDTH, panelHeight, slots, alpha);
    }
    
    /**
     * Рендерит панель со слотами
     */
    private static void renderPanel(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                     ItemStack[] slots, int alpha) {
        // Рендерим фон с прозрачностью
        int bgColor = (alpha << 24) | (COLOR_BACKGROUND & 0x00FFFFFF);
        graphics.fill(x, y, x + width, y + height, bgColor);
        
        // Рендерим рамку
        int borderColor = (alpha << 24) | (COLOR_BORDER & 0x00FFFFFF);
        renderBorder(graphics, x, y, width, height, borderColor);
        
        // Рендерим слоты
        int slotY = y + PADDING;
        for (int i = 0; i < NUM_SLOTS; i++) {
            ItemStack slot = i < slots.length ? slots[i] : ItemStack.EMPTY;
            renderSlot(graphics, font, x + PADDING, slotY, PANEL_WIDTH - PADDING * 2, 
                       SLOT_HEIGHT, i + 1, slot, i == selectedSlot, alpha);
            slotY += SLOT_HEIGHT + 4;
        }
    }
    
    /**
     * Рендерит один слот
     */
    private static void renderSlot(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                    int slotNumber, ItemStack item, boolean isSelected, int alpha) {
        // Рендерим выделение если слот выбран
        if (isSelected) {
            int selectedColor = (alpha << 24) | (COLOR_SELECTED & 0x00FFFFFF);
            graphics.fill(x - 4, y - 4, x + width + 4, y + height + 4, 0x60000000); // Внешнее выделение
            graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, selectedColor); // Синяя рамка
        }
        
        // Рендерим фоновый прямоугольник слота
        int bgColor = (alpha << 24) | 0x40404040;
        graphics.fill(x, y, x + width, y + height, bgColor);
        
        // Рендерим рамку слота
        int borderColor = (alpha << 24) | (COLOR_BORDER & 0x00FFFFFF);
        renderBorder(graphics, x, y, width, height, borderColor);
        
        // Рендерим клавишу
        String keyText = String.valueOf(slotNumber);
        int keyColor = (alpha << 24) | (COLOR_KEY & 0x00FFFFFF);
        graphics.drawString(font, keyText, x + 4, y + 4, keyColor, false);
        
        // Рендерим иконку предмета
        if (!item.isEmpty()) {
            graphics.renderItem(item, x + width / 2 - 8, y + 20, 0);
            graphics.renderItemDecorations(font, item, x + width / 2 - 8, y + 20);
            
            // Рендерим название предмета
            String itemName = item.getHoverName().getString();
            if (font.width(itemName) > width - 8) {
                itemName = font.plainSubstrByWidth(itemName, width - 11) + "...";
            }
            int textColor = (alpha << 24) | (COLOR_TEXT & 0x00FFFFFF);
            graphics.drawCenteredString(font, itemName, x + width / 2, y + height - 12, textColor);
        }
    }
    
    /**
     * Рендерит рамку
     */
    private static void renderBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color); // Верх
        graphics.fill(x, y + height - 1, x + width, y + height, color); // Низ
        graphics.fill(x, y, x + 1, y + height, color); // Лево
        graphics.fill(x + width - 1, y, x + width, y + height, color); // Право
    }
    
    /**
     * Проверяет, видна ли панель
     */
    public static boolean isVisible() {
        return isVisible;
    }
}

