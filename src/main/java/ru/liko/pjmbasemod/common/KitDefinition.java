package ru.liko.pjmbasemod.common;

import java.util.ArrayList;
import java.util.List;
import ru.liko.pjmbasemod.common.player.PjmRank;

/**
 * Represents a specific kit variation (loadout) for a class.
 */
public class KitDefinition {
    private String id;
    private String displayName;
    private String minRankId; // Stored as string ID to avoid serialization issues
    private List<String> items;

    public KitDefinition(String id, String displayName, PjmRank minRank, List<String> items) {
        this.id = id;
        this.displayName = displayName;
        this.minRankId = minRank.getId();
        this.items = items != null ? items : new ArrayList<>();
    }
    
    // Default constructor for GSON
    public KitDefinition() {
        this.id = "default";
        this.displayName = "Default";
        this.minRankId = PjmRank.PRIVATE.getId();
        this.items = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public PjmRank getMinRank() {
        return PjmRank.fromString(minRankId).orElse(PjmRank.PRIVATE);
    }

    public void setMinRank(PjmRank rank) {
        this.minRankId = rank.getId();
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
