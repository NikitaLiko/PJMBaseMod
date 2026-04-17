package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.common.map.MapManager;
import ru.liko.pjmbasemod.common.map.config.MapConfig;
import ru.liko.pjmbasemod.common.map.config.MapConfigManager;
import ru.liko.pjmbasemod.common.match.MatchManager;
import ru.liko.pjmbasemod.common.match.MatchState;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * /pjm match - Полное администрирование матчей
 * 
 * Команды:
 * /pjm match start - Начать матч (запуска отсчёта)
 * /pjm match stop - Остановить матч (сброс в лобби)
 * /pjm match state <state> - Принудительная смена состояния
 * /pjm match timer set <seconds> - Установить таймер текущей фазы
 * /pjm match timer add <seconds> - Добавить время к таймеру
 * /pjm match map load <mapId> - Загрузить карту
 * /pjm match map list - Список доступных карт
 * /pjm match map reload - Перезагрузить конфиги карт
 * /pjm match teleport lobby - Телепортировать всех в лобби
 * /pjm match teleport map - Телепортировать всех на карту
 * /pjm match status - Показать текущий статус матча
 * /pjm match skip - Пропустить текущую фазу
 */
public class MatchCommand {

    // Подсказки для состояний матча
    private static final SuggestionProvider<CommandSourceStack> MATCH_STATE_SUGGESTIONS = (ctx,
            builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(MatchState.values())
                            .map(Enum::name)
                            .collect(Collectors.toList()),
                    builder);

    // Подсказки для ID карт
    private static final SuggestionProvider<CommandSourceStack> MAP_ID_SUGGESTIONS = (ctx,
            builder) -> SharedSuggestionProvider.suggest(
                    MapConfigManager.getAvailableMapIds(),
                    builder);

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("match")
                .requires(src -> src.hasPermission(2)) // Требуется OP уровень 2

                // /pjm match help
                .executes(ctx -> showHelp(ctx.getSource()))

                // /pjm match start [countdown]
                .then(Commands.literal("start")
                        .executes(ctx -> startMatch(ctx.getSource(), 10))
                        .then(Commands.argument("countdown", IntegerArgumentType.integer(0, 300))
                                .executes(ctx -> startMatch(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "countdown")))))

                // /pjm match stop
                .then(Commands.literal("stop")
                        .executes(ctx -> stopMatch(ctx.getSource())))

                // /pjm match state <state>
                .then(Commands.literal("state")
                        .then(Commands.argument("state", StringArgumentType.word())
                                .suggests(MATCH_STATE_SUGGESTIONS)
                                .executes(
                                        ctx -> setState(ctx.getSource(), StringArgumentType.getString(ctx, "state")))))

                // /pjm match timer ...
                .then(Commands.literal("timer")
                        .then(Commands.literal("set")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setTimer(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds")))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer())
                                        .executes(ctx -> addTimer(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds"))))))

                // /pjm match map ...
                .then(Commands.literal("map")
                        .then(Commands.literal("load")
                                .then(Commands.argument("mapId", StringArgumentType.word())
                                        .suggests(MAP_ID_SUGGESTIONS)
                                        .executes(ctx -> loadMap(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mapId")))))
                        .then(Commands.literal("list")
                                .executes(ctx -> listMaps(ctx.getSource())))
                        .then(Commands.literal("reload")
                                .executes(ctx -> reloadMaps(ctx.getSource()))))

                // /pjm match teleport ...
                .then(Commands.literal("teleport")
                        .then(Commands.literal("lobby")
                                .executes(ctx -> teleportAllToLobby(ctx.getSource())))
                        .then(Commands.literal("map")
                                .executes(ctx -> teleportAllToMap(ctx.getSource()))))

                // /pjm match status
                .then(Commands.literal("status")
                        .executes(ctx -> showStatus(ctx.getSource())))

                // /pjm match skip
                .then(Commands.literal("skip")
                        .executes(ctx -> skipPhase(ctx.getSource())))

                // /pjm match end [reason]
                .then(Commands.literal("end")
                        .executes(ctx -> endMatch(ctx.getSource(), "Admin decision"))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> endMatch(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "reason")))))

                // /pjm match rotation ...
                .then(Commands.literal("rotation")
                        .executes(ctx -> listRotation(ctx.getSource()))
                        .then(Commands.literal("list")
                                .executes(ctx -> listRotation(ctx.getSource())))
                        .then(Commands.literal("add")
                                .then(Commands.argument("mapId", StringArgumentType.word())
                                        .suggests(MAP_ID_SUGGESTIONS)
                                        .executes(ctx -> addToRotation(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mapId")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("mapId", StringArgumentType.word())
                                        .suggests(MAP_ID_SUGGESTIONS)
                                        .executes(ctx -> removeFromRotation(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mapId")))))
                        .then(Commands.literal("reload")
                                .executes(ctx -> reloadRotation(ctx.getSource()))))

                // /pjm match auto ...
                .then(Commands.literal("auto")
                        .executes(ctx -> showAutoSettings(ctx.getSource()))
                        .then(Commands.literal("enable")
                                .executes(ctx -> setAutoEnabled(ctx.getSource(), true)))
                        .then(Commands.literal("disable")
                                .executes(ctx -> setAutoEnabled(ctx.getSource(), false)))
                        .then(Commands.literal("minplayers")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> setMinPlayers(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("countdown")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 300))
                                        .executes(ctx -> setAutoCountdown(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds")))))
                        .then(Commands.literal("warmup")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 120))
                                        .executes(ctx -> setWarmup(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds")))))
                        .then(Commands.literal("stats")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 120))
                                        .executes(ctx -> setStatsTime(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds")))))
                        .then(Commands.literal("voting")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 120))
                                        .executes(ctx -> setVotingTime(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds"))))));
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Match Administration ==="), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match start [countdown] §7- Начать матч"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match stop §7- Остановить и сбросить матч"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match state <state> §7- Сменить состояние"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match timer set/add <sec> §7- Управление таймером"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match map load/list/reload §7- Управление картами"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match teleport lobby/map §7- Телепортация всех"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match status §7- Текущий статус"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match skip §7- Пропустить фазу"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match end [reason] §7- Завершить матч"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match auto §7- Настройки автоматизации"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match rotation §7- Управление ротацией карт"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int showAutoSettings(CommandSourceStack source) {
        MatchManager m = MatchManager.get();
        source.sendSuccess(() -> Component.literal("§6=== Настройки Автоматизации ==="), false);
        source.sendSuccess(() -> Component.literal("§7Авто-старт: " + (m.isAutoStartEnabled() ? "§aВКЛ" : "§cВЫКЛ")),
                false);
        source.sendSuccess(() -> Component.literal("§7Мин. игроков: §e" + m.getMinPlayersToStart()), false);
        source.sendSuccess(() -> Component.literal("§7Отсчёт до старта: §e" + m.getAutoStartCountdown() + " сек"),
                false);
        source.sendSuccess(() -> Component.literal("§7Ожидание after join: §e" + m.getWarmupAfterJoin() + " сек"),
                false);
        source.sendSuccess(() -> Component.literal("§7Показ статистики: §e" + m.getStatsDisplayTime() + " сек"), false);
        source.sendSuccess(() -> Component.literal("§7Голосование: §e" + m.getVotingTime() + " сек"), false);
        int autoTimer = m.getAutoStartTimer() / 20;
        if (autoTimer > 0) {
            source.sendSuccess(() -> Component.literal("§7До авто-старта: §e" + autoTimer + " сек"), false);
        }
        source.sendSuccess(() -> Component.literal("§6Команды:"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match auto enable/disable"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match auto minplayers <N>"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match auto countdown <сек>"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match auto warmup <сек>"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match auto stats <сек>"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match auto voting <сек>"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAutoEnabled(CommandSourceStack source, boolean enabled) {
        MatchManager.get().setAutoStartEnabled(enabled);
        source.sendSuccess(() -> Component.literal("§aАвто-старт " + (enabled ? "включён" : "выключен")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setMinPlayers(CommandSourceStack source, int count) {
        MatchManager.get().setMinPlayersToStart(count);
        source.sendSuccess(() -> Component.literal("§aМинимум игроков для старта: " + count), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAutoCountdown(CommandSourceStack source, int seconds) {
        MatchManager.get().setAutoStartCountdown(seconds);
        source.sendSuccess(() -> Component.literal("§aОтсчёт до авто-старта: " + seconds + " сек"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setWarmup(CommandSourceStack source, int seconds) {
        MatchManager.get().setWarmupAfterJoin(seconds);
        source.sendSuccess(() -> Component.literal("§aОжидание после присоединения: " + seconds + " сек"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setStatsTime(CommandSourceStack source, int seconds) {
        MatchManager.get().setStatsDisplayTime(seconds);
        source.sendSuccess(() -> Component.literal("§aПоказ статистики: " + seconds + " сек"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setVotingTime(CommandSourceStack source, int seconds) {
        MatchManager.get().setVotingTime(seconds);
        source.sendSuccess(() -> Component.literal("§aВремя голосования: " + seconds + " сек"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int startMatch(CommandSourceStack source, int countdown) {
        MatchManager manager = MatchManager.get();

        if (manager.getState() != MatchState.WAITING_FOR_PLAYERS) {
            source.sendFailure(Component.literal("§cМатч уже запущен! Сначала остановите его: /pjm match stop"));
            return 0;
        }

        if (countdown > 0) {
            manager.startCountdown(countdown);
            source.sendSuccess(() -> Component.literal("§aМатч начнётся через " + countdown + " секунд!"), true);
            source.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§6[Матч] §eМатч начнётся через " + countdown + " секунд!"), false);
        } else {
            manager.forceStart(source.getServer());
            source.sendSuccess(() -> Component.literal("§aМатч принудительно запущен!"), true);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int stopMatch(CommandSourceStack source) {
        MatchManager manager = MatchManager.get();
        manager.resetMatch();

        // Телепортируем всех в лобби
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            MapManager.teleportToLobby(player);
        }

        source.sendSuccess(() -> Component.literal("§aМатч остановлен. Все игроки перемещены в лобби."), true);
        source.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§6[Матч] §cМатч был остановлен администратором."), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int setState(CommandSourceStack source, String stateName) {
        try {
            MatchState newState = MatchState.valueOf(stateName.toUpperCase());
            MatchManager.get().setState(newState);
            source.sendSuccess(() -> Component.literal("§aСостояние изменено на: §e" + newState.name()), true);
            return Command.SINGLE_SUCCESS;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("§cНеизвестное состояние: " + stateName));
            source.sendFailure(Component.literal("§7Доступные: " +
                    Arrays.stream(MatchState.values()).map(Enum::name).collect(Collectors.joining(", "))));
            return 0;
        }
    }

    private static int setTimer(CommandSourceStack source, int seconds) {
        MatchManager.get().setTimer(seconds * 20); // Конвертируем в тики
        source.sendSuccess(() -> Component.literal("§aТаймер установлен на " + seconds + " секунд"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int addTimer(CommandSourceStack source, int seconds) {
        int current = MatchManager.get().getTimer();
        MatchManager.get().setTimer(current + seconds * 20);
        int newSeconds = (current + seconds * 20) / 20;
        source.sendSuccess(
                () -> Component.literal("§aДобавлено " + seconds + " сек. Новый таймер: " + newSeconds + " сек"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int loadMap(CommandSourceStack source, String mapId) {
        MapConfig config = MapConfigManager.getConfig(mapId);
        if (config == null) {
            source.sendFailure(Component.literal("§cКарта не найдена: " + mapId));
            return 0;
        }

        MatchManager.get().setCurrentMap(mapId);
        source.sendSuccess(
                () -> Component.literal("§aКарта загружена: §e" + config.getDisplayName() + " §7(" + mapId + ")"),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listMaps(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Доступные карты ==="), false);

        for (String mapId : MapConfigManager.getAvailableMapIds()) {
            MapConfig config = MapConfigManager.getConfig(mapId);
            String displayName = config != null ? config.getDisplayName() : mapId;
            int roundTime = config != null ? config.getRoundTimeSeconds() : 0;
            source.sendSuccess(
                    () -> Component.literal("§e" + mapId + " §7- " + displayName + " (" + roundTime + " сек)"), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int reloadMaps(CommandSourceStack source) {
        MapConfigManager.reloadConfigs();
        int count = MapConfigManager.getAvailableMapIds().size();
        source.sendSuccess(() -> Component.literal("§aКонфигурации карт перезагружены. Найдено карт: " + count), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportAllToLobby(CommandSourceStack source) {
        int count = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            MapManager.teleportToLobby(player);
            count++;
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("§aТелепортировано в лобби: " + finalCount + " игроков"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportAllToMap(CommandSourceStack source) {
        int count = 0;
        String currentMapId = MatchManager.get().getCurrentMapId();

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            String team = MatchManager.get().getPlayerTeam(player.getUUID());
            if (team == null)
                team = "spectator";
            MapManager.teleportToMap(player, currentMapId, team);
            count++;
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("§aТелепортировано на карту: " + finalCount + " игроков"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int showStatus(CommandSourceStack source) {
        MatchManager manager = MatchManager.get();
        MatchState state = manager.getState();
        int timerSeconds = manager.getTimer() / 20;
        String mapId = manager.getCurrentMapId();
        int playerCount = source.getServer().getPlayerList().getPlayerCount();

        MapConfig mapConfig = MapConfigManager.getConfig(mapId);
        String mapName = mapConfig != null ? mapConfig.getDisplayName() : mapId;

        source.sendSuccess(() -> Component.literal("§6=== Статус Матча ==="), false);
        source.sendSuccess(() -> Component.literal("§7Состояние: §e" + state.name()), false);
        source.sendSuccess(() -> Component.literal("§7Таймер: §e" + formatTime(timerSeconds)), false);
        source.sendSuccess(() -> Component.literal("§7Карта: §e" + mapName + " §7(" + mapId + ")"), false);
        source.sendSuccess(() -> Component.literal("§7Игроков онлайн: §e" + playerCount), false);

        // Команды
        String team1 = ru.liko.pjmbasemod.Config.getTeam1Name();
        String team2 = ru.liko.pjmbasemod.Config.getTeam2Name();
        long t1Count = manager.getPlayerTeams().values().stream().filter(t -> t.equals(team1)).count();
        long t2Count = manager.getPlayerTeams().values().stream().filter(t -> t.equals(team2)).count();
        source.sendSuccess(() -> Component.literal("§7Команды: §b" + team1 + " (" + t1Count + ") §7vs §c" + team2 + " (" + t2Count + ")"), false);

        // Ротация
        java.util.List<String> rotation = ru.liko.pjmbasemod.common.map.MapRotationManager.getRotation();
        source.sendSuccess(() -> Component.literal("§7Ротация: §e" + rotation.size() + " карт"), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int listRotation(CommandSourceStack source) {
        java.util.List<String> rotation = ru.liko.pjmbasemod.common.map.MapRotationManager.getRotation();
        source.sendSuccess(() -> Component.literal("§6=== Ротация карт (" + rotation.size() + ") ==="), false);
        for (int i = 0; i < rotation.size(); i++) {
            String id = rotation.get(i);
            MapConfig cfg = MapConfigManager.getConfig(id);
            String name = cfg != null ? cfg.getDisplayName() : id;
            boolean isCurrent = id.equals(MatchManager.get().getCurrentMapId());
            String prefix = isCurrent ? "§a▸ " : "§7  ";
            int idx = i + 1;
            source.sendSuccess(() -> Component.literal(prefix + idx + ". §e" + id + " §7- " + name), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addToRotation(CommandSourceStack source, String mapId) {
        ru.liko.pjmbasemod.common.map.MapRotationManager.addMap(mapId);
        source.sendSuccess(() -> Component.literal("§aКарта '" + mapId + "' добавлена в ротацию."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int removeFromRotation(CommandSourceStack source, String mapId) {
        boolean removed = ru.liko.pjmbasemod.common.map.MapRotationManager.removeMap(mapId);
        if (removed) {
            source.sendSuccess(() -> Component.literal("§aКарта '" + mapId + "' удалена из ротации."), true);
        } else {
            source.sendFailure(Component.literal("§cКарта '" + mapId + "' не найдена в ротации."));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadRotation(CommandSourceStack source) {
        ru.liko.pjmbasemod.common.map.MapRotationManager.load();
        int count = ru.liko.pjmbasemod.common.map.MapRotationManager.getRotation().size();
        source.sendSuccess(() -> Component.literal("§aРотация перезагружена. Карт: " + count), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int skipPhase(CommandSourceStack source) {
        MatchManager manager = MatchManager.get();
        MatchState current = manager.getState();

        MatchState next = switch (current) {
            case WAITING_FOR_PLAYERS -> MatchState.STARTING;
            case STARTING -> MatchState.IN_PROGRESS;
            case IN_PROGRESS -> MatchState.SHOWING_STATS;
            case SHOWING_STATS -> MatchState.VOTING;
            case VOTING -> MatchState.WAITING_FOR_PLAYERS;
            case ENDING -> MatchState.WAITING_FOR_PLAYERS;
        };

        manager.setState(next);
        source.sendSuccess(() -> Component.literal("§aФаза пропущена: §e" + current.name() + " §7→ §a" + next.name()),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int endMatch(CommandSourceStack source, String reason) {
        MatchManager manager = MatchManager.get();

        if (manager.getState() == MatchState.WAITING_FOR_PLAYERS) {
            source.sendFailure(Component.literal("§cМатч не запущен."));
            return 0;
        }

        manager.setState(MatchState.SHOWING_STATS);
        manager.setTimer(manager.getStatsDisplayTime() * 20);
        manager.sendMatchStatsToAll(source.getServer(), reason);
        source.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§6[Матч] §cМатч завершён: " + reason), false);
        source.sendSuccess(() -> Component.literal("§aМатч завершён. Причина: " + reason), true);

        return Command.SINGLE_SUCCESS;
    }

    private static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
