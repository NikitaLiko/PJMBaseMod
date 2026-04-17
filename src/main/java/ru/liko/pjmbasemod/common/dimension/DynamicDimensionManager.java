package ru.liko.pjmbasemod.common.dimension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Pjmbasemod;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Управляет динамическим созданием и удалением измерений (dimensions) в рантайме.
 * Созданные измерения сохраняются в конфиг и пересоздаются при рестарте сервера.
 */
public class DynamicDimensionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_FILE = FMLPaths.CONFIGDIR.get()
            .resolve(Pjmbasemod.MODID).resolve("dynamic_dimensions.json");

    // name -> config
    private static final Map<String, DimensionConfig> dimensionConfigs = new LinkedHashMap<>();

    /**
     * Типы генерации для динамических измерений.
     */
    public enum GenType {
        VOID,   // Пустой void мир
        FLAT,   // Суперплоский
        NORMAL; // Копия overworld генерации

        public static GenType fromString(String s) {
            if (s == null) return null;
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * Возвращает ResourceKey<Level> для динамического измерения по имени.
     */
    public static ResourceKey<Level> getResourceKey(String name) {
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, name));
    }

    /**
     * Создаёт новое измерение в рантайме.
     *
     * @param server сервер
     * @param name   имя измерения (латиница, без пробелов)
     * @param genType тип генерации (VOID, FLAT, NORMAL)
     * @return true если создано успешно
     */
    public static boolean createDimension(MinecraftServer server, String name, GenType genType) {
        ResourceKey<Level> dimKey = getResourceKey(name);

        if (server.getLevel(dimKey) != null) {
            LOGGER.warn("Dimension '{}' already exists!", name);
            return false;
        }

        try {
            Map<ResourceKey<Level>, ServerLevel> levels = getLevelsMap(server);
            LevelStorageSource.LevelStorageAccess storage = getStorageSource(server);

            if (levels == null || storage == null) {
                LOGGER.error("Cannot access server internals (levels/storageSource) via reflection");
                return false;
            }

            ServerLevel overworld = server.overworld();

            // Используем тип измерения Overworld
            Registry<DimensionType> dtRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
            Holder<DimensionType> dimType = dtRegistry.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);

            // Создаём генератор чанков в зависимости от типа
            var generator = switch (genType) {
                case VOID -> createVoidGenerator(server);
                case FLAT -> createFlatGenerator(server);
                case NORMAL -> overworld.getChunkSource().getGenerator();
            };

            LevelStem stem = new LevelStem(dimType, generator);

            // Данные уровня (наследуем от overworld)
            ServerLevelData overworldData = (ServerLevelData) server.getWorldData().overworldData();
            DerivedLevelData derivedData = new DerivedLevelData(server.getWorldData(), overworldData);

            // No-op progress listener
            ChunkProgressListener noOpListener = new ChunkProgressListener() {
                @Override public void updateSpawnPos(ChunkPos center) {}
                @Override public void onStatusChange(ChunkPos pos, @Nullable ChunkStatus status) {}
                @Override public void start() {}
                @Override public void stop() {}
            };

            // Создаём ServerLevel
            ServerLevel newLevel = new ServerLevel(
                    server,
                    Util.backgroundExecutor(),
                    storage,
                    derivedData,
                    dimKey,
                    stem,
                    noOpListener,
                    false,              // isDebug
                    overworld.getSeed(),
                    List.of(),          // customSpawners
                    false,              // tickTime (только overworld тикает время)
                    null                // randomSequences
            );

            // Добавляем в карту измерений сервера
            levels.put(dimKey, newLevel);

            // Синхронизируем мировую границу с overworld
            overworld.getWorldBorder().addListener(
                    new BorderChangeListener.DelegateBorderChangeListener(newLevel.getWorldBorder()));

            // Оповещаем NeoForge о загрузке нового уровня (критично для блоков/чанков)
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.level.LevelEvent.Load(newLevel));

            // Создаём / загружаем конфиг
            DimensionConfig config = DimensionConfig.load(name);
            if (config == null) {
                config = new DimensionConfig(name, genType);
                config.save();
            }
            dimensionConfigs.put(name, config);

            // Применяем настройки из конфига
            applyConfig(newLevel, config);

            // Сохраняем реестр дименшонов
            saveToDisk();

            LOGGER.info("Created dynamic dimension '{}' (type: {})", name, genType);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to create dimension '{}'", name, e);
            return false;
        }
    }

    /**
     * Удаляет (выгружает) динамическое измерение.
     * Все игроки будут телепортированы в overworld.
     */
    public static boolean removeDimension(MinecraftServer server, String name) {
        ResourceKey<Level> dimKey = getResourceKey(name);
        ServerLevel level = server.getLevel(dimKey);

        if (level == null) {
            return false;
        }

        try {
            // Телепортируем всех игроков в overworld
            ServerLevel overworld = server.overworld();
            for (ServerPlayer player : new ArrayList<>(level.players())) {
                player.teleportTo(overworld, 0.5, 100, 0.5, 0, 0);
            }

            // Сохраняем мир
            level.save(null, false, false);

            // Оповещаем NeoForge о выгрузке уровня
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.level.LevelEvent.Unload(level));

            // Закрываем чанк-кеш для предотвращения утечек
            try {
                level.getChunkSource().close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close chunk source for dimension '{}'", name, e);
            }

            // Удаляем из карты серверных измерений
            Map<ResourceKey<Level>, ServerLevel> levels = getLevelsMap(server);
            if (levels != null) {
                levels.remove(dimKey);
            }

            // Удаляем конфиг
            dimensionConfigs.remove(name);
            DimensionConfig.delete(name);
            saveToDisk();

            LOGGER.info("Removed dynamic dimension '{}'", name);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to remove dimension '{}'", name, e);
            return false;
        }
    }

    /**
     * Возвращает имена всех динамических измерений.
     */
    public static Set<String> getDimensionNames() {
        return Collections.unmodifiableSet(dimensionConfigs.keySet());
    }

    /**
     * Возвращает все динамические измерения: имя -> тип (для обратной совместимости).
     */
    public static Map<String, String> getAllDimensions() {
        Map<String, String> result = new LinkedHashMap<>();
        for (var entry : dimensionConfigs.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getGenType());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Возвращает конфиг измерения по имени.
     */
    public static DimensionConfig getConfig(String name) {
        return dimensionConfigs.get(name);
    }

    /**
     * Возвращает все конфиги.
     */
    public static Map<String, DimensionConfig> getAllConfigs() {
        return Collections.unmodifiableMap(dimensionConfigs);
    }

    /**
     * Пересоздаёт все ранее сохранённые динамические измерения при старте сервера.
     */
    public static void recreateAll(MinecraftServer server) {
        loadFromDisk();
        if (dimensionConfigs.isEmpty()) return;

        LOGGER.info("Recreating {} dynamic dimension(s)...", dimensionConfigs.size());
        Map<String, DimensionConfig> toRecreate = new LinkedHashMap<>(dimensionConfigs);
        dimensionConfigs.clear(); // createDimension добавит обратно

        for (var entry : toRecreate.entrySet()) {
            GenType type = GenType.fromString(entry.getValue().getGenType());
            if (type == null) type = GenType.VOID;

            if (createDimension(server, entry.getKey(), type)) {
                LOGGER.info("Recreated dimension '{}'", entry.getKey());
            } else {
                LOGGER.error("Failed to recreate dimension '{}'", entry.getKey());
            }
        }
    }

    /**
     * Применяет настройки конфига к ServerLevel.
     */
    public static void applyConfig(ServerLevel level, DimensionConfig config) {
        if (config == null) return;

        // Время суток
        if (config.getTimeOfDay() >= 0) {
            level.setDayTime(config.getTimeOfDay());
        }

        // Погода
        String weather = config.getWeather();
        if (weather != null) {
            switch (weather.toLowerCase()) {
                case "clear" -> level.setWeatherParameters(6000, 0, false, false);
                case "rain" -> level.setWeatherParameters(0, 6000, true, false);
                case "storm" -> level.setWeatherParameters(0, 6000, true, true);
            }
        }

        // Мировая граница
        if (config.getWorldBorderSize() > 0) {
            level.getWorldBorder().setCenter(config.getWorldBorderCenterX(), config.getWorldBorderCenterZ());
            level.getWorldBorder().setSize(config.getWorldBorderSize());
        }

        // Гейм-рулы
        if (!config.isMobSpawning()) {
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)
                    .set(false, level.getServer());
        }
        if (config.isKeepInventory()) {
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)
                    .set(true, level.getServer());
        }
        if (!config.isAnnounceDeaths()) {
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_SHOWDEATHMESSAGES)
                    .set(false, level.getServer());
        }

        LOGGER.debug("Applied config for dimension '{}'", config.getName());
    }

    /**
     * Перезагружает конфиг дименшона с диска и применяет настройки.
     */
    public static boolean reloadConfig(MinecraftServer server, String name) {
        DimensionConfig config = DimensionConfig.load(name);
        if (config == null) return false;

        dimensionConfigs.put(name, config);

        ResourceKey<Level> dimKey = getResourceKey(name);
        ServerLevel level = server.getLevel(dimKey);
        if (level != null) {
            applyConfig(level, config);
        }
        return true;
    }

    // --- Генераторы ---

    private static FlatLevelSource createVoidGenerator(MinecraftServer server) {
        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> biome = biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);

        // Public constructor: (Optional<HolderSet<StructureSet>>, Holder<Biome>, List<Holder<PlacedFeature>>)
        // Без слоёв = void мир
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
                Optional.empty(), biome, List.<Holder<PlacedFeature>>of());
        return new FlatLevelSource(settings);
    }

    private static FlatLevelSource createFlatGenerator(MinecraftServer server) {
        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> biome = biomeRegistry.getHolderOrThrow(Biomes.PLAINS);

        List<FlatLayerInfo> layers = List.of(
                new FlatLayerInfo(1, Blocks.BEDROCK),
                new FlatLayerInfo(2, Blocks.DIRT),
                new FlatLayerInfo(1, Blocks.GRASS_BLOCK)
        );

        // Создаём базовые настройки, затем добавляем слои через withBiomeAndLayers
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
                Optional.empty(), biome, List.<Holder<PlacedFeature>>of());
        settings = settings.withBiomeAndLayers(layers, Optional.empty(), biome);
        return new FlatLevelSource(settings);
    }

    // --- Reflection ---

    @SuppressWarnings("unchecked")
    private static Map<ResourceKey<Level>, ServerLevel> getLevelsMap(MinecraftServer server) {
        try {
            Field f = MinecraftServer.class.getDeclaredField("levels");
            f.setAccessible(true);
            return (Map<ResourceKey<Level>, ServerLevel>) f.get(server);
        } catch (Exception e) {
            LOGGER.error("Cannot access MinecraftServer.levels via reflection", e);
            return null;
        }
    }

    private static LevelStorageSource.LevelStorageAccess getStorageSource(MinecraftServer server) {
        try {
            Field f = MinecraftServer.class.getDeclaredField("storageSource");
            f.setAccessible(true);
            return (LevelStorageSource.LevelStorageAccess) f.get(server);
        } catch (Exception e) {
            LOGGER.error("Cannot access MinecraftServer.storageSource via reflection", e);
            return null;
        }
    }

    // --- Persistence ---

    private static void saveToDisk() {
        try {
            Path parent = SAVE_FILE.getParent();
            if (!Files.exists(parent)) Files.createDirectories(parent);
            // Сохраняем только список имён дименшонов (конфиги хранятся отдельно)
            List<String> names = new ArrayList<>(dimensionConfigs.keySet());
            Files.writeString(SAVE_FILE, GSON.toJson(names));
        } catch (IOException e) {
            LOGGER.error("Failed to save dynamic dimensions registry", e);
        }
    }

    private static void loadFromDisk() {
        dimensionConfigs.clear();
        if (!Files.exists(SAVE_FILE)) return;

        try {
            String json = Files.readString(SAVE_FILE);
            // Попытка загрузить новый формат (список имён)
            try {
                List<String> names = GSON.fromJson(json, new TypeToken<List<String>>(){}.getType());
                if (names != null) {
                    for (String name : names) {
                        DimensionConfig config = DimensionConfig.load(name);
                        if (config != null) {
                            dimensionConfigs.put(name, config);
                        } else {
                            // Конфиг не найден — создаём дефолтный
                            config = new DimensionConfig(name, GenType.VOID);
                            config.save();
                            dimensionConfigs.put(name, config);
                        }
                    }
                    return;
                }
            } catch (Exception ignored) {}

            // Обратная совместимость: старый формат {name: type}
            Map<String, String> legacy = GSON.fromJson(json,
                    new TypeToken<Map<String, String>>(){}.getType());
            if (legacy != null) {
                for (var entry : legacy.entrySet()) {
                    GenType type = GenType.fromString(entry.getValue());
                    if (type == null) type = GenType.VOID;
                    DimensionConfig config = new DimensionConfig(entry.getKey(), type);
                    config.save();
                    dimensionConfigs.put(entry.getKey(), config);
                }
                // Пересохраняем в новом формате
                saveToDisk();
                LOGGER.info("Migrated {} dimensions from legacy format", legacy.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load dynamic dimensions registry", e);
        }
    }
}
