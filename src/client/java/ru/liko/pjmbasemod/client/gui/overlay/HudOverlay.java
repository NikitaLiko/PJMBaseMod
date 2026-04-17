package ru.liko.pjmbasemod.client.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.Config;

/**
 * NeoForge 1.21.1: Updated to use LayeredDraw.Layer and RenderGuiLayerEvent
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HudOverlay {

    // Современная милитари-палитра (tactical/HUD style)
    private static final int TICK_MAIN = 0xFF6B705C; // крупные деления (olive drab темный)
    private static final int TICK_SUB = 0xFF8B9080; // мелкие деления (приглушённый olive)
    private static final int DIR_MAIN = 0xFFCED4B3; // N E S W (tactical tan/desert sand)
    private static final int DIR_SUB = 0xFF9FA590; // NE SE SW NW (sage green)
    private static final int NORTH_CLR = 0xFFFF4444; // выделенный «N» (тактический красный NATO)
    private static final int POINTER_CLR = 0xFF00FF00; // линия + стрелка (ярко-зелёный HUD)
    private static final int DEG_CLR = 0xFFB8C5B0; // цифры (приглушённый светлый)

    // Геометрия
    private static final int WIDTH = 300;
    private static final int HEIGHT = 25;
    private static final int BOTTOM_MARGIN = 10; // отступ от нижнего края экрана

    private static float smYaw;
    private static long last;

    // Состояние нахождения в зоне выбора классов
    private static boolean inZone = false;
    private static String team1Name = "";
    private static String team2Name = "";
    private static int team1Balance = 0;
    private static int team2Balance = 0;

    /**
     * Overlay для компаса - рендерится независимо от ванильного hotbar
     */
    // NeoForge 1.21.1: LayeredDraw.Layer uses (graphics, deltaTracker) signature
    public static final LayeredDraw.Layer COMPASS_OVERLAY = (g, deltaTracker) -> {
        LocalPlayer pl = Minecraft.getInstance().player;
        if (pl == null || Minecraft.getInstance().options.hideGui)
            return;

        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        renderCompass(g, pl, sw, sh);
        if (inZone) {
            renderZoneHint(g, sw, sh);
        }
        renderMatchHud(g, sw, sh);
    };

    /**
     * Устанавливает состояние нахождения в зоне выбора классов
     */
    public static void setInZoneStatus(boolean status) {
        inZone = status;
    }

    public static void setTeamBalance(String team1Name, int team1Balance, String team2Name, int team2Balance) {
        HudOverlay.team1Name = team1Name;
        HudOverlay.team1Balance = team1Balance;
        HudOverlay.team2Name = team2Name;
        HudOverlay.team2Balance = team2Balance;
    }

    /**
     * Скрывает бар голода для милсим-реализма
     * Отменяет отрисовку HUD для элемента FOOD в событии рендера интерфейса
     */
    // NeoForge 1.21.1: Use RenderGuiLayerEvent.Pre instead of
    // RenderGuiOverlayEvent.Pre
    @SubscribeEvent
    public static void onRenderFood(RenderGuiLayerEvent.Pre e) {
        if (Config.isDisableHunger()) {
            // Только точное совпадение с vanilla food level
            if (e.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
                e.setCanceled(true);
            }
        }
    }

    /**
     * Скрывает бар брони для милсим-реализма
     * Отменяет отрисовку HUD для элемента ARMOR в событии рендера интерфейса
     */
    @SubscribeEvent
    public static void onRenderArmor(RenderGuiLayerEvent.Pre e) {
        if (Config.isDisableArmor()) {
            // Только точное совпадение с vanilla armor level
            if (e.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) {
                e.setCanceled(true);
            }
        }
    }

    private static void renderCompass(GuiGraphics g, LocalPlayer p, int sw, int sh) {
        Font f = Minecraft.getInstance().font;
        int cx = sw / 2;
        int left = cx - WIDTH / 2;
        int right = cx + WIDTH / 2;
        // Позиционируем компас внизу экрана
        int bot = sh - BOTTOM_MARGIN; // нижняя позиция
        int top = bot - HEIGHT; // верхняя позиция компаса

        // Преобразуем yaw из системы Minecraft в стандартную систему координат компаса
        // В Minecraft: 0° = Юг, 90° = Запад, 180° = Север, 270° = Восток
        // В стандартной системе: 0° = Север, 90° = Восток, 180° = Юг, 270° = Запад
        // Преобразование: standardYaw = (minecraftYaw + 180) % 360
        float minecraftYaw = (p.getYRot() % 360 + 360) % 360;
        float yaw = (minecraftYaw + 180) % 360;
        smooth(yaw);

        // Исправленное масштабирование: используем фиксированный коэффициент для
        // компаса
        // Координаты уже масштабированы через getGuiScaledWidth(), поэтому используем
        // фиксированное значение
        float scale = 2.0f;

        /* ---- деления каждые 15° ---- */
        for (int a = 0; a < 360; a += 15) {
            float d = diff(smYaw, a);
            if (Math.abs(d) > 80)
                continue;
            int x = cx + Math.round(d * scale);
            if (x < left + 4 || x > right - 4)
                continue;

            int h = (a % 90 == 0) ? 10 : (a % 45 == 0 ? 7 : 4);
            int col = (a % 90 == 0) ? TICK_MAIN : TICK_SUB;
            float fade = 1f - Math.abs(d) / 80f * 0.6f;
            // Деления рисуем снизу вверх (от bot к top)
            g.fill(x, bot - h - 3, x + 1, bot - 3, alpha(col, fade));
        }

        /* ---- направления ---- */
        String[] dir = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
        int[] ang = { 0, 45, 90, 135, 180, 225, 270, 315 };
        for (int i = 0; i < dir.length; i++) {
            float d = diff(smYaw, ang[i]);
            if (Math.abs(d) > 70)
                continue;
            int x = cx + Math.round(d * scale);
            if (x < left + 10 || x > right - 10)
                continue;

            int clr = (dir[i].length() == 1) ? DIR_MAIN : DIR_SUB;
            if (dir[i].equals("N"))
                clr = NORTH_CLR;
            float fade = 1f - Math.abs(d) / 70f * 0.5f;
            // Направления рисуем в середине компаса
            g.drawString(f, dir[i], x - f.width(dir[i]) / 2, top + 8, alpha(clr, fade));
        }

        /* ---- градусы каждые 30° ---- */
        for (int a = 0; a < 360; a += 30) {
            if (a % 45 == 0)
                continue;
            float d = diff(smYaw, a);
            if (Math.abs(d) > 60)
                continue;
            int x = cx + Math.round(d * scale);
            String t = String.valueOf(a);
            float fade = 1f - Math.abs(d) / 60f * 0.5f;
            g.drawString(f, t, x - f.width(t) / 2, top + 15, alpha(DEG_CLR, fade));
        }

        /* ---- центральный указатель ---- */
        g.fill(cx, top + 1, cx + 1, bot - 1, POINTER_CLR);
        // Треугольник теперь направлен вниз
        triDown(g, cx, bot, 6, POINTER_CLR);

        /* ---- цифровой азимут ---- */
        int b = Math.round(smYaw) % 360;
        String bs = String.format("%03d", b);
        int tw = f.width(bs);
        g.drawString(f, bs, cx - tw / 2, top - 10, DIR_MAIN);
    }

    /* рисуем равнобедренный треугольник вверх */
    private static void triUp(GuiGraphics g, int cx, int y, int h, int c) {
        for (int i = 0; i < h; i++) {
            g.fill(cx - i, y - i, cx + i + 1, y - i + 1, c);
        }
    }

    /* рисуем равнобедренный треугольник вниз */
    private static void triDown(GuiGraphics g, int cx, int y, int h, int c) {
        for (int i = 0; i < h; i++) {
            g.fill(cx - i, y + i, cx + i + 1, y + i + 1, c);
        }
    }

    private static void smooth(float t) {
        long n = System.currentTimeMillis();
        if (last == 0) {
            last = n;
            smYaw = t;
            return;
        }
        float dt = Math.min((n - last) / 1000f, 0.06f);
        last = n;
        float d = diff(smYaw, t);
        smYaw = (smYaw + d * dt * 7f) % 360;
        if (smYaw < 0)
            smYaw += 360;
    }

    private static float diff(float a, float b) {
        float d = b - a;
        if (d > 180)
            d -= 360;
        if (d < -180)
            d += 360;
        return d;
    }

    private static int alpha(int clr, float a) {
        a = Mth.clamp(a, 0f, 1f);
        int A = (int) (((clr >>> 24) & 0xFF) * a);
        return (clr & 0x00FFFFFF) | (A << 24);
    }

    /**
     * Рендерит подсказку о возможности открыть окно выбора класса
     * Современный дизайн в стиле AAA игр (Squad, Battlefield, Arma Reforger)
     */
    private static void renderZoneHint(GuiGraphics g, int sw, int sh) {
        Font f = Minecraft.getInstance().font;

        // Получаем название клавиши для открытия выбора класса
        String keyName = ru.liko.pjmbasemod.client.input.ModKeyBindings.OPEN_CLASS_SELECTION.getTranslatedKeyMessage()
                .getString();

        // Формируем компактный текст подсказки в стиле AAA игр
        String mainText = Component.translatable("wrb.class.zone.hint").getString();
        String keyText = "[" + keyName + "]";

        // Вычисляем размеры
        int mainWidth = f.width(mainText);
        int keyWidth = f.width(keyText);
        int spacing = 8;
        int totalWidth = mainWidth + spacing + keyWidth;

        // Padding для фона
        int padding = 10;
        int accentHeight = 3;

        // Позиционируем подсказку в центре экрана, немного выше компаса
        int hintY = sh - BOTTOM_MARGIN - HEIGHT - 40;
        int hintX = (sw - totalWidth) / 2;

        // Координаты фона
        int bgX = hintX - padding;
        int bgY = hintY - padding / 2;
        int bgWidth = totalWidth + padding * 2;
        int bgHeight = f.lineHeight + padding;

        // Цвета в стиле AAA игр
        int bgColor = 0xF0101010; // Очень темный фон
        int accentColor = 0xFFE67E22; // Оранжевый акцент (тактический)
        int borderColor = 0xFF555555; // Серая рамка
        int mainTextColor = 0xFFFFFFFF; // Белый текст
        int keyTextColor = 0xFFE67E22; // Оранжевая клавиша

        // Рисуем основной фон
        g.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, bgColor);

        // Верхняя акцентная полоса
        g.fill(bgX, bgY, bgX + bgWidth, bgY + accentHeight, accentColor);

        // Рамка (1px толщина)
        g.fill(bgX, bgY, bgX + bgWidth, bgY + 1, borderColor); // Верх
        g.fill(bgX, bgY + bgHeight - 1, bgX + bgWidth, bgY + bgHeight, borderColor); // Низ
        g.fill(bgX, bgY, bgX + 1, bgY + bgHeight, borderColor); // Лево
        g.fill(bgX + bgWidth - 1, bgY, bgX + bgWidth, bgY + bgHeight, borderColor); // Право

        // Внутреннее свечение
        int glowColor = alpha(accentColor, 0.3f);
        g.fill(bgX + 1, bgY + accentHeight, bgX + bgWidth - 1, bgY + accentHeight + 1, glowColor);

        // Рисуем текст
        int textY = bgY + (bgHeight - f.lineHeight) / 2;
        g.drawString(f, mainText, hintX, textY, mainTextColor, false);
        g.drawString(f, keyText, hintX + mainWidth + spacing, textY, keyTextColor, false);
    }

    /**
     * Рендерит HUD для матча: таймер, состояние и счёт
     */
    private static void renderMatchHud(GuiGraphics g, int sw, int sh) {
        ru.liko.pjmbasemod.common.match.MatchState state = ru.liko.pjmbasemod.client.ClientMatchData.getState();
        int timer = ru.liko.pjmbasemod.client.ClientMatchData.getTimer();

        // Don't render if waiting
        if (state == ru.liko.pjmbasemod.common.match.MatchState.WAITING_FOR_PLAYERS)
            return;

        Font f = Minecraft.getInstance().font;

        // Format timer as mm:ss
        int totalSeconds = timer / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);

        // State text
        String stateText = state.name().replace("_", " ");

        // Colors
        int bgColor = 0xCC111111;
        int borderColor = 0xFFFFAA00;
        int textColor = 0xFFFFFFFF;
        int timerColor = (state == ru.liko.pjmbasemod.common.match.MatchState.STARTING || totalSeconds < 60)
                ? 0xFFFF4444
                : 0xFF00FF00;

        // Position: Top center
        int centerX = sw / 2;
        int boxY = 10;

        // === TICKETS (только во время матча) ===
        String t1 = ru.liko.pjmbasemod.client.ClientMatchData.getTicketTeam1();
        String t2 = ru.liko.pjmbasemod.client.ClientMatchData.getTicketTeam2();
        int tc1 = ru.liko.pjmbasemod.client.ClientMatchData.getTicketCount1();
        int tc2 = ru.liko.pjmbasemod.client.ClientMatchData.getTicketCount2();
        boolean showTickets = state == ru.liko.pjmbasemod.common.match.MatchState.IN_PROGRESS
                && !t1.isEmpty() && !t2.isEmpty();

        if (showTickets) {
            // Центральный таймер
            int timerBoxW = 60;
            int timerBoxH = 24;
            g.fill(centerX - timerBoxW / 2, boxY, centerX + timerBoxW / 2, boxY + timerBoxH, bgColor);
            g.fill(centerX - timerBoxW / 2, boxY + timerBoxH - 2, centerX + timerBoxW / 2, boxY + timerBoxH, borderColor);
            g.drawCenteredString(f, timeText, centerX, boxY + 4, timerColor);
            g.drawCenteredString(f, stateText, centerX, boxY + 14, 0xFF888888);

            // Тикеты команды 1 (слева)
            int ticketBoxW = 80;
            int t1x = centerX - timerBoxW / 2 - ticketBoxW - 4;
            int t1Color = tc1 < 50 ? 0xFFFF4444 : (tc1 < 100 ? 0xFFFFAA00 : 0xFF55FF55);
            g.fill(t1x, boxY, t1x + ticketBoxW, boxY + timerBoxH, bgColor);
            g.fill(t1x, boxY + timerBoxH - 2, t1x + ticketBoxW, boxY + timerBoxH, 0xFF4444FF);
            g.drawCenteredString(f, t1.toUpperCase(), t1x + ticketBoxW / 2, boxY + 3, 0xFF8888FF);
            g.drawCenteredString(f, String.valueOf(tc1), t1x + ticketBoxW / 2, boxY + 13, t1Color);

            // Тикеты команды 2 (справа)
            int t2x = centerX + timerBoxW / 2 + 4;
            int t2Color = tc2 < 50 ? 0xFFFF4444 : (tc2 < 100 ? 0xFFFFAA00 : 0xFF55FF55);
            g.fill(t2x, boxY, t2x + ticketBoxW, boxY + timerBoxH, bgColor);
            g.fill(t2x, boxY + timerBoxH - 2, t2x + ticketBoxW, boxY + timerBoxH, 0xFFFF4444);
            g.drawCenteredString(f, t2.toUpperCase(), t2x + ticketBoxW / 2, boxY + 3, 0xFFFF8888);
            g.drawCenteredString(f, String.valueOf(tc2), t2x + ticketBoxW / 2, boxY + 13, t2Color);
        } else {
            // Простой бокс без тикетов (для STARTING, VOTING и т.д.)
            int timerWidth = f.width(timeText);
            int stateWidth = f.width(stateText);
            int maxWidth = Math.max(timerWidth, stateWidth);
            int boxWidth = maxWidth + 20;
            int boxHeight = 30;
            int boxX = centerX - boxWidth / 2;

            g.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, bgColor);
            g.fill(boxX, boxY + boxHeight - 2, boxX + boxWidth, boxY + boxHeight, borderColor);

            g.drawCenteredString(f, timeText, centerX, boxY + 5, timerColor);
            g.drawCenteredString(f, stateText, centerX, boxY + 16, textColor);
        }

        // Название карты под основным HUD
        String mapName = ru.liko.pjmbasemod.client.ClientMatchData.getCurrentMapDisplayName();
        if (mapName != null && !mapName.isEmpty()) {
            int mapY = boxY + (showTickets ? 28 : 34);
            g.drawCenteredString(f, mapName.toUpperCase(), centerX, mapY, 0xFF888888);
        }

        // Фаза матча (PREPARING / COMBAT / ENDING) под названием карты
        String phaseText = switch (state) {
            case STARTING -> "PREPARING";
            case IN_PROGRESS -> "COMBAT";
            case SHOWING_STATS -> "MATCH OVER";
            case VOTING -> "MAP VOTE";
            case ENDING -> "ENDING";
            default -> "";
        };
        if (!phaseText.isEmpty()) {
            int phaseY = boxY + (showTickets ? 38 : 44);
            int phaseColor = switch (state) {
                case IN_PROGRESS -> 0xFF27AE60;
                case STARTING -> 0xFFE67E22;
                case SHOWING_STATS, VOTING -> 0xFF3498DB;
                default -> 0xFF666666;
            };
            g.drawCenteredString(f, phaseText, centerX, phaseY, phaseColor);
        }
    }
}
