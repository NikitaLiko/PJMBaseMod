package ru.liko.pjmbasemod.common.player;

import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Enum представляющий классы игрока в стиле Squad/Blockfront.
 * Каждый класс имеет уникальные характеристики и снаряжение.
 */
public enum PjmPlayerClass {
    NONE("none", "wrb.class.none"),
    ASSAULT("assault", "wrb.class.assault"),
    MACHINE_GUNNER("machine_gunner", "wrb.class.machine_gunner"),
    MEDIC("medic", "wrb.class.medic"),
    CREW("crew", "wrb.class.crew"),
    ANTI_TANK("anti_tank", "wrb.class.anti_tank"),
    ENGINEER("engineer", "wrb.class.engineer"),
    SNIPER("sniper", "wrb.class.sniper"),
    SSO("sso", "wrb.class.sso"),
    UAV_OPERATOR("uav_operator", "wrb.class.uav_operator"),
    SCOUT("scout", "wrb.class.scout"),
    EW_SPECIALIST("ew_specialist", "wrb.class.ew_specialist"),
    SPN("spn", "wrb.class.spn");;

    private final String id;
    private final String translationKey;

    PjmPlayerClass(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    /**
     * Возвращает ID класса для сериализации
     */
    public String getId() {
        return id;
    }

    /**
     * Возвращает ключ перевода для названия класса
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Возвращает переводимый компонент с названием класса
     */
    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    /**
     * Возвращает ключ перевода для описания класса
     */
    public String getDescriptionKey() {
        return translationKey + ".description";
    }

    /**
     * Возвращает переводимый компонент с описанием класса
     */
    public Component getDescription() {
        return Component.translatable(getDescriptionKey());
    }

    /**
     * Находит класс по ID
     */
    public static Optional<PjmPlayerClass> fromId(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        
        for (PjmPlayerClass playerClass : values()) {
            if (playerClass.id.equals(id)) {
                return Optional.of(playerClass);
            }
        }
        
        return Optional.empty();
    }

    /**
     * Находит класс по ID или возвращает NONE по умолчанию
     */
    public static PjmPlayerClass fromIdOrDefault(String id) {
        return fromId(id).orElse(NONE);
    }

    /**
     * Проверяет, является ли класс выбираемым (не NONE)
     */
    public boolean isSelectable() {
        return this != NONE;
    }
    
    /**
     * Проверяет, требует ли класс специального разрешения для выбора
     */
    public boolean requiresPermission() {
        return this == SSO || this == SPN;
    }
    
    /**
     * Проверяет, доступен ли класс только для Team1
     */
    public boolean isTeam1Only() {
        return this == SPN;
    }

    /**
     * Возвращает ключ перевода для иконки класса в конфиге
     */
    public String getConfigKey() {
        return id;
    }
}

