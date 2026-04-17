package ru.liko.pjmbasemod.common.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Stores per-player data for the mod (rank, team, class, special inventory slots).
 * Implements INBTSerializable for NeoForge Data Attachments system.
 */
public class PjmPlayerData implements INBTSerializable<CompoundTag> {
    // NBT tag constants
    private static final String TAG_RANK = "Rank";
    private static final String TAG_RANK_POINTS = "RankPoints";
    private static final String TAG_TEAM = "Team";
    private static final String TAG_PLAYER_CLASS = "PlayerClass";
    private static final String TAG_SELECTED_KIT = "SelectedKit";
    private static final String TAG_SPECIAL_SLOTS = "SpecialSlots";
    private static final String TAG_LAST_KIT_TIME = "LastKitTime";
    private static final String TAG_ACTIVE_SKIN = "ActiveSkin";
    private static final String TAG_ACTIVE_ITEMS = "ActiveItems";
    
    /**
     * Codec for serialization with NeoForge Data Attachments.
     * Note: ItemStack[] (specialSlots) is handled separately via NBT for compatibility.
     */
    public static final Codec<PjmPlayerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("rank").forGetter(d -> d.rank.getPersistenceKey()),
            Codec.INT.fieldOf("rankPoints").forGetter(PjmPlayerData::getRankPoints),
            Codec.STRING.fieldOf("team").forGetter(PjmPlayerData::getTeam),
            Codec.STRING.fieldOf("playerClass").forGetter(d -> d.playerClass.getId()),
            Codec.STRING.fieldOf("selectedKitId").forGetter(PjmPlayerData::getSelectedKitId),
            Codec.LONG.fieldOf("lastKitTime").forGetter(PjmPlayerData::getLastKitTime),
            Codec.STRING.fieldOf("activeSkinId").forGetter(PjmPlayerData::getActiveSkinId),
            Codec.STRING.listOf().fieldOf("activeItemIds").forGetter(d -> new ArrayList<>(d.activeItemIds))
        ).apply(instance, PjmPlayerData::fromCodec)
    );
    
    /**
     * Factory method for Codec deserialization.
     */
    private static PjmPlayerData fromCodec(String rankKey, int rankPoints, String team, 
            String playerClassId, String selectedKitId, long lastKitTime, 
            String activeSkinId, List<String> activeItemIds) {
        PjmPlayerData data = new PjmPlayerData();
        data.setRank(PjmRank.fromKeyOrDefault(rankKey));
        data.setRankPoints(rankPoints);
        data.setTeam(team);
        data.setPlayerClass(PjmPlayerClass.fromIdOrDefault(playerClassId));
        data.setSelectedKitId(selectedKitId);
        data.setLastKitTime(lastKitTime);
        data.setActiveSkinId(activeSkinId);
        data.setActiveItemIds(new HashSet<>(activeItemIds));
        return data;
    }

    private PjmRank rank = PjmRank.PRIVATE;
    private int rankPoints = 0;
    private String team = ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper.NO_TEAM_PLACEHOLDER;
    private PjmPlayerClass playerClass = PjmPlayerClass.NONE; // Класс игрока
    private String selectedKitId = ""; // ID выбранного кита
    
    // Customization
    private String activeSkinId = "";
    private java.util.Set<String> activeItemIds = new java.util.HashSet<>();

    // Специальные слоты для быстрого переключения оружия (4 слота)
    private ItemStack[] specialSlots = new ItemStack[4];
    
    // Время последнего взятия кита (в миллисекундах с эпохи Unix)
    private long lastKitTime = 0;

    public PjmRank getRank() {
        return rank;
    }

    public void setRank(PjmRank rank) {
        this.rank = rank == null ? PjmRank.PRIVATE : (rank == PjmRank.NOT_ENLISTED ? PjmRank.PRIVATE : rank);
    }
    
    public int getRankPoints() {
        return rankPoints;
    }
    
    public void setRankPoints(int points) {
        this.rankPoints = Math.max(0, points);
    }
    
    public void addRankPoints(int points) {
        this.rankPoints = Math.max(0, this.rankPoints + points);
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team == null ? "" : team;
    }

    public PjmPlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PjmPlayerClass playerClass) {
        this.playerClass = playerClass == null ? PjmPlayerClass.NONE : playerClass;
    }

    public String getSelectedKitId() {
        return selectedKitId;
    }

    public void setSelectedKitId(String selectedKitId) {
        this.selectedKitId = selectedKitId == null ? "" : selectedKitId;
    }

    /**
     * Получить специальный слот по индексу (0-3)
     */
    public ItemStack getSpecialSlot(int index) {
        if (index < 0 || index >= 4) {
            return ItemStack.EMPTY;
        }
        return specialSlots[index] == null ? ItemStack.EMPTY : specialSlots[index];
    }
    
    /**
     * Установить предмет в специальный слот
     */
    public void setSpecialSlot(int index, ItemStack stack) {
        if (index >= 0 && index < 4) {
            specialSlots[index] = stack.copy();
        }
    }
    
    /**
     * Получить все специальные слоты
     */
    public ItemStack[] getSpecialSlots() {
        ItemStack[] result = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            result[i] = specialSlots[i] == null ? ItemStack.EMPTY : specialSlots[i];
        }
        return result;
    }
    
    /**
     * Получить время последнего взятия кита
     */
    public long getLastKitTime() {
        return lastKitTime;
    }
    
    /**
     * Установить время последнего взятия кита (текущее время)
     */
    public void setLastKitTime(long time) {
        this.lastKitTime = time;
    }
    
    public String getActiveSkinId() {
        return activeSkinId;
    }

    public void setActiveSkinId(String activeSkinId) {
        this.activeSkinId = activeSkinId == null ? "" : activeSkinId;
    }

    public java.util.Set<String> getActiveItemIds() {
        return activeItemIds;
    }

    public void setActiveItemIds(java.util.Set<String> activeItemIds) {
        this.activeItemIds = activeItemIds == null ? new java.util.HashSet<>() : activeItemIds;
    }

    public void addActiveItemId(String id) {
        this.activeItemIds.add(id);
    }

    public void removeActiveItemId(String id) {
        this.activeItemIds.remove(id);
    }
    
    /**
     * Проверить, можно ли взять кит (прошло ли достаточно времени с последнего взятия)
     * @param cooldownSeconds Время кулдауна в секундах
     * @return true если можно взять кит, false если кулдаун еще активен
     */
    public boolean canTakeKit(int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return true; // Кулдаун отключен
        }
        if (lastKitTime == 0) {
            return true; // Кит еще не брали
        }
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - lastKitTime) / 1000;
        return elapsedSeconds >= cooldownSeconds;
    }
    
    /**
     * Получить оставшееся время кулдауна в секундах
     * @param cooldownSeconds Время кулдауна в секундах
     * @return Оставшееся время в секундах (0 если кулдаун прошел)
     */
    public int getRemainingCooldownSeconds(int cooldownSeconds) {
        if (cooldownSeconds <= 0 || lastKitTime == 0) {
            return 0;
        }
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - lastKitTime) / 1000;
        int remaining = (int)(cooldownSeconds - elapsedSeconds);
        return Math.max(0, remaining);
    }

    public void copyFrom(PjmPlayerData other) {
        this.rank = other.rank;
        this.rankPoints = other.rankPoints;
        this.team = other.team;
        this.playerClass = other.playerClass;
        this.selectedKitId = other.selectedKitId;
        this.lastKitTime = other.lastKitTime;
        this.activeSkinId = other.activeSkinId;
        this.activeItemIds = new java.util.HashSet<>(other.activeItemIds);

        // Копируем специальные слоты
        this.specialSlots = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            ItemStack slot = other.getSpecialSlot(i);
            this.specialSlots[i] = slot.isEmpty() ? null : slot.copy();
        }
    }

    // ==================== INBTSerializable Implementation (NeoForge 1.21.1) ====================
    
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        
        // Basic data via Codec
        tag.putString(TAG_RANK, rank.getPersistenceKey());
        tag.putInt(TAG_RANK_POINTS, rankPoints);
        tag.putString(TAG_TEAM, team);
        tag.putString(TAG_PLAYER_CLASS, playerClass.getId());
        tag.putString(TAG_SELECTED_KIT, selectedKitId);
        tag.putLong(TAG_LAST_KIT_TIME, lastKitTime);
        tag.putString(TAG_ACTIVE_SKIN, activeSkinId);
        
        // Active items as list
        net.minecraft.nbt.ListTag itemsList = new net.minecraft.nbt.ListTag();
        for (String itemId : activeItemIds) {
            itemsList.add(net.minecraft.nbt.StringTag.valueOf(itemId));
        }
        tag.put(TAG_ACTIVE_ITEMS, itemsList);
        
        // Special slots - use ItemStack CODEC for 1.21.1
        CompoundTag slotsTag = new CompoundTag();
        for (int i = 0; i < 4; i++) {
            ItemStack slot = getSpecialSlot(i);
            if (!slot.isEmpty()) {
                Tag slotNbt = ItemStack.OPTIONAL_CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), slot)
                    .result().orElse(new CompoundTag());
                slotsTag.put("slot_" + i, slotNbt);
            }
        }
        tag.put(TAG_SPECIAL_SLOTS, slotsTag);
        
        return tag;
    }
    
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        // Load basic data
        PjmRank loadedRank = PjmRank.fromKeyOrDefault(tag.getString(TAG_RANK));
        if (loadedRank == PjmRank.NOT_ENLISTED || tag.getString(TAG_RANK).isEmpty()) {
            setRank(PjmRank.PRIVATE);
        } else {
            setRank(loadedRank);
        }
        setRankPoints(tag.contains(TAG_RANK_POINTS) ? tag.getInt(TAG_RANK_POINTS) : 0);
        setTeam(tag.getString(TAG_TEAM));
        setPlayerClass(PjmPlayerClass.fromIdOrDefault(tag.getString(TAG_PLAYER_CLASS)));
        setSelectedKitId(tag.getString(TAG_SELECTED_KIT));
        lastKitTime = tag.contains(TAG_LAST_KIT_TIME) ? tag.getLong(TAG_LAST_KIT_TIME) : 0;
        setActiveSkinId(tag.getString(TAG_ACTIVE_SKIN));
        
        // Load active items
        activeItemIds.clear();
        if (tag.contains(TAG_ACTIVE_ITEMS, Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag listTag = tag.getList(TAG_ACTIVE_ITEMS, Tag.TAG_STRING);
            for (int i = 0; i < listTag.size(); i++) {
                activeItemIds.add(listTag.getString(i));
            }
        }
        
        // Load special slots using ItemStack CODEC
        if (tag.contains(TAG_SPECIAL_SLOTS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag slotsTag = tag.getCompound(TAG_SPECIAL_SLOTS);
            for (int i = 0; i < 4; i++) {
                String slotKey = "slot_" + i;
                if (slotsTag.contains(slotKey, CompoundTag.TAG_COMPOUND)) {
                    CompoundTag slotTag = slotsTag.getCompound(slotKey);
                    ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), slotTag)
                        .result().orElse(ItemStack.EMPTY);
                    setSpecialSlot(i, stack);
                } else {
                    setSpecialSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }
}
