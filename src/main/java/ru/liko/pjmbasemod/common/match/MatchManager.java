package ru.liko.pjmbasemod.common.match;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.common.map.MapManager;
import ru.liko.pjmbasemod.common.map.config.MapConfig;
import ru.liko.pjmbasemod.common.map.config.MapConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Центральный менеджер матчей.
 * Отвечает за состояние игры, таймеры и переходы между фазами.
 */
public class MatchManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MatchManager INSTANCE;

    private MatchState currentState = MatchState.WAITING_FOR_PLAYERS;
    private int stateTimer = 0; // Таймер фазы в тиках
    private String currentMapId = "default"; // ID текущей карты

    // Хранение выбора команды игроками: UUID -> TeamID
    private final Map<UUID, String> playerTeams = new HashMap<>();

    // Config текущей карты
    private MapConfig currentMapConfig;

    // --- PLAYER STATISTICS ---
    private final Map<UUID, PlayerMatchStats> playerStats = new HashMap<>();

    // --- TEAM TICKETS ---
    private final Map<String, Integer> teamTickets = new HashMap<>();
    private int defaultTickets = 300;

    // --- ASSIST TRACKING ---
    private final Map<UUID, Map<UUID, Long>> recentDamage = new HashMap<>(); // victim -> (attacker -> timestamp)
    private static final long ASSIST_WINDOW_MS = 10_000; // 10 секунд окно для ассиста

    // --- MAP VOTING ---
    private final Map<String, Integer> mapVotes = new HashMap<>(); // MapID -> Vote count
    private final Map<UUID, String> playerVotes = new HashMap<>(); // Player -> MapID they voted for

    // --- TIME WARNINGS ---
    private boolean warned5min = false;
    private boolean warned1min = false;
    private boolean warned30sec = false;

    // --- AUTOMATION SETTINGS ---
    private boolean autoStartEnabled = true;
    private int minPlayersToStart = 2; // Минимум игроков для авто-старта
    private int autoStartCountdown = 30; // Секунды до старта после достижения минимума
    private int warmupAfterJoin = 15; // Секунды ожидания после присоединения игрока
    private int statsDisplayTime = 20; // Секунды показа статистики
    private int votingTime = 30; // Секунды на голосование

    private int autoStartTimer = 0; // Внутренний таймер авто-старта
    private int lastPlayerCount = 0; // Для отслеживания новых игроков
    private int syncTimer = 0; // Таймер синхронизации (каждые 20 тиков)

    private MatchManager() {
        loadSettings();
    }

    public static MatchManager get() {
        if (INSTANCE == null) {
            INSTANCE = new MatchManager();
        }
        return INSTANCE;
    }

    /**
     * Сбрасывает синглтон при остановке сервера (integrated server может
     * перезапускаться).
     */
    public static void resetInstance() {
        INSTANCE = null;
    }

    /**
     * Вызывается каждый тик сервера (из EventHandler).
     */
    public void tick(MinecraftServer server) {
        int playerCount = server.getPlayerList().getPlayerCount();

        // Логика "Пустого сервера"
        if (playerCount == 0) {
            if (currentState != MatchState.WAITING_FOR_PLAYERS) {
                LOGGER.info("Server is empty. Resetting match.");
                resetMatch();
            }
            lastPlayerCount = 0;
            autoStartTimer = 0;
            return;
        }

        // Обновление таймеров
        if (stateTimer > 0) {
            stateTimer--;
        }
        if (autoStartTimer > 0) {
            autoStartTimer--;
        }

        // Периодическая синхронизация с клиентами (каждую секунду)
        syncTimer++;
        if (syncTimer >= 20) {
            syncTimer = 0;
            syncToAllClients();
        }

        // Логика состояний
        switch (currentState) {
            case WAITING_FOR_PLAYERS:
                tickWaitingForPlayers(server, playerCount);
                break;

            case STARTING:
                tickStarting(server, playerCount);
                break;

            case IN_PROGRESS:
                tickMatchProgress(server);
                break;

            case SHOWING_STATS:
                if (stateTimer <= 0) {
                    stateTimer = votingTime * 20;
                    setState(MatchState.VOTING);  // setState sends packet with correct timer now
                    broadcastMessage(server,
                            "§6[Матч] §eГолосование за следующую карту! Время: " + votingTime + " сек");
                    // Отправляем пакет открытия экрана голосования всем клиентам
                    ru.liko.pjmbasemod.common.network.PjmNetworking.sendToAll(
                            ru.liko.pjmbasemod.common.network.packet.OpenMapVotingPacket.fromServer());
                }
                break;

            case VOTING:
                if (stateTimer <= 0) {
                    String winningMapId = getWinningMap();
                    broadcastMessage(server, "§6[Матч] §aВыбрана карта: §e" + winningMapId);
                    loadNextMap(server, winningMapId);
                }
                break;

            case ENDING:
                if (stateTimer <= 0) {
                    setState(MatchState.WAITING_FOR_PLAYERS);
                    broadcastMessage(server, "§6[Матч] §aДобро пожаловать в лобби! Ожидание игроков...");
                }
                break;
        }

        // Обновляем счётчик игроков
        lastPlayerCount = playerCount;
    }

    /**
     * Логика ожидания игроков с авто-стартом.
     */
    private void tickWaitingForPlayers(MinecraftServer server, int playerCount) {
        if (!autoStartEnabled)
            return;

        // Периодическая очистка от "призраков" (раз в 5 секунд)
        if (server.getTickCount() % 100 == 0) {
            cleanupOfflinePlayers(server);
        }

        // Новый игрок присоединился - сброс таймера (даём время на подготовку)
        if (playerCount > lastPlayerCount && autoStartTimer > warmupAfterJoin * 20) {
            autoStartTimer = warmupAfterJoin * 20;
            broadcastMessage(server, "§6[Матч] §eНовый игрок присоединился! Ожидание: " + warmupAfterJoin + " сек");
        }

        // Проверяем что в ОБЕИХ командах есть игроки
        String team1 = ru.liko.pjmbasemod.Config.getTeam1Name();
        String team2 = ru.liko.pjmbasemod.Config.getTeam2Name();
        long team1Count = playerTeams.values().stream().filter(t -> t.equals(team1)).count();
        long team2Count = playerTeams.values().stream().filter(t -> t.equals(team2)).count();
        boolean teamsReady = team1Count >= 1 && team2Count >= 1;

        // Достаточно игроков для старта (общее количество + обе команды заполнены)
        if (playerCount >= minPlayersToStart && teamsReady) {
            // Запускаем таймер если ещё не запущен
            if (autoStartTimer <= 0 && (lastPlayerCount < minPlayersToStart || autoStartTimer == 0)) {
                autoStartTimer = autoStartCountdown * 20;
                broadcastMessage(server,
                        "§6[Матч] §aКоманды готовы! Матч начнётся через " + autoStartCountdown + " сек");
            }

            // Таймер истёк - начинаем матч
            if (autoStartTimer <= 0) {
                startCountdown(10); // 10 сек финальный отсчёт
                broadcastMessage(server, "§6[Матч] §c§lМАТЧ НАЧИНАЕТСЯ! Приготовьтесь!");
            }

            // Периодические напоминания
            if (autoStartTimer > 0 && autoStartTimer % (5 * 20) == 0) {
                int secondsLeft = autoStartTimer / 20;
                broadcastMessage(server, "§6[Матч] §eДо старта: " + secondsLeft + " сек. [" +
                        team1 + ": " + team1Count + "] [" + team2 + ": " + team2Count + "]");
            }
        } else {
            // Недостаточно игроков или команды не заполнены - сброс таймера
            if (autoStartTimer > 0) {
                autoStartTimer = 0;
                if (!teamsReady) {
                    broadcastMessage(server,
                            "§6[Матч] §cНужны игроки в обеих командах! [" +
                                    team1 + ": " + team1Count + "] [" + team2 + ": " + team2Count + "]");
                } else {
                    broadcastMessage(server,
                            "§6[Матч] §cНедостаточно игроков. Ожидание... (нужно: " + minPlayersToStart + ")");
                }
            }
        }
    }

    /**
     * Логика отсчёта до старта.
     */
    private void tickStarting(MinecraftServer server, int playerCount) {
        // Если игроков стало недостаточно - отмена
        if (autoStartEnabled && playerCount < minPlayersToStart) {
            setState(MatchState.WAITING_FOR_PLAYERS);
            autoStartTimer = 0;
            broadcastMessage(server, "§6[Матч] §cСтарт отменён! Недостаточно игроков.");
            return;
        }

        // Уведомления при отсчёте
        int secondsLeft = stateTimer / 20;
        if (stateTimer > 0 && stateTimer % 20 == 0) {
            if (secondsLeft <= 5 || secondsLeft == 10) {
                broadcastMessage(server, "§6[Матч] §e" + secondsLeft + "...");
            }
        }

        // Старт!
        if (stateTimer <= 0) {
            startMatch(server);
        }
    }

    /**
     * Отправка состояния всем клиентам.
     */
    private void syncToAllClients() {
        ru.liko.pjmbasemod.common.network.PjmNetworking
                .sendToAll(new ru.liko.pjmbasemod.common.network.packet.SyncMatchStatePacket(currentState, stateTimer));

        // Синхронизация тикетов команд
        if (teamTickets.size() >= 2) {
            String t1 = ru.liko.pjmbasemod.Config.getTeam1Name();
            String t2 = ru.liko.pjmbasemod.Config.getTeam2Name();
            ru.liko.pjmbasemod.common.network.PjmNetworking.sendToAll(
                    new ru.liko.pjmbasemod.common.network.packet.SyncTeamTicketsPacket(
                            t1, teamTickets.getOrDefault(t1, 0),
                            t2, teamTickets.getOrDefault(t2, 0)));
        }
    }

    /**
     * Широковещательное сообщение.
     */
    private void broadcastMessage(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }

    private void tickMatchProgress(MinecraftServer server) {
        // Предупреждения о времени
        if (stateTimer > 0) {
            int secondsLeft = stateTimer / 20;
            if (!warned5min && secondsLeft <= 300 && secondsLeft > 60) {
                warned5min = true;
                broadcastMessage(server, "§6[Матч] §eОсталось 5 минут!");
            }
            if (!warned1min && secondsLeft <= 60 && secondsLeft > 30) {
                warned1min = true;
                broadcastMessage(server, "§6[Матч] §cОсталась 1 минута!");
            }
            if (!warned30sec && secondsLeft <= 30 && secondsLeft > 0) {
                warned30sec = true;
                broadcastMessage(server, "§6[Матч] §c§lОсталось 30 секунд!");
            }
        }

        // Проверка условий победы по времени
        if (stateTimer <= 0) {
            String winner = getTeamWithMostTickets();
            endMatch(server, "Время вышло! Победа: " + winner);
            return;
        }

        // Проверка условий победы по тикетам
        for (Map.Entry<String, Integer> entry : teamTickets.entrySet()) {
            if (entry.getValue() <= 0) {
                String loser = entry.getKey();
                String winner = teamTickets.keySet().stream()
                        .filter(t -> !t.equals(loser))
                        .findFirst().orElse("???");
                endMatch(server, "У команды " + loser + " закончились тикеты! Победа: " + winner);
                return;
            }
        }

        // Проверка захвата всех точек одной командой
        String dominantTeam = checkAllPointsCaptured(server);
        if (dominantTeam != null) {
            endMatch(server, "Команда " + dominantTeam + " захватила все точки!");
            return;
        }

        // Тикет-блид: команда с меньшим количеством точек теряет тикеты
        if (server.getTickCount() % 100 == 0) { // Каждые 5 секунд
            tickTicketBleed(server);
        }

        // Проверка баланса команд каждые 60 секунд
        if (server.getTickCount() % 1200 == 0) {
            checkAutoBalance(server);
        }
    }

    public void startLobby() {
        setState(MatchState.WAITING_FOR_PLAYERS);
        playerTeams.clear();
        // Загружаем конфиг карты по умолчанию или сохраненный
        if (currentMapConfig == null) {
            currentMapConfig = MapConfigManager.getConfig(currentMapId);
        }
    }

    public void startCountdown() {
        startCountdown(10);
    }

    public void startCountdown(int seconds) {
        setState(MatchState.STARTING);
        stateTimer = seconds * 20;
    }

    /**
     * Принудительный старт матча (для админов).
     */
    public void forceStart(MinecraftServer server) {
        startMatch(server);
    }

    private void startMatch(MinecraftServer server) {
        // Проверяем наличие конфига карты
        if (currentMapConfig == null) {
            currentMapConfig = MapConfigManager.getConfig(currentMapId);
        }
        if (currentMapConfig == null) {
            LOGGER.error("Cannot start match: no MapConfig for '{}'", currentMapId);
            broadcastMessage(server, "§c[Матч] Ошибка: конфиг карты '" + currentMapId + "' не найден!");
            setState(MatchState.WAITING_FOR_PLAYERS);
            return;
        }

        // Автосоздание динамического дименшона если указано в конфиге карты
        if (!ensureMapDimension(server)) {
            broadcastMessage(server, "§c[Матч] Ошибка: не удалось создать дименшон для карты!");
            setState(MatchState.WAITING_FOR_PLAYERS);
            return;
        }

        setState(MatchState.IN_PROGRESS);
        stateTimer = currentMapConfig.getRoundTimeSeconds() > 0
                ? currentMapConfig.getRoundTimeSeconds() * 20
                : 36000; // 30 min default

        // Сброс предупреждений о времени
        warned5min = false;
        warned1min = false;
        warned30sec = false;

        // Инициализация тикетов
        initTickets(server);

        // Применяем настройки карты (погода, время суток, контрольные точки)
        applyMapSettings(server);

        // Телепортация игроков с командами на позиции, остальных — в спектатор
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String team = playerTeams.get(player.getUUID());
            if (team != null && !team.isEmpty()) {
                MapManager.teleportToMap(player, currentMapId, team);
                player.sendSystemMessage(Component.literal("§aМатч начался! Цель: Захват точек."));
            } else {
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                player.sendSystemMessage(Component.literal("§7Вы наблюдатель. Выберите команду для участия."));
            }
        }
    }

    /**
     * Убеждается что дименшон карты существует.
     * Если карта использует dynamicDimension=true — создаёт через
     * DynamicDimensionManager.
     * 
     * @return true если дименшон готов к использованию
     */
    private boolean ensureMapDimension(MinecraftServer server) {
        if (currentMapConfig == null)
            return false;

        String dimName = currentMapConfig.getDynamicDimensionName();
        if (dimName == null) {
            // Ванильный или не-pjmbasemod дименшон — проверяем что он существует
            String dimString = currentMapConfig.getDimension();
            if (dimString == null || dimString.isEmpty())
                return true; // overworld fallback
            try {
                net.minecraft.resources.ResourceLocation dimId = net.minecraft.resources.ResourceLocation
                        .parse(dimString);
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = net.minecraft.resources.ResourceKey
                        .create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
                return server.getLevel(dimKey) != null;
            } catch (Exception e) {
                LOGGER.error("Invalid dimension '{}' in map config '{}'", dimString, currentMapId);
                return false;
            }
        }

        // pjmbasemod дименшон — проверяем/создаём через DynamicDimensionManager
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager
                .getResourceKey(dimName);

        if (server.getLevel(dimKey) != null) {
            LOGGER.info("Map dimension '{}' already exists", dimName);
            return true;
        }

        if (!currentMapConfig.isDynamicDimension()) {
            LOGGER.error("Dimension '{}' does not exist and dynamicDimension=false for map '{}'",
                    dimName, currentMapId);
            return false;
        }

        // Создаём дименшон
        ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.GenType genType = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.GenType
                .fromString(
                        currentMapConfig.getGenType());
        if (genType == null)
            genType = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.GenType.VOID;

        LOGGER.info("Auto-creating dynamic dimension '{}' (type: {}) for map '{}'",
                dimName, genType, currentMapId);
        return ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.createDimension(server, dimName, genType);
    }

    private void endMatch(MinecraftServer server, String reason) {
        setState(MatchState.SHOWING_STATS);
        stateTimer = statsDisplayTime * 20;

        server.getPlayerList().broadcastSystemMessage(Component.literal("§6[Матч] §cМатч завершён! " + reason), false);

        // Сохраняем историю матча
        logMatchResult(server, reason);

        // Определяем победителя и длительность для API
        String winnerTeam = getTeamWithMostTickets();

        // Отправляем итоги матча (win/loss для каждого) на бекенд (async)
        ru.liko.pjmbasemod.common.stats.StatsApi.processMatchEndForStats(
                winnerTeam, playerTeams, server);

        // Отправляем статистику всем игрокам
        sendMatchStatsToAll(server, reason);

        // Очищаем сущности в дименшоне карты (мобы, дропы, проектили и т.д.)
        clearMapDimensionEntities(server);

        // Телепортируем всех обратно в лобби + сброс кита/класса/ранга/инвентаря
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            resetPlayerForLobby(player);
            MapManager.teleportToLobby(player);
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        }
    }

    /**
     * Удаляет все не-игроковские сущности в дименшоне текущей карты.
     * Вызывается при окончании матча и при переходе к следующей карте.
     */
    private void clearMapDimensionEntities(MinecraftServer server) {
        net.minecraft.server.level.ServerLevel mapLevel = resolveMapLevel(server);
        if (mapLevel == null) return;
        int removed = 0;
        for (net.minecraft.world.entity.Entity entity : mapLevel.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.player.Player) continue;
            entity.discard();
            removed++;
        }
        if (removed > 0) {
            LOGGER.info("Cleared {} non-player entities from map dimension '{}'", removed, mapLevel.dimension().location());
        }
    }

    /**
     * Сбрасывает состояние игрока при возврате в лобби после окончания матча:
     * - очищает инвентарь
     * - сбрасывает класс и выбранный кит
     * - сбрасывает очки ранга и сам ранг до PRIVATE
     * - синхронизирует данные с клиентом
     */
    private void resetPlayerForLobby(ServerPlayer player) {
        // Очистка инвентаря (включая броню и оффхенд)
        player.getInventory().clearContent();

        ru.liko.pjmbasemod.common.player.PjmPlayerData data =
                player.getData(ru.liko.pjmbasemod.common.player.PjmAttachments.PLAYER_DATA);

        // Сброс класса и кита
        data.setPlayerClass(ru.liko.pjmbasemod.common.player.PjmPlayerClass.NONE);
        data.setSelectedKitId("");

        // Сброс ранга и очков
        data.setRankPoints(0);
        data.setRank(ru.liko.pjmbasemod.common.player.PjmRank.PRIVATE);

        // Сброс специальных слотов
        for (int i = 0; i < 4; i++) {
            data.setSpecialSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
        }

        // Синхронизация с клиентом и трекерами
        ru.liko.pjmbasemod.common.network.PjmNetworking.sendToClient(
                ru.liko.pjmbasemod.common.network.packet.SyncPjmDataPacket.fromPlayerData(player.getId(), data),
                player);
        ru.liko.pjmbasemod.common.network.PjmNetworking.sendToTracking(
                ru.liko.pjmbasemod.common.network.packet.SyncPjmDataPacket.fromPlayerData(player.getId(), data),
                player);

        // Анимация изменения ранга (demote) чтобы клиент отобразил сброс
        ru.liko.pjmbasemod.common.network.PjmNetworking.sendToClient(
                ru.liko.pjmbasemod.common.network.packet.RankUpdatePacket.create(
                        ru.liko.pjmbasemod.common.player.PjmRank.PRIVATE, false),
                player);
    }

    /**
     * Собирает и отправляет статистику матча всем подключённым игрокам.
     */
    public void sendMatchStatsToAll(MinecraftServer server, String reason) {
        List<ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket.PlayerStatsEntry> statsEntries = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            PlayerMatchStats stats = getStatsFor(uuid);
            String team = playerTeams.getOrDefault(uuid, "spectator");

            statsEntries.add(new ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket.PlayerStatsEntry(
                    player.getName().getString(),
                    team,
                    stats.getKills(),
                    stats.getDeaths(),
                    stats.getAssists(),
                    stats.getCapturePoints(),
                    stats.getScore()));
        }

        // Определяем победителя по сумме очков команд
        String winnerTeam = determineWinnerTeam(statsEntries);

        // Вычисляем длительность матча
        int matchDuration = 0;
        if (currentMapConfig != null) {
            int totalTicks = currentMapConfig.getRoundTimeSeconds() * 20;
            matchDuration = (totalTicks - stateTimer) / 20;
        }

        ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket packet = new ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket(
                winnerTeam, reason, matchDuration, statsEntries);

        ru.liko.pjmbasemod.common.network.PjmNetworking.sendToAll(packet);
    }

    /**
     * Отправляет статистику одному конкретному игроку.
     */
    public void sendMatchStatsToPlayer(ServerPlayer target, MinecraftServer server, String reason) {
        List<ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket.PlayerStatsEntry> statsEntries = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            PlayerMatchStats stats = getStatsFor(uuid);
            String team = playerTeams.getOrDefault(uuid, "spectator");

            statsEntries.add(new ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket.PlayerStatsEntry(
                    player.getName().getString(),
                    team,
                    stats.getKills(),
                    stats.getDeaths(),
                    stats.getAssists(),
                    stats.getCapturePoints(),
                    stats.getScore()));
        }

        String winnerTeam = determineWinnerTeam(statsEntries);

        ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket packet = new ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket(
                winnerTeam, reason, 0, statsEntries);

        ru.liko.pjmbasemod.common.network.PjmNetworking.sendToClient(packet, target);
    }

    /**
     * Определяет победившую команду по сумме очков.
     */
    private String determineWinnerTeam(
            List<ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket.PlayerStatsEntry> stats) {
        Map<String, Integer> teamScores = new HashMap<>();
        for (ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket.PlayerStatsEntry entry : stats) {
            if (!"spectator".equals(entry.team())) {
                teamScores.merge(entry.team(), entry.score(), Integer::sum);
            }
        }

        return teamScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    public void resetMatch() {
        setState(MatchState.WAITING_FOR_PLAYERS);
        playerTeams.clear();
        stateTimer = 0;
        clearVotes();
        // Полностью очищаем статистику при сбросе
        playerStats.clear();
        teamTickets.clear();
        recentDamage.clear();
        warned5min = false;
        warned1min = false;
        warned30sec = false;
    }

    /**
     * Удаляет данные офлайн-игроков из структур матча.
     * Предотвращает утечки памяти и зависание офлайн-игроков (призраков) в
     * командах.
     */
    private void cleanupOfflinePlayers(MinecraftServer server) {
        java.util.Set<UUID> onlinePlayers = server.getPlayerList().getPlayers().stream()
                .map(net.minecraft.server.level.ServerPlayer::getUUID)
                .collect(java.util.stream.Collectors.toSet());

        playerTeams.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
        playerStats.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
        playerVotes.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
        recentDamage.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));

        // Удаляем из статистики тоже
        playerStats.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
    }

    private void loadNextMap(MinecraftServer server, String mapId) {
        this.currentMapId = mapId;
        this.currentMapConfig = MapConfigManager.getConfig(mapId);

        if (currentMapConfig == null) {
            LOGGER.warn("Map config '{}' not found after voting, keeping current config", mapId);
        }

        broadcastMapInfo();

        // Очищаем оффлайн игроков, которые вышли во время матча и не вернулись
        cleanupOfflinePlayers(server);

        // Сбрасываем голоса, статистику, тикеты, ассисты — НО НЕ команды онлайн-игроков
        clearVotes();
        clearStats();
        teamTickets.clear();
        recentDamage.clear();

        // Очищаем контрольные точки от предыдущей карты
        ru.liko.pjmbasemod.common.gamemode.ControlPointManager cpManager = ru.liko.pjmbasemod.common.gamemode.ControlPointManager
                .get(server.overworld());
        cpManager.clear();

        // Очищаем сущности в дименшоне предыдущей карты
        clearMapDimensionEntities(server);

        // Переместить всех в лобби + сброс кита/класса/ранга/инвентаря
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            resetPlayerForLobby(player);
            MapManager.teleportToLobby(player);
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        }

        setState(MatchState.WAITING_FOR_PLAYERS);
        stateTimer = 0;

        String displayName = currentMapConfig != null ? currentMapConfig.getDisplayName() : mapId;
        broadcastMessage(server, "§6[Матч] §aСледующая карта: §e" + displayName + " §a| Ожидание игроков...");
    }

    public void setState(MatchState newState) {
        this.currentState = newState;
        ru.liko.pjmbasemod.common.network.PjmNetworking
                .sendToAll(new ru.liko.pjmbasemod.common.network.packet.SyncMatchStatePacket(currentState, stateTimer));
    }

    public MatchState getState() {
        return currentState;
    }

    public void playerJoinTeam(ServerPlayer player, String teamId) {
        playerTeams.put(player.getUUID(), teamId);
    }

    /**
     * Удаляет данные игрока при выходе с сервера.
     * Во время активного матча команда сохраняется для реконнекта.
     */
    public void playerLeave(UUID playerUUID) {
        // Во время матча не удаляем команду — игрок может переподключиться
        if (currentState == MatchState.WAITING_FOR_PLAYERS || currentState == MatchState.ENDING) {
            playerTeams.remove(playerUUID);
        }
        playerVotes.remove(playerUUID);
        // Статистику тоже сохраняем во время матча
        if (currentState == MatchState.WAITING_FOR_PLAYERS || currentState == MatchState.ENDING) {
            playerStats.remove(playerUUID);
        }
        recentDamage.remove(playerUUID);
    }

    public String getPlayerTeam(UUID playerParams) {
        return playerTeams.get(playerParams);
    }

    public Map<UUID, String> getPlayerTeams() {
        return java.util.Collections.unmodifiableMap(playerTeams);
    }

    // --- MAP VOTING ---

    /**
     * Called when a player votes for a map.
     */
    public void voteForMap(ServerPlayer player, String mapId) {
        UUID uuid = player.getUUID();
        // Remove previous vote if exists
        String previousVote = playerVotes.get(uuid);
        if (previousVote != null) {
            mapVotes.put(previousVote, Math.max(0, mapVotes.getOrDefault(previousVote, 0) - 1));
        }
        // Add new vote
        playerVotes.put(uuid, mapId);
        mapVotes.put(mapId, mapVotes.getOrDefault(mapId, 0) + 1);
    }

    /**
     * Returns the map with the most votes, or the next map from rotation if no
     * votes.
     */
    private String getWinningMap() {
        if (mapVotes.isEmpty()) {
            // Нет голосов — берём следующую карту из ротации
            return ru.liko.pjmbasemod.common.map.MapRotationManager.getNextMap(currentMapId);
        }
        return mapVotes.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(ru.liko.pjmbasemod.common.map.MapRotationManager.getNextMap(currentMapId));
    }

    /**
     * Clears votes for a new voting round.
     */
    private void clearVotes() {
        mapVotes.clear();
        playerVotes.clear();
    }

    // --- STATISTICS ---

    /**
     * Gets or creates stats for a player.
     */
    public PlayerMatchStats getStatsFor(UUID playerUUID) {
        return playerStats.computeIfAbsent(playerUUID, PlayerMatchStats::new);
    }

    /**
     * Records a kill event: updates stats, processes assists, deducts ticket.
     */
    public void recordKill(ServerPlayer killer, ServerPlayer victim) {
        if (currentState != MatchState.IN_PROGRESS)
            return;
        getStatsFor(killer.getUUID()).addKill();
        getStatsFor(victim.getUUID()).addDeath();

        // Ассисты
        processAssists(killer, victim);

        // В Squad-дизайне тикет снимается только при окончательной смерти (Give Up).
        // См. метод recordGiveUp()
    }

    /**
     * Records a final death (Give Up or bleeding out) and deducts a ticket.
     */
    public void recordGiveUp(ServerPlayer victim) {
        if (currentState != MatchState.IN_PROGRESS)
            return;

        String victimTeam = playerTeams.get(victim.getUUID());
        if (victimTeam != null && !victimTeam.isEmpty()) {
            deductTicket(victimTeam);
        }
    }

    /**
     * Clears all statistics for a new match.
     */
    private void clearStats() {
        playerStats.values().forEach(PlayerMatchStats::reset);
    }

    public int getTimer() {
        return stateTimer;
    }

    public void setTimer(int ticks) {
        this.stateTimer = ticks;
        ru.liko.pjmbasemod.common.network.PjmNetworking
                .sendToAll(new ru.liko.pjmbasemod.common.network.packet.SyncMatchStatePacket(currentState, stateTimer));
    }

    public void setCurrentMap(String mapId) {
        this.currentMapId = mapId;
        this.currentMapConfig = MapConfigManager.getConfig(mapId);
        broadcastMapInfo();
    }

    public void sendMapInfo(ServerPlayer player) {
        String displayName = currentMapConfig != null ? currentMapConfig.getDisplayName() : currentMapId;
        ru.liko.pjmbasemod.common.network.PjmNetworking.sendToClient(
                new ru.liko.pjmbasemod.common.network.packet.SyncMapInfoPacket(currentMapId, displayName),
                player);
    }

    private void broadcastMapInfo() {
        String displayName = currentMapConfig != null ? currentMapConfig.getDisplayName() : currentMapId;
        ru.liko.pjmbasemod.common.network.PjmNetworking.sendToAll(
                new ru.liko.pjmbasemod.common.network.packet.SyncMapInfoPacket(currentMapId, displayName));
    }

    public String getCurrentMapId() {
        return currentMapId;
    }

    // --- AUTOMATION SETTINGS GETTERS/SETTERS ---

    public boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(boolean enabled) {
        this.autoStartEnabled = enabled;
        saveSettings();
    }

    public int getMinPlayersToStart() {
        return minPlayersToStart;
    }

    public void setMinPlayersToStart(int count) {
        this.minPlayersToStart = Math.max(1, count);
        saveSettings();
    }

    public int getAutoStartCountdown() {
        return autoStartCountdown;
    }

    public void setAutoStartCountdown(int seconds) {
        this.autoStartCountdown = Math.max(5, seconds);
        saveSettings();
    }

    public int getWarmupAfterJoin() {
        return warmupAfterJoin;
    }

    public void setWarmupAfterJoin(int seconds) {
        this.warmupAfterJoin = Math.max(0, seconds);
        saveSettings();
    }

    public int getStatsDisplayTime() {
        return statsDisplayTime;
    }

    public void setStatsDisplayTime(int seconds) {
        this.statsDisplayTime = Math.max(5, seconds);
        saveSettings();
    }

    public int getVotingTime() {
        return votingTime;
    }

    public void setVotingTime(int seconds) {
        this.votingTime = Math.max(10, seconds);
        saveSettings();
    }

    public int getAutoStartTimer() {
        return autoStartTimer;
    }

    // --- TICKET SYSTEM ---

    private void initTickets(MinecraftServer server) {
        teamTickets.clear();
        String team1 = ru.liko.pjmbasemod.Config.getTeam1Name();
        String team2 = ru.liko.pjmbasemod.Config.getTeam2Name();
        teamTickets.put(team1, defaultTickets);
        teamTickets.put(team2, defaultTickets);
    }

    public void deductTicket(String teamId) {
        teamTickets.computeIfPresent(teamId, (k, v) -> Math.max(0, v - 1));
    }

    public int getTeamTickets(String teamId) {
        return teamTickets.getOrDefault(teamId, 0);
    }

    private String getTeamWithMostTickets() {
        return teamTickets.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("???");
    }

    private String checkAllPointsCaptured(MinecraftServer server) {
        net.minecraft.server.level.ServerLevel mapLevel = resolveMapLevel(server);
        ru.liko.pjmbasemod.common.gamemode.ControlPointManager cpManager = ru.liko.pjmbasemod.common.gamemode.ControlPointManager
                .get(mapLevel);
        if (cpManager.isEmpty())
            return null;

        Map<String, Integer> ownerCounts = new HashMap<>();
        int totalPoints = 0;
        for (ru.liko.pjmbasemod.common.gamemode.ControlPoint cp : cpManager.getAll()) {
            totalPoints++;
            String owner = cp.getOwnerTeam();
            if (owner != null && !owner.isEmpty()) {
                ownerCounts.merge(owner, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : ownerCounts.entrySet()) {
            if (entry.getValue() == totalPoints) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void tickTicketBleed(MinecraftServer server) {
        if (teamTickets.size() < 2)
            return;

        net.minecraft.server.level.ServerLevel mapLevel = resolveMapLevel(server);
        ru.liko.pjmbasemod.common.gamemode.ControlPointManager cpManager = ru.liko.pjmbasemod.common.gamemode.ControlPointManager
                .get(mapLevel);
        if (cpManager.isEmpty())
            return;

        Map<String, Integer> ownerCounts = new HashMap<>();
        for (ru.liko.pjmbasemod.common.gamemode.ControlPoint cp : cpManager.getAll()) {
            String owner = cp.getOwnerTeam();
            if (owner != null && !owner.isEmpty()) {
                ownerCounts.merge(owner, 1, Integer::sum);
            }
        }

        // Команда с меньшим числом точек теряет тикеты
        int maxPoints = ownerCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (String team : teamTickets.keySet()) {
            int ownedPoints = ownerCounts.getOrDefault(team, 0);
            if (ownedPoints < maxPoints) {
                int bleed = maxPoints - ownedPoints;
                teamTickets.computeIfPresent(team, (k, v) -> Math.max(0, v - bleed));
            }
        }
    }

    public int getDefaultTickets() {
        return defaultTickets;
    }

    public void setDefaultTickets(int tickets) {
        this.defaultTickets = Math.max(50, tickets);
        saveSettings();
    }

    public Map<String, Integer> getTeamTicketsMap() {
        return java.util.Collections.unmodifiableMap(teamTickets);
    }

    // --- ASSIST TRACKING ---

    public void recordDamage(ServerPlayer attacker, ServerPlayer victim) {
        if (currentState != MatchState.IN_PROGRESS)
            return;
        if (attacker.getUUID().equals(victim.getUUID()))
            return;
        recentDamage
                .computeIfAbsent(victim.getUUID(), k -> new HashMap<>())
                .put(attacker.getUUID(), System.currentTimeMillis());
    }

    private void processAssists(ServerPlayer killer, ServerPlayer victim) {
        Map<UUID, Long> damagers = recentDamage.remove(victim.getUUID());
        if (damagers == null)
            return;

        long now = System.currentTimeMillis();
        UUID killerUUID = killer.getUUID();

        for (Map.Entry<UUID, Long> entry : damagers.entrySet()) {
            if (entry.getKey().equals(killerUUID))
                continue;
            if (now - entry.getValue() <= ASSIST_WINDOW_MS) {
                getStatsFor(entry.getKey()).addAssist();
            }
        }
    }

    /**
     * Резолвит ServerLevel текущей карты из MapConfig.dimension.
     * Fallback на overworld если дименшон не найден.
     */
    private net.minecraft.server.level.ServerLevel resolveMapLevel(MinecraftServer server) {
        if (currentMapConfig == null)
            return server.overworld();

        String dimString = currentMapConfig.getDimension();
        if (dimString != null && !dimString.isEmpty()) {
            try {
                net.minecraft.resources.ResourceLocation dimId = net.minecraft.resources.ResourceLocation
                        .parse(dimString);
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = net.minecraft.resources.ResourceKey
                        .create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
                net.minecraft.server.level.ServerLevel level = server.getLevel(dimKey);
                if (level != null)
                    return level;
            } catch (Exception e) {
                LOGGER.warn("Could not resolve map dimension: {}", dimString);
            }
        }
        return server.overworld();
    }

    // --- WEATHER & CAPTURE POINTS SETUP ---

    private void applyMapSettings(MinecraftServer server) {
        if (currentMapConfig == null)
            return;

        // Определяем измерение карты через единый резолвер
        net.minecraft.server.level.ServerLevel gameLevel = resolveMapLevel(server);

        // Применяем погоду
        MapConfig.WeatherConfig weather = currentMapConfig.getWeather();
        if (weather != null) {
            if (weather.getTimeOfDay() >= 0) {
                gameLevel.setDayTime(weather.getTimeOfDay());
            }
            String weatherType = weather.getType();
            if (weatherType != null) {
                switch (weatherType.toLowerCase()) {
                    case "clear":
                        gameLevel.setWeatherParameters(6000, 0, false, false);
                        break;
                    case "rain":
                        gameLevel.setWeatherParameters(0, 6000, true, false);
                        break;
                    case "storm":
                        gameLevel.setWeatherParameters(0, 6000, true, true);
                        break;
                }
            }
        }

        // Очищаем старые контрольные точки перед загрузкой новых
        ru.liko.pjmbasemod.common.gamemode.ControlPointManager cpManager = ru.liko.pjmbasemod.common.gamemode.ControlPointManager
                .get(gameLevel);
        cpManager.clear();

        // Загружаем контрольные точки из конфига карты
        List<MapConfig.CapturePointConfig> cpConfigs = currentMapConfig.getCapturePoints();
        if (cpConfigs != null && !cpConfigs.isEmpty()) {

            for (MapConfig.CapturePointConfig cpCfg : cpConfigs) {
                double[] pos = cpCfg.getPos();
                if (pos == null || pos.length < 3)
                    continue;

                net.minecraft.world.phys.Vec3 center = new net.minecraft.world.phys.Vec3(pos[0], pos[1], pos[2]);
                double radius = cpCfg.getRadius() > 0 ? cpCfg.getRadius() : 10.0;

                ru.liko.pjmbasemod.common.gamemode.ControlPoint cp = new ru.liko.pjmbasemod.common.gamemode.ControlPoint(
                        cpCfg.getId(), cpCfg.getName(), gameLevel.dimension(), center, radius);

                if (cpCfg.getCaptureTime() > 0) {
                    cp.setCaptureTimeSeconds(cpCfg.getCaptureTime());
                }
                cp.setOwnerTeam("");
                cp.resetCapture();
                cpManager.addOrUpdate(cp);
            }
            LOGGER.info("Loaded {} capture points from map config '{}'", cpConfigs.size(), currentMapId);
        }
    }

    // --- AUTO-BALANCE ---

    public void checkAutoBalance(MinecraftServer server) {
        if (currentState != MatchState.IN_PROGRESS)
            return;

        String team1 = ru.liko.pjmbasemod.Config.getTeam1Name();
        String team2 = ru.liko.pjmbasemod.Config.getTeam2Name();
        int threshold = ru.liko.pjmbasemod.Config.getTeamBalanceThreshold();

        int count1 = 0, count2 = 0;
        for (String team : playerTeams.values()) {
            if (team.equals(team1))
                count1++;
            else if (team.equals(team2))
                count2++;
        }

        int diff = Math.abs(count1 - count2);
        if (diff >= threshold) {
            String bigTeam = count1 > count2 ? team1 : team2;
            String smallTeam = count1 > count2 ? team2 : team1;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String pTeam = playerTeams.get(player.getUUID());
                if (bigTeam.equals(pTeam)) {
                    player.sendSystemMessage(Component.literal(
                            "§e[Баланс] §fКоманды неравны (" + count1 + " vs " + count2 +
                                    "). Рассмотрите переход в §a" + smallTeam));
                }
            }
        }
    }

    // --- SETTINGS PERSISTENCE ---

    private static final java.nio.file.Path SETTINGS_FILE = java.nio.file.Paths.get("config", "pjmbasemod",
            "match_settings.json");

    public void saveSettings() {
        try {
            java.nio.file.Files.createDirectories(SETTINGS_FILE.getParent());
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("autoStartEnabled", autoStartEnabled);
            json.addProperty("minPlayersToStart", minPlayersToStart);
            json.addProperty("autoStartCountdown", autoStartCountdown);
            json.addProperty("warmupAfterJoin", warmupAfterJoin);
            json.addProperty("statsDisplayTime", statsDisplayTime);
            json.addProperty("votingTime", votingTime);
            json.addProperty("defaultTickets", defaultTickets);
            java.nio.file.Files.writeString(SETTINGS_FILE,
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
            LOGGER.info("Match settings saved to {}", SETTINGS_FILE);
        } catch (Exception e) {
            LOGGER.error("Failed to save match settings", e);
        }
    }

    private void loadSettings() {
        if (!java.nio.file.Files.exists(SETTINGS_FILE))
            return;
        try {
            String content = java.nio.file.Files.readString(SETTINGS_FILE);
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            if (json.has("autoStartEnabled"))
                autoStartEnabled = json.get("autoStartEnabled").getAsBoolean();
            if (json.has("minPlayersToStart"))
                minPlayersToStart = json.get("minPlayersToStart").getAsInt();
            if (json.has("autoStartCountdown"))
                autoStartCountdown = json.get("autoStartCountdown").getAsInt();
            if (json.has("warmupAfterJoin"))
                warmupAfterJoin = json.get("warmupAfterJoin").getAsInt();
            if (json.has("statsDisplayTime"))
                statsDisplayTime = json.get("statsDisplayTime").getAsInt();
            if (json.has("votingTime"))
                votingTime = json.get("votingTime").getAsInt();
            if (json.has("defaultTickets"))
                defaultTickets = json.get("defaultTickets").getAsInt();
            LOGGER.info("Match settings loaded from {}", SETTINGS_FILE);
        } catch (Exception e) {
            LOGGER.error("Failed to load match settings", e);
        }
    }

    // --- MATCH HISTORY ---

    private void logMatchResult(MinecraftServer server, String reason) {
        try {
            java.nio.file.Path historyDir = java.nio.file.Paths.get("config", "pjmbasemod", "match_history");
            java.nio.file.Files.createDirectories(historyDir);

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            java.nio.file.Path file = historyDir.resolve("match_" + timestamp + ".txt");

            List<String> lines = new ArrayList<>();
            lines.add("=== Match Result ===");
            lines.add("Map: " + currentMapId);
            lines.add("Reason: " + reason);
            lines.add("Time: " + timestamp);
            lines.add("");

            // Тикеты
            for (Map.Entry<String, Integer> entry : teamTickets.entrySet()) {
                lines.add("Team " + entry.getKey() + " tickets: " + entry.getValue());
            }
            lines.add("");

            // Статистика игроков
            lines.add(String.format("%-20s %-10s %6s %6s %6s %6s %6s",
                    "Player", "Team", "Kills", "Deaths", "Assists", "Caps", "Score"));
            lines.add("-".repeat(76));

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                PlayerMatchStats stats = getStatsFor(uuid);
                String team = playerTeams.getOrDefault(uuid, "spec");
                lines.add(String.format("%-20s %-10s %6d %6d %6d %6d %6d",
                        player.getName().getString(), team,
                        stats.getKills(), stats.getDeaths(), stats.getAssists(),
                        stats.getCapturePoints(), stats.getScore()));
            }

            java.nio.file.Files.write(file, lines);
            LOGGER.info("Match history saved to {}", file);
        } catch (Exception e) {
            LOGGER.error("Failed to save match history", e);
        }
    }
}
