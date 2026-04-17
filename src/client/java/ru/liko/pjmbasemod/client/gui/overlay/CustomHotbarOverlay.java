package ru.liko.pjmbasemod.client.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Оверлей: хотбар вертикально справа по центру с плавным исчезновением
 * NeoForge 1.21.1: Use LayeredDraw.Layer instead of IGuiOverlay
 */
public final class CustomHotbarOverlay {
    
    private static long lastInteractionTime = System.currentTimeMillis();
    private static int lastSelectedSlot = -1;
    
    private static final long FADE_DELAY_MS = 3000;
    private static final long FADE_DURATION_MS = 1000;
    
    // NeoForge 1.21.1: LayeredDraw.Layer uses (graphics, deltaTracker) signature
    public static final LayeredDraw.Layer INSTANCE = (g, deltaTracker) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.hideGui) return;
        
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int slotSize = 16;
        int step = 20;
        int count = 9;
        int margin = 4;

        int currentSelected = mc.player.getInventory().selected;
        if (currentSelected != lastSelectedSlot) {
            lastInteractionTime = System.currentTimeMillis();
            lastSelectedSlot = currentSelected;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceInteraction = currentTime - lastInteractionTime;
        
        float alpha = 1.0f;
        if (timeSinceInteraction > FADE_DELAY_MS) {
            long fadeTime = timeSinceInteraction - FADE_DELAY_MS;
            alpha = 1.0f - Mth.clamp((float) fadeTime / FADE_DURATION_MS, 0.0f, 1.0f);
        }
        
        if (alpha <= 0.0f) return;

        g.pose().pushPose();
        try {
            // Общая высота колонки: 8*step + 16 = 176
            int totalH = 8 * step + slotSize;
            int x = sw - slotSize - margin;           // выравнивание к правому краю
            int y = (sh - totalH) / 2;                 // центр по вертикали

            int selected = mc.player.getInventory().selected;

            // Рисуем слоты hotbar вертикально
            for (int i = 0; i < count; i++) {
                int xi = x;
                int yi = y + i * step;

                // Применяем прозрачность к цветам
                int bgAlpha = (int) (alpha * 255);
                int bgColor = (i == selected) 
                    ? (bgAlpha << 24) | 0x80FFFFFF 
                    : (bgAlpha << 24) | 0x40000000;
                
                // Фон слота (полупрозрачный серый)
                g.fill(xi - 2, yi - 2, xi + slotSize + 2, yi + slotSize + 2, bgColor);

                ItemStack stack = mc.player.getInventory().items.get(i);
                
                // Рисуем предмет (без setColor чтобы не ломать другие overlay'и)
                g.renderItem(stack, xi, yi);
                g.renderItemDecorations(mc.font, stack, xi, yi);

                // Подсветка выбранного слота - яркая рамка с прозрачностью
                if (i == selected) {
                    int borderColor = (bgAlpha << 24) | 0xFFFFFF;
                    // Внешняя рамка
                    g.fill(xi - 2, yi - 2, xi + slotSize + 2, yi - 1, borderColor); // верх
                    g.fill(xi - 2, yi + slotSize + 1, xi + slotSize + 2, yi + slotSize + 2, borderColor); // низ
                    g.fill(xi - 2, yi - 1, xi - 1, yi + slotSize + 1, borderColor); // лево
                    g.fill(xi + slotSize + 1, yi - 1, xi + slotSize + 2, yi + slotSize + 1, borderColor); // право
                }
            }
        } finally {
            g.pose().popPose();
        }
    };
    
    /**
     * Обновляет время последнего взаимодействия (вызывается извне при взаимодействиях)
     */
    public static void updateInteractionTime() {
        lastInteractionTime = System.currentTimeMillis();
    }
}

