package ru.liko.pjmbasemod.common.permission;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Система разрешений для интеграции с нативными командами Minecraft
 * LuckPerms интеграция + vanilla fallback для команд /team, /scoreboard и
 * других
 */
public final class PjmPermissions {

    // Базовые разрешения
    public static final PermissionNode<Boolean> BASE = new PermissionNode<>("pjmbasemod", "base",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(1));

    // Интеграция с нативными командами Minecraft
    public static final PermissionNode<Boolean> MINECRAFT_TEAM = new PermissionNode<>("minecraft", "command.team",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    public static final PermissionNode<Boolean> MINECRAFT_SCOREBOARD = new PermissionNode<>("minecraft",
            "command.scoreboard",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    // WRB расширения для нативных команд
    public static final PermissionNode<Boolean> TEAM_MANAGE = new PermissionNode<>("pjmbasemod", "team.manage",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    public static final PermissionNode<Boolean> TEAM_JOIN_SELF = new PermissionNode<>("pjmbasemod", "team.join.self",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(1));

    public static final PermissionNode<Boolean> TEAM_JOIN_OTHER = new PermissionNode<>("pjmbasemod", "team.join.other",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    // Военные специализации
    public static final PermissionNode<Boolean> MILITARY_SETUP = new PermissionNode<>("pjmbasemod", "military.setup",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(3));

    // Управление рангами
    public static final PermissionNode<Boolean> RANK_MANAGE = new PermissionNode<>("pjmbasemod", "rank.manage",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    public static final PermissionNode<Boolean> RANK_SET_SELF = new PermissionNode<>("pjmbasemod", "rank.set.self",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(1));

    // Управление зонами выбора классов
    public static final PermissionNode<Boolean> CLASS_ZONE_MANAGE = new PermissionNode<>("pjmbasemod",
            "class.zone.manage",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    // Управление точками автоспавна техники
    public static final PermissionNode<Boolean> VEHICLE_SPAWN_MANAGE = new PermissionNode<>("pjmbasemod",
            "vehicle.spawn.manage",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    public static final PermissionNode<Boolean> VEHICLE_SPAWN_FORCE = new PermissionNode<>("pjmbasemod",
            "vehicle.spawn.force",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(3));

    // Управление таймерами команд
    public static final PermissionNode<Boolean> TIMER_CREATE = new PermissionNode<>("pjmbasemod", "timer.create",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    public static final PermissionNode<Boolean> TIMER_MANAGE = new PermissionNode<>("pjmbasemod", "timer.manage",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));

    public static final PermissionNode<Boolean> TIMER_VIEW = new PermissionNode<>("pjmbasemod", "timer.view",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(1));

    // Разрешение на выбор класса ССО (Специальные Силы)
    public static final PermissionNode<Boolean> CLASS_SSO = new PermissionNode<>("pjmbasemod", "class.sso",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2)); // OP level
                                                                                                             // 2 по
                                                                                                             // умолчанию

    // Разрешение на выбор класса СпН (Спецназ) - только для Team1
    public static final PermissionNode<Boolean> CLASS_SPN = new PermissionNode<>("pjmbasemod", "class.spn",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2)); // OP level
                                                                                                             // 2 по
                                                                                                             // умолчанию

    public static final PermissionNode<Boolean> CONFIG_RELOAD = new PermissionNode<>("pjmbasemod", "config.reload",
            PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(3));

    private PjmPermissions() {
    }

    /**
     * Проверяет разрешение на использование нативных команд Minecraft
     */
    public static boolean canUseMinecraftCommand(ServerPlayer player, String command) {
        return switch (command.toLowerCase()) {
            case "team" -> PermissionAPI.getPermission(player, MINECRAFT_TEAM);
            case "scoreboard" -> PermissionAPI.getPermission(player, MINECRAFT_SCOREBOARD);
            default -> player.hasPermissions(2); // Fallback на OP level 2
        };
    }

    /**
     * Проверяет разрешение на управление командами
     */
    public static boolean canModifyTeam(ServerPlayer player, boolean isSelf) {
        if (isSelf) {
            return PermissionAPI.getPermission(player, TEAM_JOIN_SELF);
        } else {
            return PermissionAPI.getPermission(player, TEAM_JOIN_OTHER);
        }
    }

    /**
     * Проверяет разрешение для изменения данных других игроков
     */
    public static boolean canModifyOther(ServerPlayer player, String command) {
        return switch (command.toLowerCase()) {
            case "rank" -> PermissionAPI.getPermission(player, RANK_MANAGE);
            case "team" -> PermissionAPI.getPermission(player, TEAM_MANAGE);
            case "military" -> PermissionAPI.getPermission(player, MILITARY_SETUP);
            default -> false;
        };
    }

    /**
     * Гибридная проверка для нативных команд Minecraft
     */
    public static boolean checkNativeCommand(ServerPlayer player, String command) {
        return canUseMinecraftCommand(player, command);
    }

    /**
     * Проверка для специфичных возможностей мода
     */
    public static boolean checkModFeature(ServerPlayer player, String feature) {
        return canModifyOther(player, feature);
    }

    /**
     * Проверяет, считается ли игрок администратором (OP level 3+)
     */
    public static boolean isAdmin(ServerPlayer player) {
        return player != null && player.hasPermissions(3);
    }
}