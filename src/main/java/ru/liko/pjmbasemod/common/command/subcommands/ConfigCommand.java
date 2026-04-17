package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import ru.liko.pjmbasemod.common.PjmServerConfig;

/**
 * /pjm config - Просмотр и изменение конфигурации в реальном времени.
 *
 * Команды:
 * /pjm config list              - Показать все настройки
 * /pjm config get <key>         - Показать значение
 * /pjm config set <key> <value> - Установить значение
 * /pjm config reload            - Перезагрузить с диска
 */
public class ConfigCommand {

    private static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{
                    // Teams
                    "teams.team1Name", "teams.team2Name", "teams.balanceThreshold",
                    // Kit
                    "kitCooldownSeconds",
                    // Capture
                    "capture.enabled", "capture.captureTimeSeconds", "capture.spawnCooldownSeconds",
                    "capture.defaultPointRadius",
                    // AntiGrief
                    "antiGrief.enabled", "antiGrief.preventItemDrop", "antiGrief.preventItemPickup",
                    "antiGrief.preventBlockInteraction", "antiGrief.maxDigDepth", "antiGrief.enableBlockLogging",
                    // MilSim
                    "milsim.disableHunger", "milsim.disableArmor", "milsim.blackDeathScreen",
                    "milsim.muteSoundsOnDeath", "milsim.enableCameraHeadBob",
                    // SquadHud
                    "squadHud.enableSquadPlayerList", "squadHud.enableWeaponInfo",
                    "squadHud.enableItemSwitchPanel", "squadHud.itemSwitchDisplayTime",
                    // Chat
                    "chat.enabled", "chat.localChatRadius", "chat.defaultChatMode",
                    // Debug
                    "enableDebugLogging"
            }, builder);

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("config")
            .requires(src -> src.hasPermission(3))
            .executes(ctx -> listConfig(ctx.getSource()))

            // /pjm config reload
            .then(Commands.literal("reload")
                .executes(ctx -> reloadConfig(ctx.getSource())))

            // /pjm config list
            .then(Commands.literal("list")
                .executes(ctx -> listConfig(ctx.getSource())))

            // /pjm config get <key>
            .then(Commands.literal("get")
                .then(Commands.argument("key", StringArgumentType.word())
                    .suggests(KEY_SUGGESTIONS)
                    .executes(ctx -> getConfig(ctx.getSource(), StringArgumentType.getString(ctx, "key")))))

            // /pjm config set <key> <value>
            .then(Commands.literal("set")
                .then(Commands.argument("key", StringArgumentType.word())
                    .suggests(KEY_SUGGESTIONS)
                    .then(Commands.argument("value", StringArgumentType.greedyString())
                        .executes(ctx -> setConfig(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "key"),
                            StringArgumentType.getString(ctx, "value")
                        )))));
    }

    private static int reloadConfig(CommandSourceStack source) {
        if (ru.liko.pjmbasemod.Config.reload()) {
            source.sendSuccess(() -> Component.literal("§a✔ Конфигурация перезагружена с диска."), true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("§cОшибка перезагрузки конфигурации."));
            return 0;
        }
    }

    private static int getConfig(CommandSourceStack source, String key) {
        String value = getValueByKey(key);
        if (value == null) {
            source.sendFailure(Component.literal("§cНеизвестный ключ: §e" + key));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§e" + key + " §7= §a" + value), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setConfig(CommandSourceStack source, String key, String value) {
        String result = setValueByKey(key, value);
        if (result == null) {
            source.sendFailure(Component.literal("§cНеизвестный ключ: §e" + key));
            return 0;
        }
        if (result.startsWith("ERROR:")) {
            source.sendFailure(Component.literal("§c" + result.substring(6)));
            return 0;
        }
        PjmServerConfig.saveToFile();
        source.sendSuccess(() -> Component.literal("§a✔ §e" + key + " §a= §e" + result), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listConfig(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== WRB Config ==="), false);
        source.sendSuccess(() -> Component.literal("§7Файл: §8" + PjmServerConfig.getConfigFilePath()), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // Teams
        source.sendSuccess(() -> Component.literal("§6--- Teams ---"), false);
        source.sendSuccess(() -> Component.literal("§7teams.team1Name: §e" + PjmServerConfig.getTeam1Name()), false);
        source.sendSuccess(() -> Component.literal("§7teams.team2Name: §e" + PjmServerConfig.getTeam2Name()), false);
        source.sendSuccess(() -> Component.literal("§7teams.balanceThreshold: §e" + PjmServerConfig.getTeamBalanceThreshold()), false);

        // Kit
        source.sendSuccess(() -> Component.literal("§7kitCooldownSeconds: §e" + PjmServerConfig.getKitCooldownSeconds()), false);

        // Capture
        source.sendSuccess(() -> Component.literal("§6--- Capture ---"), false);
        source.sendSuccess(() -> Component.literal("§7capture.enabled: §e" + PjmServerConfig.isCaptureSystemEnabled()), false);
        source.sendSuccess(() -> Component.literal("§7capture.captureTimeSeconds: §e" + PjmServerConfig.getCaptureTimeSeconds()), false);
        source.sendSuccess(() -> Component.literal("§7capture.spawnCooldownSeconds: §e" + PjmServerConfig.getSpawnCooldownSeconds()), false);

        // AntiGrief
        source.sendSuccess(() -> Component.literal("§6--- AntiGrief ---"), false);
        source.sendSuccess(() -> Component.literal("§7antiGrief.enabled: §e" + PjmServerConfig.isAntiGriefEnabled()), false);
        source.sendSuccess(() -> Component.literal("§7antiGrief.maxDigDepth: §e" + PjmServerConfig.getMaxDigDepth()), false);
        source.sendSuccess(() -> Component.literal("§7antiGrief.preventItemDrop: §e" + PjmServerConfig.isPreventItemDrop()), false);
        source.sendSuccess(() -> Component.literal("§7antiGrief.preventItemPickup: §e" + PjmServerConfig.isPreventItemPickup()), false);

        // MilSim
        source.sendSuccess(() -> Component.literal("§6--- MilSim ---"), false);
        source.sendSuccess(() -> Component.literal("§7milsim.disableHunger: §e" + PjmServerConfig.isDisableHunger()), false);
        source.sendSuccess(() -> Component.literal("§7milsim.disableArmor: §e" + PjmServerConfig.isDisableArmor()), false);
        source.sendSuccess(() -> Component.literal("§7milsim.blackDeathScreen: §e" + PjmServerConfig.isBlackDeathScreen()), false);

        // Chat
        source.sendSuccess(() -> Component.literal("§6--- Chat ---"), false);
        source.sendSuccess(() -> Component.literal("§7chat.enabled: §e" + PjmServerConfig.isChatSystemEnabled()), false);
        source.sendSuccess(() -> Component.literal("§7chat.localChatRadius: §e" + PjmServerConfig.getLocalChatRadius()), false);
        source.sendSuccess(() -> Component.literal("§7chat.defaultChatMode: §e" + PjmServerConfig.getDefaultChatMode()), false);

        source.sendSuccess(() -> Component.literal("§6--- Debug ---"), false);
        source.sendSuccess(() -> Component.literal("§7enableDebugLogging: §e" + PjmServerConfig.isDebugLoggingEnabled()), false);

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7Изменить: §e/pjm config set <key> <value>"), false);
        source.sendSuccess(() -> Component.literal("§7Перезагрузить: §e/pjm config reload"), false);
        return Command.SINGLE_SUCCESS;
    }

    // --- Универсальный getter ---

    private static String getValueByKey(String key) {
        return switch (key) {
            // Teams
            case "teams.team1Name" -> PjmServerConfig.getTeam1Name();
            case "teams.team2Name" -> PjmServerConfig.getTeam2Name();
            case "teams.balanceThreshold" -> String.valueOf(PjmServerConfig.getTeamBalanceThreshold());
            // Kit
            case "kitCooldownSeconds" -> String.valueOf(PjmServerConfig.getKitCooldownSeconds());
            // Capture
            case "capture.enabled" -> String.valueOf(PjmServerConfig.isCaptureSystemEnabled());
            case "capture.captureTimeSeconds" -> String.valueOf(PjmServerConfig.getCaptureTimeSeconds());
            case "capture.spawnCooldownSeconds" -> String.valueOf(PjmServerConfig.getSpawnCooldownSeconds());
            case "capture.defaultPointRadius" -> String.valueOf(PjmServerConfig.getCapturePointDefaultRadius());
            // AntiGrief
            case "antiGrief.enabled" -> String.valueOf(PjmServerConfig.isAntiGriefEnabled());
            case "antiGrief.preventItemDrop" -> String.valueOf(PjmServerConfig.isPreventItemDrop());
            case "antiGrief.preventItemPickup" -> String.valueOf(PjmServerConfig.isPreventItemPickup());
            case "antiGrief.preventBlockInteraction" -> String.valueOf(PjmServerConfig.isPreventBlockInteraction());
            case "antiGrief.maxDigDepth" -> String.valueOf(PjmServerConfig.getMaxDigDepth());
            case "antiGrief.enableBlockLogging" -> String.valueOf(PjmServerConfig.isBlockLoggingEnabled());
            // MilSim
            case "milsim.disableHunger" -> String.valueOf(PjmServerConfig.isDisableHunger());
            case "milsim.disableArmor" -> String.valueOf(PjmServerConfig.isDisableArmor());
            case "milsim.blackDeathScreen" -> String.valueOf(PjmServerConfig.isBlackDeathScreen());
            case "milsim.muteSoundsOnDeath" -> String.valueOf(PjmServerConfig.isMuteSoundsOnDeath());
            case "milsim.enableCameraHeadBob" -> String.valueOf(PjmServerConfig.isEnableCameraHeadBob());
            // SquadHud
            case "squadHud.enableSquadPlayerList" -> String.valueOf(PjmServerConfig.isEnableSquadPlayerList());
            case "squadHud.enableWeaponInfo" -> String.valueOf(PjmServerConfig.isEnableWeaponInfo());
            case "squadHud.enableItemSwitchPanel" -> String.valueOf(PjmServerConfig.isEnableItemSwitchPanel());
            case "squadHud.itemSwitchDisplayTime" -> String.valueOf(PjmServerConfig.getItemSwitchDisplayTime());
            // Chat
            case "chat.enabled" -> String.valueOf(PjmServerConfig.isChatSystemEnabled());
            case "chat.localChatRadius" -> String.valueOf(PjmServerConfig.getLocalChatRadius());
            case "chat.defaultChatMode" -> PjmServerConfig.getDefaultChatMode();
            // Debug
            case "enableDebugLogging" -> String.valueOf(PjmServerConfig.isDebugLoggingEnabled());
            default -> null;
        };
    }

    // --- Универсальный setter ---

    private static String setValueByKey(String key, String value) {
        try {
            return switch (key) {
                // Teams
                case "teams.team1Name" -> { PjmServerConfig.setConfigValue(c -> c.teams.team1Name = value); yield value; }
                case "teams.team2Name" -> { PjmServerConfig.setConfigValue(c -> c.teams.team2Name = value); yield value; }
                case "teams.balanceThreshold" -> { int v = Integer.parseInt(value); PjmServerConfig.setConfigValue(c -> c.teams.balanceThreshold = v); yield value; }
                // Kit
                case "kitCooldownSeconds" -> { int v = Integer.parseInt(value); PjmServerConfig.setConfigValue(c -> c.kitCooldownSeconds = v); yield value; }
                // Capture
                case "capture.enabled" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.capture.enabled = v); yield value; }
                case "capture.captureTimeSeconds" -> { int v = Integer.parseInt(value); PjmServerConfig.setConfigValue(c -> c.capture.captureTimeSeconds = v); yield value; }
                case "capture.spawnCooldownSeconds" -> { int v = Integer.parseInt(value); PjmServerConfig.setConfigValue(c -> c.capture.spawnCooldownSeconds = v); yield value; }
                case "capture.defaultPointRadius" -> { double v = Double.parseDouble(value); PjmServerConfig.setConfigValue(c -> c.capture.defaultPointRadius = v); yield value; }
                // AntiGrief
                case "antiGrief.enabled" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.antiGrief.enabled = v); yield value; }
                case "antiGrief.preventItemDrop" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.antiGrief.preventItemDrop = v); yield value; }
                case "antiGrief.preventItemPickup" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.antiGrief.preventItemPickup = v); yield value; }
                case "antiGrief.preventBlockInteraction" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.antiGrief.preventBlockInteraction = v); yield value; }
                case "antiGrief.maxDigDepth" -> { int v = Integer.parseInt(value); PjmServerConfig.setConfigValue(c -> c.antiGrief.maxDigDepth = v); yield value; }
                case "antiGrief.enableBlockLogging" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.antiGrief.enableBlockLogging = v); yield value; }
                // MilSim
                case "milsim.disableHunger" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.milsim.disableHunger = v); yield value; }
                case "milsim.disableArmor" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.milsim.disableArmor = v); yield value; }
                case "milsim.blackDeathScreen" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.milsim.blackDeathScreen = v); yield value; }
                case "milsim.muteSoundsOnDeath" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.milsim.muteSoundsOnDeath = v); yield value; }
                case "milsim.enableCameraHeadBob" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.milsim.enableCameraHeadBob = v); yield value; }
                // SquadHud
                case "squadHud.enableSquadPlayerList" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.squadHud.enableSquadPlayerList = v); yield value; }
                case "squadHud.enableWeaponInfo" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.squadHud.enableWeaponInfo = v); yield value; }
                case "squadHud.enableItemSwitchPanel" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.squadHud.enableItemSwitchPanel = v); yield value; }
                case "squadHud.itemSwitchDisplayTime" -> { int v = Integer.parseInt(value); PjmServerConfig.setConfigValue(c -> c.squadHud.itemSwitchDisplayTime = v); yield value; }
                // Chat
                case "chat.enabled" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.chat.enabled = v); yield value; }
                case "chat.localChatRadius" -> { double v = Double.parseDouble(value); PjmServerConfig.setConfigValue(c -> c.chat.localChatRadius = v); yield value; }
                case "chat.defaultChatMode" -> { PjmServerConfig.setConfigValue(c -> c.chat.defaultChatMode = value); yield value; }
                // Debug
                case "enableDebugLogging" -> { boolean v = Boolean.parseBoolean(value); PjmServerConfig.setConfigValue(c -> c.enableDebugLogging = v); yield value; }
                default -> null;
            };
        } catch (NumberFormatException e) {
            return "ERROR: Неверный формат числа: " + value;
        }
    }
}
