package ru.liko.pjmbasemod.common.block;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Система логирования разрушения блоков (аналог CoreProtect).
 * Записывает информацию о том, кто, когда и какой блок сломал.
 */
public class BlockBreakLogger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_FOLDER = "pjm_block_logs";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static boolean initialized = false;
    
    /**
     * Запись лога о разрушении блока
     */
    public static class BlockBreakEntry {
        public String timestamp;
        public String playerName;
        public String playerUuid;
        public String blockId;
        public String dimension;
        public int x;
        public int y;
        public int z;
        public String tool;
        
        public BlockBreakEntry() {}
        
        public BlockBreakEntry(ServerPlayer player, BlockState blockState, BlockPos pos, String toolUsed) {
            LocalDateTime now = LocalDateTime.now();
            this.timestamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.playerName = player.getName().getString();
            this.playerUuid = player.getUUID().toString();
            
            ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
            this.blockId = blockId != null ? blockId.toString() : "unknown";
            
            this.dimension = player.level().dimension().location().toString();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.tool = toolUsed;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s (%s) сломал %s в %s [%d, %d, %d] инструментом: %s",
                timestamp, playerName, playerUuid, blockId, dimension, x, y, z, tool);
        }
    }
    
    /**
     * Инициализирует систему логирования
     */
    public static void init() {
        if (initialized) return;
        
        try {
            Path logFolder = getLogFolder();
            if (!Files.exists(logFolder)) {
                Files.createDirectories(logFolder);
            }
            initialized = true;
            LOGGER.info("BlockBreakLogger инициализирован. Папка логов: {}", logFolder);
        } catch (IOException e) {
            LOGGER.error("Ошибка инициализации BlockBreakLogger: {}", e.getMessage());
        }
    }
    
    /**
     * Получает путь к папке логов
     */
    public static Path getLogFolder() {
        return FMLPaths.GAMEDIR.get().resolve(LOG_FOLDER);
    }
    
    /**
     * Получает путь к файлу лога за текущий день
     */
    private static Path getDailyLogFile() {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        return getLogFolder().resolve("block_breaks_" + date + ".log");
    }
    
    /**
     * Получает путь к JSON файлу лога за текущий день
     */
    private static Path getDailyJsonFile() {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        return getLogFolder().resolve("block_breaks_" + date + ".json");
    }
    
    /**
     * Логирует разрушение блока
     */
    public static void logBlockBreak(ServerPlayer player, BlockState blockState, BlockPos pos, String toolUsed) {
        if (!ru.liko.pjmbasemod.common.PjmServerConfig.isBlockLoggingEnabled()) {
            return;
        }
        
        if (!initialized) {
            init();
        }
        
        BlockBreakEntry entry = new BlockBreakEntry(player, blockState, pos, toolUsed);
        
        // Записываем в текстовый лог
        writeToTextLog(entry);
        
        // Записываем в JSON лог
        writeToJsonLog(entry);
        
        if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
            LOGGER.info("Block break logged: {}", entry);
        }
    }
    
    /**
     * Записывает в текстовый лог файл
     */
    private static void writeToTextLog(BlockBreakEntry entry) {
        try {
            Path logFile = getDailyLogFile();
            String logLine = entry.toString() + System.lineSeparator();
            
            Files.writeString(logFile, logLine, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.error("Ошибка записи в текстовый лог: {}", e.getMessage());
        }
    }
    
    /**
     * Записывает в JSON лог файл
     */
    private static void writeToJsonLog(BlockBreakEntry entry) {
        try {
            Path jsonFile = getDailyJsonFile();
            List<BlockBreakEntry> entries = new ArrayList<>();
            
            // Читаем существующие записи
            if (Files.exists(jsonFile)) {
                String content = Files.readString(jsonFile);
                if (!content.isBlank()) {
                    Type listType = new TypeToken<List<BlockBreakEntry>>(){}.getType();
                    List<BlockBreakEntry> existing = GSON.fromJson(content, listType);
                    if (existing != null) {
                        entries = new ArrayList<>(existing);
                    }
                }
            }
            
            // Добавляем новую запись
            entries.add(entry);
            
            // Записываем обратно
            try (Writer writer = Files.newBufferedWriter(jsonFile)) {
                GSON.toJson(entries, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка записи в JSON лог: {}", e.getMessage());
        }
    }
    
    /**
     * Получает историю разрушений блоков в указанной позиции
     */
    public static List<BlockBreakEntry> getHistoryAt(BlockPos pos, String dimension, int days) {
        List<BlockBreakEntry> result = new ArrayList<>();
        
        for (int i = 0; i < days; i++) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.format(DATE_FORMAT);
            Path jsonFile = getLogFolder().resolve("block_breaks_" + dateStr + ".json");
            
            if (Files.exists(jsonFile)) {
                try {
                    String content = Files.readString(jsonFile);
                    Type listType = new TypeToken<List<BlockBreakEntry>>(){}.getType();
                    List<BlockBreakEntry> entries = GSON.fromJson(content, listType);
                    
                    if (entries != null) {
                        for (BlockBreakEntry entry : entries) {
                            if (entry.x == pos.getX() && 
                                entry.y == pos.getY() && 
                                entry.z == pos.getZ() &&
                                entry.dimension.equals(dimension)) {
                                result.add(entry);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Ошибка чтения лога за {}: {}", dateStr, e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Получает историю разрушений блоков игроком
     */
    public static List<BlockBreakEntry> getHistoryByPlayer(UUID playerUuid, int days) {
        List<BlockBreakEntry> result = new ArrayList<>();
        String uuidStr = playerUuid.toString();
        
        for (int i = 0; i < days; i++) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.format(DATE_FORMAT);
            Path jsonFile = getLogFolder().resolve("block_breaks_" + dateStr + ".json");
            
            if (Files.exists(jsonFile)) {
                try {
                    String content = Files.readString(jsonFile);
                    Type listType = new TypeToken<List<BlockBreakEntry>>(){}.getType();
                    List<BlockBreakEntry> entries = GSON.fromJson(content, listType);
                    
                    if (entries != null) {
                        for (BlockBreakEntry entry : entries) {
                            if (entry.playerUuid.equals(uuidStr)) {
                                result.add(entry);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Ошибка чтения лога за {}: {}", dateStr, e.getMessage());
                }
            }
        }
        
        return result;
    }
}
