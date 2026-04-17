package ru.liko.pjmbasemod.common.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.common.match.MatchManager;
import ru.liko.pjmbasemod.common.match.PlayerMatchStats;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * HTTP клиент для отправки игровой статистики на внешний API сервер.
 * Все запросы выполняются асинхронно — не блокируют серверный тик.
 * 
 * Эндпоинты:
 * - POST /stats/player — полная статистика игрока (при logout)
 * - POST /events/kill — событие убийства (реальное время)
 * - POST /events/match-end — итоги матча
 */
public class StatsApi {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private static HttpClient httpClient;
    private static ExecutorService executor;
    private static boolean active = false;

    // --- Circuit Breaker ---
    // After MAX_CONSECUTIVE_FAILURES consecutive failed requests, pause sending for
    // CIRCUIT_BREAK_MS.
    // This prevents spamming a dead API server with requests that will all timeout.
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final long CIRCUIT_BREAK_MS = 60_000L; // 1 minute cooldown
    private static int consecutiveFailures = 0;
    private static long circuitBrokenUntil = 0L;

    private StatsApi() {
    }

    /**
     * Инициализация HTTP клиента. Вызывается при старте сервера.
     */
    public static void init() {
        if (active)
            return;

        if (!StatsApiConfig.isEnabled()) {
            LOGGER.info("[StatsAPI] Disabled in config, skipping initialization");
            return;
        }

        executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "StatsAPI-Worker");
            t.setDaemon(true);
            return t;
        });

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(StatsApiConfig.getTimeoutSeconds()))
                .executor(executor)
                .build();

        active = true;
        LOGGER.info("[StatsAPI] Initialized. Target: {}", StatsApiConfig.getApiUrl());
    }

    /**
     * Завершение работы HTTP клиента. Вызывается при остановке сервера.
     */
    public static void shutdown() {
        if (!active)
            return;
        active = false;

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        httpClient = null;
        executor = null;
        LOGGER.info("[StatsAPI] Shutdown complete");
    }

    // ====================== PUBLIC API ======================

    /**
     * Отправляет полную статистику игрока на бекенд.
     * Используется для разовой отправки всего массива данных игрока.
     */
    public static void sendFullStats(ServerPlayer player) {
        if (!canSend() || !StatsApiConfig.isSendOnLogout())
            return;

        try {
            PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
            PlayerMatchStats matchStats = MatchManager.get().getStatsFor(player.getUUID());
            String team = MatchManager.get().getPlayerTeam(player.getUUID());

            JsonObject json = new JsonObject();
            json.addProperty("username", player.getName().getString());
            json.addProperty("uuid", player.getUUID().toString());

            // Опциональные поля
            json.addProperty("kills", matchStats.getKills());
            json.addProperty("deaths", matchStats.getDeaths());
            // wins and losses are tracked via single events or match end, but we can send
            // current score if needed
            json.addProperty("damageDealt", matchStats.getScore()); // Example placeholder if tracking damage

            // Если есть дополнительные поля в PjmPlayerData:
            json.addProperty("level", data.getRankPoints() / 100); // Пример расчета уровня
            json.addProperty("experience", data.getRankPoints());
            json.addProperty("faction", team != null ? team : data.getTeam());
            json.addProperty("isOnline", false); // Так как обычно вызывается при логауте

            postAsync("/api/minecraft/stats", json);
        } catch (Exception e) {
            LOGGER.error("[StatsAPI] Failed to build player stats payload", e);
        }
    }

    /**
     * Отправляет одиночное событие (join, leave, kill, death, win, loss) на бекенд.
     *
     * @param player      игрок, с которым произошло событие
     * @param eventType   тип события ("kill", "death", "join", "leave", "win",
     *                    "loss")
     * @param damageDealt урон нанесенный (опционально, передайте null если не
     *                    применимо)
     * @param damageTaken урон полученный (опционально, передайте null если не
     *                    применимо)
     */
    public static void sendSingleEvent(ServerPlayer player, String eventType, Number damageDealt, Number damageTaken) {
        if (!canSend())
            return;

        try {
            JsonObject json = new JsonObject();
            json.addProperty("username", player.getName().getString());
            json.addProperty("uuid", player.getUUID().toString());
            json.addProperty("event", eventType);

            if (damageDealt != null) {
                json.addProperty("damageDealt", damageDealt);
            }
            if (damageTaken != null) {
                json.addProperty("damageTaken", damageTaken);
            }

            postAsync("/api/minecraft/event", json);
        } catch (Exception e) {
            LOGGER.error("[StatsAPI] Failed to build single event payload for {} ({})", eventType,
                    player.getName().getString(), e);
        }
    }

    /**
     * Отправляет итоги матча всем игрокам.
     * 
     * @param winnerTeam  Название победившей команды
     * @param playerTeams Map UUID игроков к их командам
     * @param server      MinecraftServer инстанс
     */
    public static void processMatchEndForStats(String winnerTeam, Map<UUID, String> playerTeams,
            net.minecraft.server.MinecraftServer server) {
        if (!canSend() || !StatsApiConfig.isSendOnMatchEnd())
            return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String team = playerTeams.getOrDefault(player.getUUID(), "spectator");
            if (!"spectator".equals(team)) {
                String result = team.equals(winnerTeam) ? "win" : "loss";
                sendSingleEvent(player, result, null, null);
            }
        }
    }

    // ====================== INTERNAL ======================

    private static boolean canSend() {
        if (!active || httpClient == null || !StatsApiConfig.isEnabled())
            return false;

        // Circuit breaker: skip sending if too many recent failures
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            long now = System.currentTimeMillis();
            if (now < circuitBrokenUntil) {
                return false; // Still in cooldown
            }
            // Cooldown expired — reset and allow retry
            consecutiveFailures = 0;
            LOGGER.info("[StatsAPI] Circuit breaker reset, resuming requests");
        }
        return true;
    }

    /**
     * Асинхронный POST запрос к API.
     * Не блокирует вызывающий поток. Ошибки логируются.
     */
    private static void postAsync(String endpoint, JsonObject body) {
        String url = StatsApiConfig.getApiUrl() + endpoint;
        String jsonBody = GSON.toJson(body);

        if (StatsApiConfig.isDebugLogging()) {
            LOGGER.info("[StatsAPI] POST {} | Body: {}", url, jsonBody);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(StatsApiConfig.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        // Авторизация через Bearer token
        String apiKey = StatsApiConfig.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = requestBuilder.build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Success — reset circuit breaker
                    consecutiveFailures = 0;

                    if (StatsApiConfig.isDebugLogging()) {
                        LOGGER.info("[StatsAPI] Response {} from {} | Body: {}",
                                response.statusCode(), endpoint, response.body());
                    }
                    if (response.statusCode() >= 400) {
                        LOGGER.warn("[StatsAPI] HTTP {} from POST {} | Response: {}",
                                response.statusCode(), endpoint, response.body());
                    }
                })
                .exceptionally(ex -> {
                    // Failure — increment circuit breaker counter
                    consecutiveFailures++;
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        circuitBrokenUntil = System.currentTimeMillis() + CIRCUIT_BREAK_MS;
                        LOGGER.warn("[StatsAPI] Circuit breaker activated after {} failures. Pausing for {}s",
                                consecutiveFailures, CIRCUIT_BREAK_MS / 1000);
                    } else {
                        LOGGER.warn("[StatsAPI] Request failed ({}/{}): POST {} | Error: {}",
                                consecutiveFailures, MAX_CONSECUTIVE_FAILURES, endpoint, ex.getMessage());
                    }
                    return null;
                });
    }

    /**
     * Извлекает имя оружия из DamageSource/предмета в руке убийцы.
     * Используется для передачи в sendKillEvent.
     */
    public static String extractWeaponName(ServerPlayer killer) {
        try {
            ItemStack mainHand = killer.getMainHandItem();
            if (!mainHand.isEmpty()) {
                return net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(mainHand.getItem()).toString();
            }
        } catch (Exception ignored) {
        }
        return "minecraft:air";
    }
}
