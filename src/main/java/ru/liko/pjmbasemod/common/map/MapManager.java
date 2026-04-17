package ru.liko.pjmbasemod.common.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import ru.liko.pjmbasemod.common.map.config.MapConfig;
import ru.liko.pjmbasemod.common.map.config.MapConfigManager;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Управляет измерениями и перемещением игроков между ними.
 */
public class MapManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ResourceKey для измерения лобби (через DynamicDimensionManager).
    public static final ResourceKey<Level> LOBBY_DIMENSION = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager
            .getResourceKey("lobby");

    /**
     * Телепортирует игрока в лобби.
     * Координаты спавна берутся из DimensionConfig лобби.
     * Fallback на Overworld если лобби не существует.
     */
    public static void teleportToLobby(ServerPlayer player) {
        if (player.level().dimension().equals(LOBBY_DIMENSION))
            return;

        net.minecraft.world.level.GameType prevMode = player.gameMode.getGameModeForPlayer();
        ServerLevel lobbyLevel = player.server.getLevel(LOBBY_DIMENSION);

        if (lobbyLevel != null) {
            // Координаты спавна из DimensionConfig лобби
            ru.liko.pjmbasemod.common.dimension.DimensionConfig lobbyConfig = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager
                    .getConfig("lobby");
            double x = 0.5, y = 100, z = 0.5;
            float yaw = 0;
            if (lobbyConfig != null) {
                x = lobbyConfig.getSpawnX();
                y = lobbyConfig.getSpawnY();
                z = lobbyConfig.getSpawnZ();
                yaw = lobbyConfig.getSpawnYaw();
            }
            player.teleportTo(lobbyLevel, x, y, z, yaw, 0);
        } else {
            LOGGER.warn("Lobby dimension not found, falling back to Overworld.");
            ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                player.teleportTo(overworld, 0.5, 100, 0.5, 0, 0);
            }
        }
        // Пересинхронизируем GameMode с новым уровнем (фикс для динамических
        // дименшонов)
        player.setGameMode(prevMode);
    }

    /**
     * Телепортирует игрока в измерение, указанное для спавна при входе на сервер.
     */
    public static void teleportToServerJoin(ServerPlayer player) {
        String dimName = ru.liko.pjmbasemod.common.PjmServerConfig.getServerJoinDimension();
        if (dimName != null && !dimName.isEmpty() && !dimName.equalsIgnoreCase("lobby")) {
            ServerLevel targetLevel = resolveDimension(player.server, dimName);
            if (targetLevel != null) {
                net.minecraft.world.level.GameType prevMode = player.gameMode.getGameModeForPlayer();

                // Попытка получить координаты спавна из конфигурации динамического измерения
                String cleanName = dimName;
                if (cleanName.contains(":")) {
                    cleanName = cleanName.substring(cleanName.indexOf(":") + 1);
                }
                ru.liko.pjmbasemod.common.dimension.DimensionConfig config = ru.liko.pjmbasemod.common.dimension.DynamicDimensionManager
                        .getConfig(cleanName);

                double x, y, z;
                float yaw;

                if (config != null) {
                    x = config.getSpawnX();
                    y = config.getSpawnY();
                    z = config.getSpawnZ();
                    yaw = config.getSpawnYaw();
                } else {
                    net.minecraft.core.BlockPos spawnPos = targetLevel.getSharedSpawnPos();
                    x = spawnPos.getX() + 0.5;
                    y = spawnPos.getY();
                    z = spawnPos.getZ() + 0.5;
                    yaw = targetLevel.getSharedSpawnAngle();
                }

                player.teleportTo(targetLevel, x, y, z, yaw, 0);
                player.setGameMode(prevMode);
                return;
            } else {
                LOGGER.warn("Server join dimension '{}' not found, falling back to lobby.", dimName);
            }
        }

        // Fallback на лобби
        teleportToLobby(player);
    }

    /**
     * Телепортирует игрока на игровую карту.
     * Использует измерение из конфига карты (поле "dimension").
     */
    public static void teleportToMap(ServerPlayer player, String mapId, String teamId) {
        MapConfig config = MapConfigManager.getConfig(mapId);
        if (config == null) {
            LOGGER.error("Map config not found for: " + mapId);
            return;
        }

        // Получаем измерение из конфига, fallback на Overworld
        ServerLevel gameLevel = resolveDimension(player.server, config.getDimension());

        if (gameLevel != null) {
            net.minecraft.world.level.GameType prevMode = player.gameMode.getGameModeForPlayer();
            MapConfig.TeamConfig teamConfig = config.getTeams().stream()
                    .filter(t -> t.getTeamId().equals(teamId))
                    .findFirst()
                    .orElse(null);

            if (teamConfig != null && teamConfig.getSpawnPos() != null && teamConfig.getSpawnPos().length >= 3) {
                double[] pos = teamConfig.getSpawnPos();
                player.teleportTo(gameLevel, pos[0], pos[1], pos[2], teamConfig.getSpawnYaw(), 0);
            } else {
                player.teleportTo(gameLevel, 0, 100, 0, 0, 0); // Default fallback
            }
            // Пересинхронизируем GameMode с новым уровнем (фикс для динамических
            // дименшонов)
            player.setGameMode(prevMode);
        } else {
            LOGGER.error("Could not resolve dimension for map '{}'. Dimension: '{}'", mapId, config.getDimension());
        }
    }

    /**
     * Резолвит измерение по строке из конфига.
     * Поддерживаемые форматы:
     * - "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"
     * - "modid:dimension_name" (кастомные измерения)
     * - null или пустая строка → Overworld
     */
    private static ServerLevel resolveDimension(net.minecraft.server.MinecraftServer server, String dimensionString) {
        if (dimensionString == null || dimensionString.isEmpty()) {
            return server.getLevel(Level.OVERWORLD);
        }

        // Разделяем namespace:path
        ResourceLocation dimensionId;
        try {
            dimensionId = ResourceLocation.parse(dimensionString);
        } catch (Exception e) {
            LOGGER.warn("Invalid dimension format '{}', falling back to Overworld", dimensionString);
            return server.getLevel(Level.OVERWORLD);
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, dimensionId);

        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            LOGGER.warn("Dimension '{}' not found, falling back to Overworld", dimensionString);
            return server.getLevel(Level.OVERWORLD);
        }

        return level;
    }

    public static boolean isInLobby(ServerPlayer player) {
        return player.level().dimension().equals(LOBBY_DIMENSION);
    }
}
