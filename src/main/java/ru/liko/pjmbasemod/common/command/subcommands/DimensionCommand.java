package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import ru.liko.pjmbasemod.common.dimension.DimensionConfig;
import ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager;
import ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.GenType;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * /pjm dimension - Динамическое создание и управление измерениями.
 *
 * Команды:
 * /pjm dimension create <name> [type] - Создать новое измерение
 * (void/flat/normal)
 * /pjm dimension remove <name> - Удалить (выгрузить) измерение
 * /pjm dimension list - Список динамических измерений
 * /pjm dimension tp <name> [player] - Телепортация в измерение
 */
public class DimensionCommand {

    private static final SuggestionProvider<CommandSourceStack> TYPE_SUGGESTIONS = (ctx,
            builder) -> SharedSuggestionProvider.suggest(new String[] { "void", "flat", "normal" }, builder);

    private static final SuggestionProvider<CommandSourceStack> DIM_NAME_SUGGESTIONS = (ctx, builder) -> {
        java.util.List<String> names = new java.util.ArrayList<>(DynamicDimensionManager.getDimensionNames());
        names.add("overworld");
        names.add("nether");
        names.add("the_end");
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEY_SUGGESTIONS = (ctx,
            builder) -> SharedSuggestionProvider.suggest(new String[] {
                    "displayName", "spawnX", "spawnY", "spawnZ", "spawnYaw",
                    "timeOfDay", "weather", "timeFrozen",
                    "pvpEnabled", "mobSpawning", "allowBlockBreaking", "allowBlockPlacing",
                    "allowExplosions", "keepInventory", "announceDeaths",
                    "antiGriefOverride", "worldBorderSize", "worldBorderCenterX", "worldBorderCenterZ",
                    "requiredPermissionLevel", "requiredTeam"
            }, builder);

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("dimension")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> showHelp(ctx.getSource()))

                // /pjm dimension create <name> [type]
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> createDimension(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"), "void"))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(TYPE_SUGGESTIONS)
                                        .executes(ctx -> createDimension(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "type"))))))

                // /pjm dimension remove <name>
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(DIM_NAME_SUGGESTIONS)
                                .executes(ctx -> removeDimension(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))

                // /pjm dimension list
                .then(Commands.literal("list")
                        .executes(ctx -> listDimensions(ctx.getSource())))

                // /pjm dimension tp <name> [player]
                .then(Commands.literal("tp")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(DIM_NAME_SUGGESTIONS)
                                .executes(ctx -> teleportToDimension(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"), null))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> teleportToDimension(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                EntityArgument.getPlayer(ctx, "target"))))))

                // /pjm dimension info
                .then(Commands.literal("info")
                        .executes(ctx -> showDimensionInfo(ctx.getSource())))

                // /pjm dimension config <name> — показать конфиг
                // /pjm dimension config <name> set <key> <value> — установить значение
                // /pjm dimension config <name> reload — перезагрузить с диска
                .then(Commands.literal("config")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(DIM_NAME_SUGGESTIONS)
                                .executes(ctx -> showConfig(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))
                                .then(Commands.literal("reload")
                                        .executes(ctx -> reloadConfig(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("key", StringArgumentType.word())
                                                .suggests(CONFIG_KEY_SUGGESTIONS)
                                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                                        .executes(ctx -> setConfigValue(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                StringArgumentType.getString(ctx, "key"),
                                                                StringArgumentType.getString(ctx, "value"))))))))

                // /pjm dimension setspawn
                .then(Commands.literal("setspawn")
                        .executes(ctx -> setDimensionSpawn(ctx.getSource())));
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Управление измерениями ==="), false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension create <name> [void|flat|normal] §7- Создать"),
                false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension remove <name> §7- Удалить"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension list §7- Список измерений"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension tp <name> [player] §7- Телепортация"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension config <name> §7- Показать конфиг"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension config <name> set <key> <value> §7- Изменить"),
                false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension config <name> reload §7- Перезагрузить"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension info §7- Диагностика текущего дименшона"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int createDimension(CommandSourceStack source, String name, String typeStr) {
        // Валидация имени (только латиница, цифры, подчёркивания)
        if (!name.matches("[a-z0-9_]+")) {
            source.sendFailure(Component.literal(
                    "§cИмя измерения должно содержать только строчные латинские буквы, цифры и подчёркивания."));
            return 0;
        }

        GenType type = GenType.fromString(typeStr);
        if (type == null) {
            source.sendFailure(Component.literal("§cНеизвестный тип генерации: " + typeStr));
            source.sendFailure(Component.literal("§7Доступные типы: §evoid§7, §eflat§7, §enormal"));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("§eСоздание измерения '§6" + name + "§e' (тип: §6" + type + "§e)..."), true);

        if (DynamicDimensionManager.createDimension(source.getServer(), name, type)) {
            source.sendSuccess(() -> Component.literal(
                    "§a✔ Измерение '§e" + name + "§a' успешно создано!\n" +
                            "§7Телепортация: §e/pjm dimension tp " + name),
                    true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component
                    .literal("§cНе удалось создать измерение '§e" + name + "§c'. Возможно, оно уже существует."));
            return 0;
        }
    }

    private static int removeDimension(CommandSourceStack source, String name) {
        if (DynamicDimensionManager.removeDimension(source.getServer(), name)) {
            source.sendSuccess(
                    () -> Component
                            .literal("§a✔ Измерение '§e" + name + "§a' удалено. Все игроки перемещены в overworld."),
                    true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(
                    Component.literal("§cИзмерение '§e" + name + "§c' не найдено среди динамических измерений."));
            return 0;
        }
    }

    private static int listDimensions(CommandSourceStack source) {
        Map<String, String> dims = DynamicDimensionManager.getAllDimensions();

        if (dims.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("§7Динамических измерений нет. Создайте: /pjm dimension create <name>"),
                    false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("§6=== Динамические измерения (" + dims.size() + ") ==="), false);
        for (var entry : dims.entrySet()) {
            String dimName = entry.getKey();
            String type = entry.getValue();
            // Проверяем, загружено ли измерение
            ResourceKey<Level> key = DynamicDimensionManager.getResourceKey(dimName);
            boolean loaded = source.getServer().getLevel(key) != null;
            String status = loaded ? "§a[загружено]" : "§c[не загружено]";

            source.sendSuccess(() -> Component.literal(
                    "  §e" + dimName + " §7[" + type + "] " + status), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportToDimension(CommandSourceStack source, String name, @Nullable ServerPlayer target) {
        if (target == null) {
            if (source.getEntity() instanceof ServerPlayer player) {
                target = player;
            } else {
                source.sendFailure(Component.literal("§cУкажите игрока для телепортации."));
                return 0;
            }
        }

        // Запоминаем текущий GameType до телепорта
        net.minecraft.world.level.GameType prevGameMode = target.gameMode.getGameModeForPlayer();

        // Специальная обработка ванильных измерений
        if (name.equalsIgnoreCase("overworld")) {
            ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) {
                source.sendFailure(Component.literal("§cOverworld не найден!"));
                return 0;
            }
            net.minecraft.core.BlockPos spawnPos = overworld.getSharedSpawnPos();
            target.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    target.getYRot(), 0);
            target.setGameMode(prevGameMode);
            ServerPlayer finalTarget = target;
            source.sendSuccess(() -> Component.literal(
                    "§a✔ §e" + finalTarget.getName().getString() + " §aтелепортирован в §eOverworld §7(спавн мира)"),
                    true);
            return Command.SINGLE_SUCCESS;
        }

        if (name.equalsIgnoreCase("the_nether") || name.equalsIgnoreCase("nether")) {
            ServerLevel nether = source.getServer().getLevel(Level.NETHER);
            if (nether == null) {
                source.sendFailure(Component.literal("§cThe Nether не найден!"));
                return 0;
            }
            target.teleportTo(nether, 0.5, 64, 0.5, target.getYRot(), 0);
            target.setGameMode(prevGameMode);
            ServerPlayer finalTarget = target;
            source.sendSuccess(() -> Component.literal(
                    "§a✔ §e" + finalTarget.getName().getString() + " §aтелепортирован в §eThe Nether"), true);
            return Command.SINGLE_SUCCESS;
        }

        if (name.equalsIgnoreCase("the_end") || name.equalsIgnoreCase("end")) {
            ServerLevel end = source.getServer().getLevel(Level.END);
            if (end == null) {
                source.sendFailure(Component.literal("§cThe End не найден!"));
                return 0;
            }
            target.teleportTo(end, 100.5, 50, 0.5, target.getYRot(), 0);
            target.setGameMode(prevGameMode);
            ServerPlayer finalTarget = target;
            source.sendSuccess(() -> Component.literal(
                    "§a✔ §e" + finalTarget.getName().getString() + " §aтелепортирован в §eThe End"), true);
            return Command.SINGLE_SUCCESS;
        }

        // Динамические измерения
        ResourceKey<Level> dimKey = DynamicDimensionManager.getResourceKey(name);
        ServerLevel level = source.getServer().getLevel(dimKey);

        if (level == null) {
            source.sendFailure(Component.literal("§cИзмерение '§e" + name + "§c' не найдено или не загружено."));
            return 0;
        }

        // Используем спавн из конфига дименшона
        DimensionConfig config = DynamicDimensionManager.getConfig(name);
        double sx = config != null ? config.getSpawnX() : 0.5;
        double sy = config != null ? config.getSpawnY() : 100;
        double sz = config != null ? config.getSpawnZ() : 0.5;
        float syaw = config != null ? config.getSpawnYaw() : 0;
        target.teleportTo(level, sx, sy, sz, syaw, 0);

        // Принудительно пересинхронизируем GameMode с новым уровнем
        target.setGameMode(prevGameMode);

        ServerPlayer finalTarget = target;
        source.sendSuccess(() -> Component.literal(
                "§a✔ §e" + finalTarget.getName().getString() + " §aтелепортирован в '§e" + name + "§a'"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setDimensionSpawn(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cЭту команду может использовать только игрок."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        String dimName = level.dimension().location().getPath();
        DimensionConfig config = DynamicDimensionManager.getConfig(dimName);

        if (config != null) {
            // Это динамическое измерение
            config.setSpawnX(player.getX());
            config.setSpawnY(player.getY());
            config.setSpawnZ(player.getZ());
            config.setSpawnYaw(player.getYRot());
            config.save();
            // Метод applyConfig применит спавн (не обязательно, но для чистоты)
            DynamicDimensionManager.applyConfig(level, config);
            source.sendSuccess(() -> Component.literal("§a✔ Точка возрождения (spawn) для динамического измерения '§e"
                    + dimName + "§a' успешно установлена!"), true);
        } else {
            // Это ванильное или иное измерение
            level.setDefaultSpawnPos(player.blockPosition(), player.getYRot());
            source.sendSuccess(() -> Component.literal(
                    "§a✔ Глобальная точка возрождения для измерения '§e" + dimName + "§a' успешно установлена!"), true);
        }

        return Command.SINGLE_SUCCESS;
    }

    // --- Config ---

    private static int showConfig(CommandSourceStack source, String name) {
        DimensionConfig config = DynamicDimensionManager.getConfig(name);
        if (config == null) {
            source.sendFailure(Component.literal("§cКонфиг измерения '§e" + name + "§c' не найден."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6=== Конфиг: §e" + name + " §6==="), false);
        source.sendSuccess(() -> Component.literal("§7displayName: §e" + config.getDisplayName()), false);
        source.sendSuccess(() -> Component.literal("§7genType: §e" + config.getGenType()), false);
        source.sendSuccess(() -> Component.literal("§7spawn: §e" + config.getSpawnX() + ", " + config.getSpawnY() + ", "
                + config.getSpawnZ() + " yaw=" + config.getSpawnYaw()), false);
        source.sendSuccess(
                () -> Component
                        .literal("§7timeOfDay: §e" + (config.getTimeOfDay() < 0 ? "natural" : config.getTimeOfDay())),
                false);
        source.sendSuccess(() -> Component.literal("§7weather: §e" + config.getWeather()), false);
        source.sendSuccess(() -> Component.literal("§7timeFrozen: §e" + config.isTimeFrozen()), false);
        source.sendSuccess(() -> Component.literal("§7pvpEnabled: §e" + config.isPvpEnabled()), false);
        source.sendSuccess(() -> Component.literal("§7mobSpawning: §e" + config.isMobSpawning()), false);
        source.sendSuccess(() -> Component.literal("§7allowBlockBreaking: §e" + config.isAllowBlockBreaking()), false);
        source.sendSuccess(() -> Component.literal("§7allowBlockPlacing: §e" + config.isAllowBlockPlacing()), false);
        source.sendSuccess(() -> Component.literal("§7allowExplosions: §e" + config.isAllowExplosions()), false);
        source.sendSuccess(() -> Component.literal("§7keepInventory: §e" + config.isKeepInventory()), false);
        source.sendSuccess(() -> Component.literal("§7announceDeaths: §e" + config.isAnnounceDeaths()), false);
        source.sendSuccess(() -> Component.literal("§7antiGriefOverride: §e" + config.getAntiGriefOverride()), false);
        source.sendSuccess(() -> Component.literal("§7worldBorderSize: §e" + config.getWorldBorderSize()), false);
        source.sendSuccess(() -> Component.literal(
                "§7worldBorderCenter: §e" + config.getWorldBorderCenterX() + ", " + config.getWorldBorderCenterZ()),
                false);
        source.sendSuccess(
                () -> Component.literal("§7requiredPermissionLevel: §e" + config.getRequiredPermissionLevel()), false);
        source.sendSuccess(() -> Component.literal(
                "§7requiredTeam: §e" + (config.getRequiredTeam().isEmpty() ? "(любая)" : config.getRequiredTeam())),
                false);
        source.sendSuccess(() -> Component.literal("§7Файл: §8config/pjmbasemod/dimensions/" + name + ".json"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadConfig(CommandSourceStack source, String name) {
        if (DynamicDimensionManager.reloadConfig(source.getServer(), name)) {
            source.sendSuccess(() -> Component.literal("§a✔ Конфиг '§e" + name + "§a' перезагружен и применён."), true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("§cНе удалось перезагрузить конфиг '§e" + name + "§c'."));
            return 0;
        }
    }

    private static int setConfigValue(CommandSourceStack source, String name, String key, String value) {
        DimensionConfig config = DynamicDimensionManager.getConfig(name);
        if (config == null) {
            source.sendFailure(Component.literal("§cКонфиг измерения '§e" + name + "§c' не найден."));
            return 0;
        }

        try {
            switch (key) {
                case "displayName" -> config.setDisplayName(value);
                case "spawnX" -> config.setSpawnX(Double.parseDouble(value));
                case "spawnY" -> config.setSpawnY(Double.parseDouble(value));
                case "spawnZ" -> config.setSpawnZ(Double.parseDouble(value));
                case "spawnYaw" -> config.setSpawnYaw(Float.parseFloat(value));
                case "timeOfDay" -> config.setTimeOfDay(Integer.parseInt(value));
                case "weather" -> config.setWeather(value);
                case "timeFrozen" -> config.setTimeFrozen(Boolean.parseBoolean(value));
                case "pvpEnabled" -> config.setPvpEnabled(Boolean.parseBoolean(value));
                case "mobSpawning" -> config.setMobSpawning(Boolean.parseBoolean(value));
                case "allowBlockBreaking" -> config.setAllowBlockBreaking(Boolean.parseBoolean(value));
                case "allowBlockPlacing" -> config.setAllowBlockPlacing(Boolean.parseBoolean(value));
                case "allowExplosions" -> config.setAllowExplosions(Boolean.parseBoolean(value));
                case "keepInventory" -> config.setKeepInventory(Boolean.parseBoolean(value));
                case "announceDeaths" -> config.setAnnounceDeaths(Boolean.parseBoolean(value));
                case "antiGriefOverride" -> config.setAntiGriefOverride(value);
                case "worldBorderSize" -> config.setWorldBorderSize(Double.parseDouble(value));
                case "worldBorderCenterX" -> config.setWorldBorderCenterX(Double.parseDouble(value));
                case "worldBorderCenterZ" -> config.setWorldBorderCenterZ(Double.parseDouble(value));
                case "requiredPermissionLevel" -> config.setRequiredPermissionLevel(Integer.parseInt(value));
                case "requiredTeam" -> config.setRequiredTeam(value);
                default -> {
                    source.sendFailure(Component.literal("§cНеизвестный ключ: §e" + key));
                    return 0;
                }
            }

            config.save();

            // Применяем настройки к уровню если он загружен
            ResourceKey<Level> dimKey = DynamicDimensionManager.getResourceKey(name);
            ServerLevel level = source.getServer().getLevel(dimKey);
            if (level != null) {
                DynamicDimensionManager.applyConfig(level, config);
            }

            source.sendSuccess(
                    () -> Component.literal("§a✔ §e" + key + " §a= §e" + value + " §7(дименшон: " + name + ")"), true);
            return Command.SINGLE_SUCCESS;

        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("§cНеверный формат числа: §e" + value));
            return 0;
        }
    }

    // --- Диагностика ---

    /**
     * /pjm dimension info — показать информацию о текущем измерении и состоянии
     * игрока
     */
    private static int showDimensionInfo(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cТолько для игроков"));
            return 0;
        }

        ServerLevel playerLevel = player.serverLevel();
        String dimId = playerLevel.dimension().location().toString();
        String gameMode = player.gameMode.getGameModeForPlayer().getName();
        boolean isDynamic = DynamicDimensionManager.getDimensionNames().stream()
                .anyMatch(n -> DynamicDimensionManager.getResourceKey(n).equals(playerLevel.dimension()));
        int loadedChunks = playerLevel.getChunkSource().getLoadedChunksCount();
        boolean canBreak = !playerLevel.getServer().isUnderSpawnProtection(playerLevel, player.blockPosition(), player);

        // Получаем ServerPlayerGameMode.level через рефлексию
        String gmLevelDim = "unknown";
        boolean levelMatch = false;
        try {
            java.lang.reflect.Field levelField = net.minecraft.server.level.ServerPlayerGameMode.class
                    .getDeclaredField("level");
            levelField.setAccessible(true);
            ServerLevel gmLevel = (ServerLevel) levelField.get(player.gameMode);
            gmLevelDim = gmLevel.dimension().location().toString();
            levelMatch = gmLevel == playerLevel;
        } catch (Exception e) {
            gmLevelDim = "reflection error: " + e.getMessage();
        }

        boolean hasOp = player.hasPermissions(2);
        String finalGmLevelDim = gmLevelDim;
        boolean finalLevelMatch = levelMatch;

        source.sendSuccess(() -> Component.literal("§6=== Dimension Info ==="), false);
        source.sendSuccess(() -> Component.literal("§7Dimension: §e" + dimId), false);
        source.sendSuccess(() -> Component.literal("§7Dynamic: §e" + isDynamic), false);
        source.sendSuccess(() -> Component.literal("§7GameMode: §e" + gameMode), false);
        source.sendSuccess(() -> Component.literal("§7Loaded chunks: §e" + loadedChunks), false);
        source.sendSuccess(() -> Component.literal("§7Spawn protection: §e" + !canBreak), false);
        source.sendSuccess(() -> Component.literal("§7Has OP (lvl 2): §e" + hasOp), false);
        source.sendSuccess(() -> Component.literal("§7GameMode level dim: §e" + finalGmLevelDim), false);
        source.sendSuccess(() -> Component.literal("§7Player level dim: §e" + dimId), false);
        source.sendSuccess(() -> Component.literal(
                finalLevelMatch ? "§a✔ GameMode.level совпадает с playerLevel"
                        : "§c✘ MISMATCH! GameMode.level != playerLevel (ЭТО БАГ!)"),
                false);

        return Command.SINGLE_SUCCESS;
    }
}
