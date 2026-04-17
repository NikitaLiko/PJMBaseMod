package ru.liko.pjmbasemod.common.event;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;

import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.death.DeathTracker;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.SyncPjmDataPacket;
import ru.liko.pjmbasemod.common.permission.PjmPermissions;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper;
import ru.liko.pjmbasemod.common.util.TeamBalanceHelper;
import ru.liko.pjmbasemod.common.zone.ClassSelectionZoneManager;
import ru.liko.pjmbasemod.common.vehicle.VehicleSpawnPointManager;
import ru.liko.pjmbasemod.common.vehicle.VehicleSpawnSystem;
import ru.liko.pjmbasemod.common.network.packet.OpenTeamSelectionPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Common events for server-side functionality.
 * NeoForge 1.21.1: Uses Data Attachments instead of Capabilities.
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PjmCommonEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Менеджер зон выбора классов
    private static ClassSelectionZoneManager zoneManager;

    // Менеджер точек автоспавна техники
    private static VehicleSpawnPointManager vehicleSpawnManager;

    // Отслеживание статуса нахождения в зоне для каждого игрока
    private static final Map<UUID, Boolean> playerZoneStatus = new HashMap<>();
    private static int zoneCheckTicks = 0;
    private static final int ZONE_CHECK_INTERVAL = 20; // Проверять каждую секунду (20 тиков)

    // Хранение измерения, в котором умер игрок
    private static final Map<UUID, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> deathDimensions = new HashMap<>();

    private PjmCommonEvents() {
    }

    // NeoForge 1.21.1: Capabilities removed, using Data Attachments instead
    // No onAttachCapabilities needed - attachments are registered in PjmAttachments

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Save experience from old player
            int oldExperienceLevel = event.getOriginal().experienceLevel;
            float oldExperienceProgress = event.getOriginal().experienceProgress;
            int oldTotalExperience = event.getOriginal().totalExperience;

            // NeoForge 1.21.1: Use getData() instead of getCapability()
            PjmPlayerData oldData = event.getOriginal().getData(PjmAttachments.PLAYER_DATA);
            PjmPlayerData newData = serverPlayer.getData(PjmAttachments.PLAYER_DATA);

            // Copy data from old player
            newData.copyFrom(oldData);

            // Update rank if NOT_ENLISTED
            if (newData.getRank() == ru.liko.pjmbasemod.common.player.PjmRank.NOT_ENLISTED) {
                newData.setRank(ru.liko.pjmbasemod.common.player.PjmRank.PRIVATE);
                LOGGER.debug("Updated rank from NOT_ENLISTED to PRIVATE on respawn: {}",
                        serverPlayer.getName().getString());
            }

            // Restore team in scoreboard
            String teamId = newData.getTeam();
            if (teamId != null && !teamId.isEmpty()
                    && !teamId.equals(ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper.NO_TEAM_PLACEHOLDER)) {
                net.minecraft.world.scores.Scoreboard scoreboard = serverPlayer.server.getScoreboard();
                net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(teamId);
                if (team != null) {
                    scoreboard.addPlayerToTeam(serverPlayer.getScoreboardName(), team);
                    LOGGER.debug("Restored team {} for player {} on respawn", teamId,
                            serverPlayer.getName().getString());
                }
            }

            // Restore experience
            serverPlayer.experienceLevel = oldExperienceLevel;
            serverPlayer.experienceProgress = oldExperienceProgress;
            serverPlayer.totalExperience = oldTotalExperience;

            // Restore kit if player had a class
            ru.liko.pjmbasemod.common.player.PjmPlayerClass playerClass = newData.getPlayerClass();
            if (playerClass != null && playerClass != ru.liko.pjmbasemod.common.player.PjmPlayerClass.NONE) {
                String playerTeamName = newData.getTeam();
                if (playerTeamName != null && !playerTeamName.isEmpty() && !playerTeamName
                        .equals(ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper.NO_TEAM_PLACEHOLDER)) {
                    String selectedKitId = newData.getSelectedKitId();

                    java.util.Optional<ru.liko.pjmbasemod.common.KitDefinition> kitOpt = ru.liko.pjmbasemod.common.KitsConfig
                            .getKit(playerClass.getId(), playerTeamName, selectedKitId);
                    if (kitOpt.isEmpty() && !selectedKitId.isEmpty()) {
                        kitOpt = ru.liko.pjmbasemod.common.KitsConfig.getKit(playerClass.getId(), playerTeamName,
                                "default");
                    }
                    if (kitOpt.isEmpty()) {
                        java.util.List<ru.liko.pjmbasemod.common.KitDefinition> allKits = ru.liko.pjmbasemod.common.KitsConfig
                                .getKits(playerClass.getId(), playerTeamName);
                        if (!allKits.isEmpty()) {
                            kitOpt = java.util.Optional.of(allKits.get(0));
                        }
                    }

                    if (kitOpt.isPresent()) {
                        ru.liko.pjmbasemod.common.KitDefinition kit = kitOpt.get();
                        ru.liko.pjmbasemod.common.network.packet.SelectClassPacket.giveClassItems(
                                serverPlayer, kit.getItems(), true);
                        LOGGER.debug("Restored kit {} of class {} for player {} on respawn",
                                kit.getId(), playerClass.getId(), serverPlayer.getName().getString());
                    }
                }
            }

            // Sync player data
            PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(serverPlayer.getId(), newData), serverPlayer);
            PjmNetworking.sendToTracking(SyncPjmDataPacket.fromPlayerData(serverPlayer.getId(), newData), serverPlayer);

            // Sync to all online players for tab
            for (ServerPlayer otherPlayer : serverPlayer.server.getPlayerList().getPlayers()) {
                if (otherPlayer != serverPlayer) {
                    PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(serverPlayer.getId(), newData),
                            otherPlayer);
                }
            }
        }
    }

    /**
     * После респавна — если матч активен, переключаем в спектатора и открываем меню
     * спавна.
     * Игрок выбирает точку спавна, после чего телепортируется и переключается в
     * выживание.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
            return;

        // Не открываем спавн-меню если это был respawn из End (победа дракона)
        if (event.isEndConquered())
            return;

        ru.liko.pjmbasemod.common.match.MatchManager matchManager = ru.liko.pjmbasemod.common.match.MatchManager.get();

        // Открываем спавн-меню только если матч активен
        if (matchManager.getState() != ru.liko.pjmbasemod.common.match.MatchState.IN_PROGRESS) {
            // Вне матча: респавн в том же измерении, где умер игрок
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> deathDim = deathDimensions
                    .remove(serverPlayer.getUUID());
            if (deathDim != null && serverPlayer.getRespawnPosition() == null) {
                net.minecraft.server.level.ServerLevel deathLevel = serverPlayer.server.getLevel(deathDim);
                if (deathLevel != null && serverPlayer.level() != deathLevel) {
                    // Ищем, не динамическое ли это измерение
                    String dimName = deathDim.location().getPath();
                    ru.liko.pjmbasemod.common.dimension.DimensionConfig config = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager
                            .getConfig(dimName);

                    double x, y, z;
                    float yaw;
                    if (config != null) {
                        x = config.getSpawnX();
                        y = config.getSpawnY();
                        z = config.getSpawnZ();
                        yaw = config.getSpawnYaw();
                    } else {
                        net.minecraft.core.BlockPos sharedSpawn = deathLevel.getSharedSpawnPos();
                        x = sharedSpawn.getX() + 0.5;
                        y = sharedSpawn.getY();
                        z = sharedSpawn.getZ() + 0.5;
                        yaw = deathLevel.getSharedSpawnAngle();
                    }
                    serverPlayer.teleportTo(deathLevel, x, y, z, yaw, 0);
                    LOGGER.debug("Respawned player {} in their death dimension: {}", serverPlayer.getName().getString(),
                            deathDim.location());
                }
            }
            return;
        }

        PjmPlayerData data = serverPlayer.getData(PjmAttachments.PLAYER_DATA);
        String teamName = data.getTeam();

        // Игрок должен быть в команде
        if (teamName == null || teamName.isEmpty()
                || teamName.equals(ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper.NO_TEAM_PLACEHOLDER)) {
            // Если нет команды, тоже спавним в измерении смерти
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> deathDim2 = deathDimensions
                    .remove(serverPlayer.getUUID());
            if (deathDim2 != null && serverPlayer.getRespawnPosition() == null) {
                net.minecraft.server.level.ServerLevel deathLevel2 = serverPlayer.server.getLevel(deathDim2);
                if (deathLevel2 != null && serverPlayer.level() != deathLevel2) {
                    net.minecraft.core.BlockPos sharedSpawn = deathLevel2.getSharedSpawnPos();
                    serverPlayer.teleportTo(deathLevel2, sharedSpawn.getX() + 0.5, sharedSpawn.getY(),
                            sharedSpawn.getZ() + 0.5, deathLevel2.getSharedSpawnAngle(), 0);
                }
            }
            return;
        }

        // Удаляем из deathDimensions чтобы не копилось
        deathDimensions.remove(serverPlayer.getUUID());

        // Переключаем в спектатор чтобы игрок не был убит на спавне мира
        serverPlayer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

        // Формируем список точек спавна
        java.util.List<ru.liko.pjmbasemod.common.network.packet.OpenSpawnMenuPacket.SpawnPoint> spawnPoints = new java.util.ArrayList<>();

        // База команды
        spawnPoints.add(new ru.liko.pjmbasemod.common.network.packet.OpenSpawnMenuPacket.SpawnPoint(
                "team_base",
                "База команды (" + teamName + ")",
                "team_base",
                true));

        // Захваченные контрольные точки команды (ищем в дименшоне текущей карты)
        ru.liko.pjmbasemod.common.map.config.MapConfig mapConfig = ru.liko.pjmbasemod.common.map.config.MapConfigManager
                .getConfig(matchManager.getCurrentMapId());
        net.minecraft.server.level.ServerLevel cpLevel = serverPlayer.server.overworld();
        if (mapConfig != null && mapConfig.getDimension() != null && !mapConfig.getDimension().isEmpty()) {
            try {
                net.minecraft.resources.ResourceLocation dimId = net.minecraft.resources.ResourceLocation
                        .parse(mapConfig.getDimension());
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = net.minecraft.resources.ResourceKey
                        .create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
                net.minecraft.server.level.ServerLevel resolved = serverPlayer.server.getLevel(dimKey);
                if (resolved != null)
                    cpLevel = resolved;
            } catch (Exception ignored) {
            }
        }
        ru.liko.pjmbasemod.common.gamemode.ControlPointManager cpManager = ru.liko.pjmbasemod.common.gamemode.ControlPointManager
                .get(cpLevel);
        for (ru.liko.pjmbasemod.common.gamemode.ControlPoint cp : cpManager.getAll()) {
            if (teamName.equals(cp.getOwnerTeam())) {
                spawnPoints.add(new ru.liko.pjmbasemod.common.network.packet.OpenSpawnMenuPacket.SpawnPoint(
                        "cp_" + cp.getId(),
                        "⚑ " + cp.getDisplayName(),
                        "capture_point",
                        true));
            }
        }

        // Лобби
        spawnPoints.add(new ru.liko.pjmbasemod.common.network.packet.OpenSpawnMenuPacket.SpawnPoint(
                "lobby",
                "Лобби",
                "lobby",
                true));

        int cooldown = ru.liko.pjmbasemod.Config.getSpawnCooldownSeconds();

        ru.liko.pjmbasemod.common.network.packet.OpenSpawnMenuPacket packet = new ru.liko.pjmbasemod.common.network.packet.OpenSpawnMenuPacket(
                spawnPoints, cooldown);

        PjmNetworking.sendToClient(packet, serverPlayer);
        LOGGER.debug("Opened spawn menu for player {} after respawn", serverPlayer.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // NeoForge 1.21.1: Use getData() instead of getCapability()
            PjmPlayerData data = serverPlayer.getData(PjmAttachments.PLAYER_DATA);

            // Update rank if NOT_ENLISTED
            if (data.getRank() == ru.liko.pjmbasemod.common.player.PjmRank.NOT_ENLISTED) {
                data.setRank(ru.liko.pjmbasemod.common.player.PjmRank.PRIVATE);
                LOGGER.debug("Updated rank from NOT_ENLISTED to PRIVATE for player: {}",
                        serverPlayer.getName().getString());
            }

            // Отправляем событие входа на сервер (join)
            ru.liko.pjmbasemod.common.stats.StatsApi.sendSingleEvent(serverPlayer, "join", null, null);

            // Sync player data
            PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(serverPlayer.getId(), data), serverPlayer);

            // Sync Map Info
            ru.liko.pjmbasemod.common.match.MatchManager.get().sendMapInfo(serverPlayer);

            // Телепорт при входе в зависимости от состояния матча
            ru.liko.pjmbasemod.common.match.MatchManager matchMgr = ru.liko.pjmbasemod.common.match.MatchManager.get();
            ru.liko.pjmbasemod.common.match.MatchState matchState = matchMgr.getState();
            if (matchState == ru.liko.pjmbasemod.common.match.MatchState.WAITING_FOR_PLAYERS
                    || matchState == ru.liko.pjmbasemod.common.match.MatchState.STARTING) {
                ru.liko.pjmbasemod.common.map.MapManager.teleportToServerJoin(serverPlayer);
            } else if (matchState == ru.liko.pjmbasemod.common.match.MatchState.IN_PROGRESS) {
                // Mid-match join: если у игрока есть команда — телепорт на карту
                String team = matchMgr.getPlayerTeam(serverPlayer.getUUID());
                if (team != null && !team.isEmpty()) {
                    ru.liko.pjmbasemod.common.map.MapManager.teleportToMap(
                            serverPlayer, matchMgr.getCurrentMapId(), team);
                    serverPlayer.sendSystemMessage(
                            net.minecraft.network.chat.Component
                                    .literal("§aМатч идёт! Вы подключены к команде " + team));
                } else {
                    // Без команды — спектатор на карте
                    serverPlayer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                    ru.liko.pjmbasemod.common.map.MapManager.teleportToServerJoin(serverPlayer);
                    serverPlayer.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§7Матч идёт. Выберите команду для участия."));
                }
            }

            // Sync Capture Points
            ru.liko.pjmbasemod.common.gamemode.CaptureNetwork.syncAllTo(serverPlayer);

            // Синхронизируем конфигурацию команд с клиентом
            // Получаем отображаемые имена команд из scoreboard
            net.minecraft.world.scores.Scoreboard scoreboard = serverPlayer.server.getScoreboard();
            String team1Id = ru.liko.pjmbasemod.Config.getTeam1Name();
            String team2Id = ru.liko.pjmbasemod.Config.getTeam2Name();

            net.minecraft.world.scores.PlayerTeam team1 = scoreboard.getPlayerTeam(team1Id);
            net.minecraft.world.scores.PlayerTeam team2 = scoreboard.getPlayerTeam(team2Id);

            String team1DisplayName = (team1 != null && team1.getDisplayName() != null)
                    ? team1.getDisplayName().getString()
                    : team1Id;
            String team2DisplayName = (team2 != null && team2.getDisplayName() != null)
                    ? team2.getDisplayName().getString()
                    : team2Id;

            PjmNetworking.sendToClient(
                    new ru.liko.pjmbasemod.common.network.packet.SyncTeamConfigPacket(
                            team1Id,
                            team1DisplayName,
                            team2Id,
                            team2DisplayName,
                            ru.liko.pjmbasemod.Config.getTeamBalanceThreshold()),
                    serverPlayer);

            // Синхронизируем все опции кастомизации (скины, предметы)
            ru.liko.pjmbasemod.common.customization.CustomizationManager manager = ru.liko.pjmbasemod.common.customization.CustomizationManager
                    .get(serverPlayer.server);
            PjmNetworking.sendToClient(
                    new ru.liko.pjmbasemod.common.network.packet.SyncCustomizationOptionsPacket(
                            manager.getAllOptions()),
                    serverPlayer);

            // Синхронизируем данные о китах (снаряжение фракций)
            PjmNetworking.sendToClient(
                    new ru.liko.pjmbasemod.common.network.packet.SyncKitsDataPacket(
                            ru.liko.pjmbasemod.common.KitsConfig.getAllKitsData()),
                    serverPlayer);

            // Синхронизируем статистику смертей
            DeathTracker.syncToPlayer(serverPlayer);

            // Sync data of all online players to new player
            for (ServerPlayer otherPlayer : serverPlayer.server.getPlayerList().getPlayers()) {
                if (otherPlayer != serverPlayer) {
                    PjmPlayerData otherData = otherPlayer.getData(PjmAttachments.PLAYER_DATA);
                    PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(otherPlayer.getId(), otherData),
                            serverPlayer);
                }
            }

            // Проверяем, назначена ли команда игроку. Если нет - открываем меню выбора
            // команды
            String currentTeam = data.getTeam();
            if (currentTeam != null && !currentTeam.isEmpty()
                    && !currentTeam.equals(ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper.NO_TEAM_PLACEHOLDER)) {
                // Если команда есть - регистрируем в MatchManager
                ru.liko.pjmbasemod.common.match.MatchManager.get().playerJoinTeam(serverPlayer, currentTeam);
            }

            if (!TeamBalanceHelper.hasTeam(serverPlayer)) {
                if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                    LOGGER.info("Игрок {} не имеет команды - открываем меню выбора команды",
                            serverPlayer.getName().getString());
                }

                // Получаем информацию о балансе команд
                Map<String, Integer> balanceInfo = TeamBalanceHelper.getTeamBalanceInfo(serverPlayer.server);

                // Open team selection menu (allowBack = false for first login)
                PjmNetworking.sendToClient(
                        OpenTeamSelectionPacket.create(
                                balanceInfo,
                                ru.liko.pjmbasemod.Config.getTeam1Name(),
                                ru.liko.pjmbasemod.Config.getTeam2Name(),
                                false,
                                ru.liko.pjmbasemod.Config.getTeamBalanceThreshold()),
                        serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
            // Send target data to tracker
            PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
            PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(target.getId(), data), tracker);
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }
        if (!event.getLevel().isClientSide() && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                // Display player info
                PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
                String playerName = target.getName().getString();

                serverPlayer.displayClientMessage(
                        Component.literal("§6Игрок: §f" + playerName), false);
                serverPlayer.displayClientMessage(
                        Component.literal("§aЗвание: ").append(data.getRank().getDisplayName()), false);
                serverPlayer.displayClientMessage(
                        Component.literal("§bКоманда: §f" + ScoreboardTeamHelper.getTeamName(target)), false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Регистрируем смерть в трекере
            DeathTracker.recordDeath(player);
            // Запоминаем мир смерти
            deathDimensions.put(player.getUUID(), player.level().dimension());
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Initialize configurations (ensure they are loaded before use)
        ru.liko.pjmbasemod.common.PjmServerConfig.init();
        ru.liko.pjmbasemod.common.KitsConfig.init();

        // Загружаем конфиги карт и ротацию
        ru.liko.pjmbasemod.common.map.config.MapConfigManager.loadAll();
        ru.liko.pjmbasemod.common.map.MapRotationManager.load();
        LOGGER.info("Loaded map configs and rotation");

        // Инициализируем менеджер зон при старте сервера
        zoneManager = new ClassSelectionZoneManager(event.getServer());
        zoneManager.load();
        LOGGER.info("Initialized ClassSelectionZoneManager");

        // Инициализируем менеджер автоспавна техники
        vehicleSpawnManager = new VehicleSpawnPointManager(event.getServer());
        vehicleSpawnManager.load();
        VehicleSpawnSystem.initialize(event.getServer(), vehicleSpawnManager);
        LOGGER.info("Initialized VehicleSpawnPointManager");

        // Отключаем естественную регенерацию для милсим-реализма
        if (ru.liko.pjmbasemod.Config.isDisableHunger()) {
            event.getServer().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION)
                    .set(false, event.getServer());
            LOGGER.info("Disabled natural health regeneration for MIL-SIM realism mode");
        }

        // Инициализация Stats API (отправка статистики на бекенд)
        ru.liko.pjmbasemod.common.stats.StatsApiConfig.init();
        ru.liko.pjmbasemod.common.stats.StatsApi.init();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Пересоздаём динамические измерения после полной загрузки сервера
        ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.recreateAll(event.getServer());
        LOGGER.info("Dynamic dimensions recreation complete");

        // Автосоздание лобби-дименшона если он ещё не существует
        ensureLobbyDimension(event.getServer());
    }

    /**
     * Создаёт лобби-дименшон через DynamicDimensionManager если он ещё не
     * существует.
     * Настройки лобби берутся из DimensionConfig
     * (config/pjmbasemod/dimensions/lobby.json).
     */
    private static void ensureLobbyDimension(net.minecraft.server.MinecraftServer server) {
        String lobbyName = "lobby";
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lobbyKey = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager
                .getResourceKey(lobbyName);

        if (server.getLevel(lobbyKey) != null) {
            LOGGER.info("Lobby dimension already exists");
            return;
        }

        LOGGER.info("Creating lobby dimension...");
        boolean created = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.createDimension(
                server, lobbyName, ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.GenType.VOID);

        if (created) {
            // Применяем дефолтные настройки для лобби
            ru.liko.pjmbasemod.common.dimension.DimensionConfig lobbyConfig = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager
                    .getConfig(lobbyName);
            if (lobbyConfig != null) {
                lobbyConfig.setDisplayName("Lobby");
                lobbyConfig.setPvpEnabled(false);
                lobbyConfig.setMobSpawning(false);
                lobbyConfig.setKeepInventory(true);
                lobbyConfig.setAllowBlockBreaking(false);
                lobbyConfig.setAllowBlockPlacing(false);
                lobbyConfig.setAllowExplosions(false);
                lobbyConfig.setAntiGriefOverride("enabled");
                lobbyConfig.setTimeOfDay(6000); // Полдень
                lobbyConfig.setWeather("clear");
                lobbyConfig.setTimeFrozen(true);
                lobbyConfig.save();

                // Применяем настройки к уровню
                net.minecraft.server.level.ServerLevel lobbyLevel = server.getLevel(lobbyKey);
                if (lobbyLevel != null) {
                    ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager.applyConfig(lobbyLevel, lobbyConfig);
                }
            }
            LOGGER.info("Lobby dimension created successfully");
        } else {
            LOGGER.error("Failed to create lobby dimension! Players will be sent to Overworld as fallback.");
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Сохраняем зоны при остановке сервера
        if (zoneManager != null) {
            zoneManager.save();
            LOGGER.info("Saved class selection zones");
        }

        // Сохраняем точки автоспавна техники
        if (vehicleSpawnManager != null) {
            vehicleSpawnManager.save();
            LOGGER.info("Saved vehicle spawn points");
        }
        VehicleSpawnSystem.reset();
        vehicleSpawnManager = null;

        // Завершаем Stats API (graceful shutdown HTTP клиента)
        ru.liko.pjmbasemod.common.stats.StatsApi.shutdown();

        // Сбрасываем MatchManager синглтон
        ru.liko.pjmbasemod.common.match.MatchManager.resetInstance();

        // Очищаем статусы зон
        playerZoneStatus.clear();
    }

    /**
     * Check player zone status and send updates.
     * NeoForge 1.21.1: Uses ServerTickEvent.Post instead of phase check.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        // Match System Tick — вызывается КАЖДЫЙ тик сервера (таймеры в тиках)
        ru.liko.pjmbasemod.common.match.MatchManager.get().tick(server);

        // Зоны проверяются раз в секунду (20 тиков)
        zoneCheckTicks++;
        if (zoneCheckTicks < ZONE_CHECK_INTERVAL) {
            return;
        }
        zoneCheckTicks = 0;

        if (zoneManager == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            boolean currentlyInZone = zoneManager.isInAnyZone(player.position(), player.level().dimension());
            Boolean previousStatus = playerZoneStatus.get(playerId);

            if (previousStatus == null || previousStatus != currentlyInZone) {
                playerZoneStatus.put(playerId, currentlyInZone);
                PjmNetworking.sendToClient(
                        new ru.liko.pjmbasemod.common.network.packet.SyncInZoneStatusPacket(currentlyInZone),
                        player);
            }
        }
    }

    /**
     * Handle level tick for capture game mode.
     * NeoForge 1.21.1: Uses LevelTickEvent.Post instead of phase check.
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ru.liko.pjmbasemod.common.gamemode.CaptureGameModeController.tick(serverLevel);
        }
    }

    /**
     * Обновляет статус зоны для игрока при входе
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Отправляем событие выхода (leave)
            ru.liko.pjmbasemod.common.stats.StatsApi.sendSingleEvent(player, "leave", null, null);

            // Отправляем полную статистику игрока на бекенд перед очисткой данных
            ru.liko.pjmbasemod.common.stats.StatsApi.sendFullStats(player);

            playerZoneStatus.remove(player.getUUID());
            // Удаляем из MatchManager чтобы избежать утечки памяти
            ru.liko.pjmbasemod.common.match.MatchManager.get().playerLeave(player.getUUID());
        }
    }

    /**
     * Получить менеджер зон выбора классов
     */
    public static ClassSelectionZoneManager getZoneManager() {
        return zoneManager;
    }

    /**
     * Получить менеджер точек автоспавна техники
     */
    public static VehicleSpawnPointManager getVehicleSpawnManager() {
        return vehicleSpawnManager;
    }

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        // Регистрируем permissions для интеграции с нативными командами
        event.addNodes(
                PjmPermissions.BASE,
                PjmPermissions.MINECRAFT_TEAM, PjmPermissions.MINECRAFT_SCOREBOARD,
                PjmPermissions.TEAM_MANAGE, PjmPermissions.TEAM_JOIN_SELF, PjmPermissions.TEAM_JOIN_OTHER,
                PjmPermissions.MILITARY_SETUP,
                PjmPermissions.RANK_MANAGE, PjmPermissions.RANK_SET_SELF,
                PjmPermissions.CLASS_ZONE_MANAGE,
                PjmPermissions.TIMER_CREATE, PjmPermissions.TIMER_MANAGE, PjmPermissions.TIMER_VIEW,
                PjmPermissions.VEHICLE_SPAWN_MANAGE, PjmPermissions.VEHICLE_SPAWN_FORCE,
                PjmPermissions.CLASS_SSO, PjmPermissions.CLASS_SPN,
                PjmPermissions.CONFIG_RELOAD);
    }
}
