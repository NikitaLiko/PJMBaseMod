package ru.liko.pjmbasemod.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.common.player.PjmRank;
import ru.liko.pjmbasemod.common.util.ItemParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Конфигурация китов для классов.
 * Хранит киты в JSON файле: pjm_kits.json
 * 
 * Updated to support multiple Kit Definitions (Variations) per class/team.
 */
public class KitsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String KITS_FILE_NAME = "pjm_kits.json";
    private static final String OLD_KITS_FILE_NAME = "wrb_kits.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Структура: className -> teamName -> List<KitDefinition>
    private static Map<String, Map<String, List<KitDefinition>>> kitsData = new HashMap<>();
    
    // Кэш Item объектов для быстрого доступа: className -> teamName -> kitId -> List<Item>
    private static Map<String, Map<String, Map<String, List<Item>>>> kitsItemsCache = new HashMap<>();
    
    private static boolean initialized = false;
    
    public static Path getKitsFilePath() {
        return FMLPaths.GAMEDIR.get().resolve(KITS_FILE_NAME);
    }
    
    public static void init() {
        if (initialized) {
            return;
        }
        
        Path kitsPath = getKitsFilePath();
        
        // Миграция: wrb_kits.json → pjm_kits.json
        if (!Files.exists(kitsPath)) {
            Path oldPath = FMLPaths.GAMEDIR.get().resolve(OLD_KITS_FILE_NAME);
            if (Files.exists(oldPath)) {
                try {
                    Files.move(oldPath, kitsPath);
                    LOGGER.info("Migrated kits config from {} to {}", oldPath, kitsPath);
                } catch (IOException e) {
                    LOGGER.warn("Failed to migrate kits config, copying instead", e);
                    try { Files.copy(oldPath, kitsPath); } catch (IOException ignored) {}
                }
            }
        }
        
        if (Files.exists(kitsPath)) {
            loadFromFile();
        } else {
            createDefaultKits();
            saveToFile();
        }
        
        rebuildItemsCache();
        initialized = true;
        
        LOGGER.info("KitsConfig инициализирован (Ranked System). Файл: {}", kitsPath);
    }
    
    public static void loadFromFile() {
        Path kitsPath = getKitsFilePath();
        
        try (Reader reader = Files.newBufferedReader(kitsPath)) {
            // Try loading as new format first
            try {
                Type type = new TypeToken<Map<String, Map<String, List<KitDefinition>>>>(){}.getType();
                Map<String, Map<String, List<KitDefinition>>> loaded = GSON.fromJson(reader, type);
                
                if (loaded != null) {
                    kitsData = loaded;
                    // Validate data structure (ensure lists are not null)
                    for (var classEntry : kitsData.values()) {
                        for (var teamEntry : classEntry.entrySet()) {
                            if (teamEntry.getValue() == null) {
                                teamEntry.setValue(new ArrayList<>());
                            }
                        }
                    }
                    LOGGER.info("Киты загружены успешно (New Format)");
                    return;
                }
            } catch (Exception e) {
                // If failed, try loading as old format and migrating
                LOGGER.warn("Не удалось загрузить киты в новом формате, пробуем старый формат...");
            }
            
            // Re-open reader for second attempt
        } catch (IOException e) {
            LOGGER.error("Ошибка чтения файла китов: {}", e.getMessage());
        }

        // Fallback: Try loading old format manually
        try (Reader reader = Files.newBufferedReader(kitsPath)) {
             Type oldType = new TypeToken<Map<String, Map<String, List<String>>>>(){}.getType();
             Map<String, Map<String, List<String>>> oldData = GSON.fromJson(reader, oldType);
             
             if (oldData != null) {
                 LOGGER.info("Обнаружен старый формат китов, выполняется миграция...");
                 kitsData.clear();
                 
                 for (var classEntry : oldData.entrySet()) {
                     String classId = classEntry.getKey();
                     Map<String, List<KitDefinition>> newClassMap = new HashMap<>();
                     
                     for (var teamEntry : classEntry.getValue().entrySet()) {
                         String teamId = teamEntry.getKey();
                         List<String> items = teamEntry.getValue();
                         
                         // Create a default kit from old items
                         KitDefinition defaultKit = new KitDefinition(
                             "default", 
                             "Standard", 
                             PjmRank.PRIVATE, 
                             items
                         );
                         
                         List<KitDefinition> kits = new ArrayList<>();
                         kits.add(defaultKit);
                         newClassMap.put(teamId, kits);
                     }
                     kitsData.put(classId, newClassMap);
                 }
                 saveToFile(); // Save converted data
             }
        } catch (Exception e) {
            LOGGER.error("Ошибка миграции китов: {}", e.getMessage());
            createDefaultKits();
        }
    }
    
    public static void saveToFile() {
        Path kitsPath = getKitsFilePath();
        try (Writer writer = Files.newBufferedWriter(kitsPath)) {
            GSON.toJson(kitsData, writer);
            LOGGER.info("Киты сохранены в {}", kitsPath);
        } catch (IOException e) {
            LOGGER.error("Ошибка сохранения китов в файл: {}", e.getMessage());
        }
    }
    
    public static boolean reload() {
        try {
            loadFromFile();
            rebuildItemsCache();
            return true;
        } catch (Exception e) {
            LOGGER.error("Ошибка перезагрузки китов: {}", e.getMessage());
            return false;
        }
    }
    
    public static Map<String, Map<String, List<KitDefinition>>> getAllKitsData() {
        if (!initialized) init();
        return new HashMap<>(kitsData);
    }
    
    private static void createDefaultKits() {
        kitsData.clear();
        // Create basic default kits for all classes
        // ... (For brevity, I will recreate a simplified version of defaults or just the structure)
        // Re-implementing logic from previous file but wrapping in KitDefinition
        
        createDefaultKitForClass("assault", 
            Arrays.asList("minecraft:iron_sword", "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots", "minecraft:cooked_beef:16", "minecraft:arrow:64"),
            Arrays.asList("minecraft:diamond_sword", "minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots", "minecraft:cooked_beef:16", "minecraft:arrow:64")
        );
        
        createDefaultKitForClass("machine_gunner",
            Arrays.asList("minecraft:bow", "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots", "minecraft:cooked_beef:16", "minecraft:arrow:128"),
            Arrays.asList("minecraft:crossbow", "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots", "minecraft:cooked_beef:16", "minecraft:arrow:128")
        );
        
        createDefaultKitForClass("medic",
            Arrays.asList("minecraft:golden_sword", "minecraft:golden_helmet", "minecraft:golden_chestplate", "minecraft:golden_leggings", "minecraft:golden_boots", "minecraft:cooked_beef:32", "minecraft:golden_apple:8", "minecraft:potion:8"),
            Arrays.asList("minecraft:iron_sword", "minecraft:golden_helmet", "minecraft:golden_chestplate", "minecraft:golden_leggings", "minecraft:golden_boots", "minecraft:cooked_beef:32", "minecraft:golden_apple:16", "minecraft:potion:16")
        );

        createDefaultKitForClass("anti_tank",
            Arrays.asList("minecraft:crossbow", "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots", "minecraft:cooked_beef:16", "minecraft:tnt:16", "minecraft:arrow:32"),
            Arrays.asList("minecraft:crossbow", "minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots", "minecraft:cooked_beef:16", "minecraft:tnt:32", "minecraft:arrow:64")
        );
        
        createDefaultKitForClass("engineer",
            Arrays.asList("minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_shovel", "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:cooked_beef:16", "minecraft:oak_planks:64", "minecraft:cobblestone:64"),
            Arrays.asList("minecraft:diamond_pickaxe", "minecraft:diamond_axe", "minecraft:diamond_shovel", "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:cooked_beef:16", "minecraft:oak_planks:64", "minecraft:cobblestone:128")
        );
        
        createDefaultKitForClass("crew",
            Arrays.asList("minecraft:stone_sword", "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:cooked_beef:16"),
            Arrays.asList("minecraft:stone_sword", "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:cooked_beef:16")
        );
        
        createDefaultKitForClass("sniper",
            Arrays.asList("minecraft:bow", "minecraft:spyglass", "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:cooked_beef:16", "minecraft:arrow:64"),
            Arrays.asList("minecraft:bow", "minecraft:spyglass", "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots", "minecraft:cooked_beef:16", "minecraft:arrow:128")
        );

        createDefaultKitForClass("sso",
             Arrays.asList("minecraft:netherite_sword", "minecraft:netherite_helmet", "minecraft:netherite_chestplate", "minecraft:netherite_leggings", "minecraft:netherite_boots", "minecraft:cooked_beef:32", "minecraft:golden_apple:16", "minecraft:potion:16", "minecraft:arrow:128"),
             Arrays.asList("minecraft:netherite_sword", "minecraft:netherite_helmet", "minecraft:netherite_chestplate", "minecraft:netherite_leggings", "minecraft:netherite_boots", "minecraft:cooked_beef:32", "minecraft:golden_apple:16", "minecraft:potion:16", "minecraft:arrow:128")
        );
        
        createDefaultKitForClass("uav_operator",
            Arrays.asList("minecraft:iron_sword", "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots", "minecraft:cooked_beef:16", "minecraft:compass", "minecraft:spyglass", "minecraft:arrow:64"),
            Arrays.asList("minecraft:diamond_sword", "minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots", "minecraft:cooked_beef:16", "minecraft:compass", "minecraft:spyglass", "minecraft:arrow:64")
        );
        
        createDefaultKitForClass("scout",
            Arrays.asList("minecraft:bow", "minecraft:iron_sword", "minecraft:spyglass", "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:cooked_beef:16", "minecraft:arrow:96"),
            Arrays.asList("minecraft:crossbow", "minecraft:iron_sword", "minecraft:spyglass", "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots", "minecraft:cooked_beef:16", "minecraft:arrow:96")
        );
        
        createDefaultKitForClass("ew_specialist",
            Arrays.asList("minecraft:iron_sword", "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots", "minecraft:cooked_beef:16", "minecraft:compass", "minecraft:spyglass"),
            Arrays.asList("minecraft:diamond_sword", "minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots", "minecraft:cooked_beef:16", "minecraft:compass", "minecraft:spyglass")
        );
        
        createDefaultKitForClass("spn",
            Arrays.asList("minecraft:netherite_sword", "minecraft:netherite_helmet", "minecraft:netherite_chestplate", "minecraft:netherite_leggings", "minecraft:netherite_boots", "minecraft:cooked_beef:32", "minecraft:golden_apple:8", "minecraft:ender_pearl:4", "minecraft:arrow:64"),
            Arrays.asList("minecraft:netherite_sword", "minecraft:netherite_helmet", "minecraft:netherite_chestplate", "minecraft:netherite_leggings", "minecraft:netherite_boots", "minecraft:cooked_beef:32", "minecraft:golden_apple:8", "minecraft:ender_pearl:4", "minecraft:arrow:64")
        );
    }
    
    private static void createDefaultKitForClass(String classId, List<String> team1Items, List<String> team2Items) {
        Map<String, List<KitDefinition>> teamMap = new HashMap<>();
        
        List<KitDefinition> t1List = new ArrayList<>();
        t1List.add(new KitDefinition("default", "Standard", PjmRank.PRIVATE, new ArrayList<>(team1Items)));
        teamMap.put("team1", t1List);
        
        List<KitDefinition> t2List = new ArrayList<>();
        t2List.add(new KitDefinition("default", "Standard", PjmRank.PRIVATE, new ArrayList<>(team2Items)));
        teamMap.put("team2", t2List);
        
        kitsData.put(classId, teamMap);
    }
    
    private static void rebuildItemsCache() {
        kitsItemsCache.clear();
        
        for (var classEntry : kitsData.entrySet()) {
            String className = classEntry.getKey();
            Map<String, Map<String, List<Item>>> teamItemsMap = new HashMap<>();
            
            for (var teamEntry : classEntry.getValue().entrySet()) {
                String teamName = teamEntry.getKey();
                Map<String, List<Item>> kitItemsMap = new HashMap<>();
                
                for (KitDefinition kit : teamEntry.getValue()) {
                    List<Item> items = new ArrayList<>();
                    for (String itemString : kit.getItems()) {
                         ItemParser.parseItemStack(itemString).ifPresent(stack -> items.add(stack.getItem()));
                    }
                    kitItemsMap.put(kit.getId(), items);
                }
                
                teamItemsMap.put(teamName, kitItemsMap);
            }
            
            kitsItemsCache.put(className, teamItemsMap);
        }
    }
    
    // --- Access Methods ---
    
    /**
     * Gets specific kit definition.
     */
    public static Optional<KitDefinition> getKit(String classId, String teamName, String kitId) {
        if (!initialized) init();
        String normalizedTeam = normalizeTeamName(teamName);
        
        Map<String, List<KitDefinition>> teamKits = kitsData.get(classId);
        if (teamKits == null) return Optional.empty();
        
        List<KitDefinition> kits = teamKits.get(normalizedTeam);
        if (kits == null) return Optional.empty();
        
        return kits.stream()
            .filter(k -> k.getId().equals(kitId))
            .findFirst();
    }
    
    /**
     * Legacy method: gets default kit items.
     */
    public static List<String> getKitItemStrings(String classId, String teamName) {
        // Return default kit items
        return getKit(classId, teamName, "default")
            .map(KitDefinition::getItems)
            .orElseGet(() -> {
                // If no default, try first available
                if (!initialized) init();
                String normalizedTeam = normalizeTeamName(teamName);
                Map<String, List<KitDefinition>> teamKits = kitsData.get(classId);
                if (teamKits != null) {
                    List<KitDefinition> kits = teamKits.get(normalizedTeam);
                    if (kits != null && !kits.isEmpty()) {
                        return kits.get(0).getItems();
                    }
                }
                return new ArrayList<>();
            });
    }
    
    /**
     * Gets all kits for a class/team
     */
    public static List<KitDefinition> getKits(String classId, String teamName) {
        if (!initialized) init();
        String normalizedTeam = normalizeTeamName(teamName);
        
        Map<String, List<KitDefinition>> teamKits = kitsData.get(classId);
        if (teamKits == null) return new ArrayList<>();
        
        List<KitDefinition> kits = teamKits.get(normalizedTeam);
        return kits != null ? new ArrayList<>(kits) : new ArrayList<>();
    }

    /**
     * Legacy method: Gets list of Item objects for the default kit.
     */
    public static List<Item> getKitItems(String classId, String teamName) {
        if (!initialized) init();
        String normalizedTeam = normalizeTeamName(teamName);
        
        Map<String, Map<String, List<Item>>> classItems = kitsItemsCache.get(classId);
        if (classItems == null) return new ArrayList<>();
        
        Map<String, List<Item>> kitItemsMap = classItems.get(normalizedTeam);
        if (kitItemsMap == null || kitItemsMap.isEmpty()) return new ArrayList<>();
        
        // Return default if exists, else first
        return kitItemsMap.getOrDefault("default", 
               kitItemsMap.values().stream().findFirst().orElse(new ArrayList<>()));
    }

    /**
     * Legacy method: Sets the default kit.
     */
    public static void setKit(String classId, String teamName, List<String> items) {
        saveKit(classId, teamName, new KitDefinition("default", "Standard", PjmRank.PRIVATE, items));
    }

    /**
     * Saves a kit definition. Overwrites if ID exists.
     */
    public static void saveKit(String classId, String teamName, KitDefinition kit) {
        if (!initialized) init();
        String normalizedTeam = normalizeTeamName(teamName);
        
        kitsData.computeIfAbsent(classId, k -> new HashMap<>())
                .computeIfAbsent(normalizedTeam, k -> new ArrayList<>());
        
        List<KitDefinition> kits = kitsData.get(classId).get(normalizedTeam);
        
        // Remove existing with same ID
        kits.removeIf(k -> k.getId().equals(kit.getId()));
        kits.add(kit);
        
        // Sort by rank? Or keep insertion order?
        // Let's keep insertion order but maybe we want to sort by rank logic later.
        
        rebuildItemsCache();
        saveToFile();
    }
    
    /**
     * Removes a kit.
     */
    public static boolean removeKit(String classId, String teamName, String kitId) {
        if (!initialized) init();
        String normalizedTeam = normalizeTeamName(teamName);
        
        Map<String, List<KitDefinition>> teamKits = kitsData.get(classId);
        if (teamKits == null) return false;
        
        List<KitDefinition> kits = teamKits.get(normalizedTeam);
        if (kits == null) return false;
        
        boolean removed = kits.removeIf(k -> k.getId().equals(kitId));
        if (removed) {
            rebuildItemsCache();
            saveToFile();
        }
        return removed;
    }

    // --- Legacy Adapter Methods ---
    
    public static void addItemToKit(String classId, String teamName, String itemString) {
        // Add to default kit
        getKit(classId, teamName, "default").ifPresentOrElse(kit -> {
            kit.getItems().add(itemString);
            saveKit(classId, teamName, kit);
        }, () -> {
            // Create default if not exists
            List<String> items = new ArrayList<>();
            items.add(itemString);
            saveKit(classId, teamName, new KitDefinition("default", "Standard", PjmRank.PRIVATE, items));
        });
    }
    
    public static boolean removeItemFromKit(String classId, String teamName, String itemString) {
        Optional<KitDefinition> kitOpt = getKit(classId, teamName, "default");
        if (kitOpt.isPresent()) {
            KitDefinition kit = kitOpt.get();
            boolean removed = kit.getItems().remove(itemString);
            if (removed) {
                saveKit(classId, teamName, kit);
            }
            return removed;
        }
        return false;
    }
    
    public static Set<String> getClassIds() {
        if (!initialized) init();
        return kitsData.keySet();
    }
    
    private static String normalizeTeamName(String teamName) {
        if (teamName == null || teamName.isEmpty()) return "team1";
        String lower = teamName.toLowerCase();
        if (lower.equals("team1") || lower.equals("team2")) return lower;
        String team1Name = Config.getTeam1Name().toLowerCase();
        String team2Name = Config.getTeam2Name().toLowerCase();
        if (lower.equals(team1Name)) return "team1";
        else if (lower.equals(team2Name)) return "team2";
        return "team1";
    }
    
    public static int getClassLimit(String classId) {
        return Config.getClassLimit(classId);
    }
}
