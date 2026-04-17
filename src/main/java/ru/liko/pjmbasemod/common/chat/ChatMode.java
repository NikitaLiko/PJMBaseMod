package ru.liko.pjmbasemod.common.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Режимы чата для системы локального/глобального чата
 */
public enum ChatMode {
    LOCAL("local", "Локальный", "Local", ChatFormatting.AQUA, "[LOCAL]"),
    GLOBAL("global", "Глобальный", "Global", ChatFormatting.GOLD, "[GLOBAL]"),
    TEAM("team", "Командный", "Team", ChatFormatting.YELLOW, "[TEAM]");

    private final String id;
    private final String russianName;
    private final String englishName;
    private final ChatFormatting color;
    private final String prefix;

    ChatMode(String id, String russianName, String englishName, ChatFormatting color, String prefix) {
        this.id = id;
        this.russianName = russianName;
        this.englishName = englishName;
        this.color = color;
        this.prefix = prefix;
    }

    public String getId() {
        return id;
    }

    public String getRussianName() {
        return russianName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Возвращает отформатированное название режима чата
     */
    public MutableComponent getDisplayName() {
        return Component.literal(russianName).withStyle(color);
    }

    /**
     * Возвращает отформатированный префикс для сообщения
     */
    public MutableComponent getFormattedPrefix() {
        return Component.literal(prefix + " ").withStyle(color);
    }

    /**
     * Парсит режим чата из строки (поддерживает ID, русское и английское названия)
     */
    public static ChatMode fromString(String input) {
        if (input == null || input.isEmpty()) {
            return LOCAL; // По умолчанию локальный
        }

        String normalized = input.toLowerCase().trim();

        for (ChatMode mode : values()) {
            if (mode.id.equalsIgnoreCase(normalized) ||
                mode.russianName.equalsIgnoreCase(normalized) ||
                mode.englishName.equalsIgnoreCase(normalized)) {
                return mode;
            }
        }

        return LOCAL; // Fallback на локальный чат
    }

    /**
     * Возвращает следующий режим чата (для циклического переключения)
     */
    public ChatMode next() {
        ChatMode[] modes = values();
        int nextIndex = (this.ordinal() + 1) % modes.length;
        return modes[nextIndex];
    }

    /**
     * Проверяет, является ли строка валидным режимом чата
     */
    public static boolean isValid(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String normalized = input.toLowerCase().trim();

        for (ChatMode mode : values()) {
            if (mode.id.equalsIgnoreCase(normalized) ||
                mode.russianName.equalsIgnoreCase(normalized) ||
                mode.englishName.equalsIgnoreCase(normalized)) {
                return true;
            }
        }

        return false;
    }
}
