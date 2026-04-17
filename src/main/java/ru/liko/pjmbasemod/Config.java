package ru.liko.pjmbasemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Конфигурация мода PJM BaseMod.
 * Все настройки загружаются из pjm_config.json через PjmServerConfig.
 * Этот класс предоставляет методы-обертки для обратной совместимости.
 */
public class Config {

    // Runtime flags (not saved to config file)
    public static boolean adminEspEnabled = false;
    public static boolean espShowBox = true;
    public static boolean espShowTracers = false;
    public static boolean espShowInfo = true;
    public static boolean espShowHealth = true;
    public static boolean espTeamColors = true;
    
    // Entity ESP Flags
    public static boolean espShowMobs = false;
    public static boolean espShowAnimals = false;
    public static boolean espShowItems = false;
    public static boolean espShowProjectiles = false;


    /**
     * Проверяет, разрешен ли блок для ломания в режиме выживания
     */
    public static boolean isBlockBreakable(net.minecraft.world.level.block.state.BlockState blockState) {
        if (!isAntiGriefEnabled()) {
            return true; // Если защита выключена, все блоки разрешены
        }
        
        Block block = blockState.getBlock();
        ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == null) {
            return false;
        }
        
        String blockIdString = blockId.toString();
        return ru.liko.pjmbasemod.common.PjmServerConfig.getBreakableBlocks().contains(blockIdString);
    }

    /**
     * Проверяет, разрешен ли блок для ломания в режиме выживания по ResourceLocation
     */
    public static boolean isBlockBreakable(ResourceLocation blockId) {
        if (!isAntiGriefEnabled()) {
            return true; // Если защита выключена, все блоки разрешены
        }
        
        if (blockId == null) {
            return false;
        }
        
        String blockIdString = blockId.toString();
        return ru.liko.pjmbasemod.common.PjmServerConfig.getBreakableBlocks().contains(blockIdString);
    }

    /**
     * Проверяет, разрешен ли блок для установки в режиме выживания
     */
    public static boolean isBlockPlaceable(ResourceLocation blockId) {
        if (!isAntiGriefEnabled()) {
            return true; // Если защита выключена, все блоки разрешены
        }
        
        if (blockId == null) {
            return false;
        }
        
        String blockIdString = blockId.toString();
        return ru.liko.pjmbasemod.common.PjmServerConfig.getPlaceableBlocks().contains(blockIdString);
    }

    /**
     * Проверяет, разрешено ли взаимодействие с блоком в режиме выживания
     */
    public static boolean isBlockInteractable(net.minecraft.world.level.block.state.BlockState blockState) {
        if (!isAntiGriefEnabled() || !ru.liko.pjmbasemod.common.PjmServerConfig.isPreventBlockInteraction()) {
            return true; // Если защита выключена или взаимодействие не блокируется, все блоки разрешены
        }
        
        Block block = blockState.getBlock();
        ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == null) {
            return false;
        }
        
        String blockIdString = blockId.toString();
        return ru.liko.pjmbasemod.common.PjmServerConfig.getInteractableBlocks().contains(blockIdString);
    }

    /**
     * Проверяет, разрешено ли взаимодействие с блоком в режиме выживания по ResourceLocation
     */
    public static boolean isBlockInteractable(ResourceLocation blockId) {
        if (!isAntiGriefEnabled() || !ru.liko.pjmbasemod.common.PjmServerConfig.isPreventBlockInteraction()) {
            return true; // Если защита выключена или взаимодействие не блокируется, все блоки разрешены
        }
        
        if (blockId == null) {
            return false;
        }
        
        String blockIdString = blockId.toString();
        return ru.liko.pjmbasemod.common.PjmServerConfig.getInteractableBlocks().contains(blockIdString);
    }
    
    /**
     * Проверяет, включена ли блокировка взаимодействия с блоками
     */
    public static boolean isBlockInteractionPreventionEnabled() {
        return isAntiGriefEnabled() && ru.liko.pjmbasemod.common.PjmServerConfig.isPreventBlockInteraction();
    }

    
    /**
     * Получает список предметов для класса и команды.
     * Делегирует в KitsConfig для загрузки из внешнего файла kits.json.
     */
    public static List<Item> getClassItems(String classId, String teamName) {
        return ru.liko.pjmbasemod.common.KitsConfig.getKitItems(classId, teamName);
    }
    
    /**
     * Получает лимит игроков для класса.
     * Делегирует в PjmServerConfig для загрузки из внешнего файла pjm_config.json.
     */
    public static int getClassLimit(String classId) {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getClassLimit(classId);
    }
    
    // ============== STATIC GETTERS (делегируют в PjmServerConfig) ==============
    
    public static String getTeam1Name() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getTeam1Name();
    }
    
    public static String getTeam2Name() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getTeam2Name();
    }
    
    public static int getTeamBalanceThreshold() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getTeamBalanceThreshold();
    }
    
    public static int getKitCooldownSeconds() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getKitCooldownSeconds();
    }
    
    public static boolean isAntiGriefEnabled() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isAntiGriefEnabled();
    }
    
    public static boolean isDebugLoggingEnabled() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isDebugLoggingEnabled();
    }
    
    public static boolean isChatSystemEnabled() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isChatSystemEnabled();
    }
    
    public static double getLocalChatRadius() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getLocalChatRadius();
    }
    
    public static String getDefaultChatMode() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getDefaultChatMode();
    }
    
    public static int getSpawnCooldownSeconds() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getSpawnCooldownSeconds();
    }
    
    // MilSim
    public static boolean isDisableHunger() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isDisableHunger();
    }
    
    public static boolean isDisableArmor() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isDisableArmor();
    }
    
    public static boolean isBlackDeathScreen() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isBlackDeathScreen();
    }
    
    public static boolean isMuteSoundsOnDeath() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isMuteSoundsOnDeath();
    }
    
    public static boolean isEnableCameraHeadBob() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isEnableCameraHeadBob();
    }
    
    // Anti-Grief
    public static boolean isPreventItemDrop() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isPreventItemDrop();
    }
    
    public static boolean isPreventItemPickup() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isPreventItemPickup();
    }
    
    public static int getMaxDigDepth() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getMaxDigDepth();
    }
    
    public static boolean isBlockLoggingEnabled() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isBlockLoggingEnabled();
    }
    
    public static java.util.Map<String, String> getToolRequiredBlocks() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getToolRequiredBlocks();
    }
    
    // Capture System
    public static boolean isCaptureSystemEnabled() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isCaptureSystemEnabled();
    }
    
    public static int getCaptureTimeSeconds() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getCaptureTimeSeconds();
    }
    
    // Squad HUD
    public static boolean isEnableSquadPlayerList() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isEnableSquadPlayerList();
    }
    
    public static boolean isEnableWeaponInfo() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isEnableWeaponInfo();
    }
    
    public static boolean isEnableItemSwitchPanel() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.isEnableItemSwitchPanel();
    }
    
    public static int getItemSwitchDisplayTime() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getItemSwitchDisplayTime();
    }
    
    // Vehicle Crew
    public static List<String> getCrewRestrictedVehicles() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getCrewRestrictedVehicles();
    }
    
    public static List<Integer> getCrewRestrictedSeats() {
        return ru.liko.pjmbasemod.common.PjmServerConfig.getCrewRestrictedSeats();
    }
    
    /**
     * Перезагружает конфигурацию с диска.
     */
    public static boolean reload() {
        boolean result = ru.liko.pjmbasemod.common.PjmServerConfig.reload();
        ru.liko.pjmbasemod.common.KitsConfig.reload();
        return result;
    }
    
    /**
     * Получает полную строку конфигурации предмета с количеством для конкретной команды.
     * Делегирует в KitsConfig для загрузки из внешнего файла kits.json.
     */
    public static List<String> getClassItemStrings(String classId, String teamName) {
        return ru.liko.pjmbasemod.common.KitsConfig.getKitItemStrings(classId, teamName);
    }
}
