package ru.liko.pjmbasemod.common.customization;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class CustomizationOption {
    private final String id;
    private final CustomizationType type;
    private final String value; // Skin texture path or Item registry name
    private final String displayName;
    private final String requiredTeam;
    private final boolean slimModel; // true = slim (Alex), false = default (Steve)

    public CustomizationOption(String id, CustomizationType type, String value, String displayName, String requiredTeam, boolean slimModel) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.displayName = displayName;
        this.requiredTeam = requiredTeam;
        this.slimModel = slimModel;
    }

    public CustomizationOption(String id, CustomizationType type, String value, String displayName, String requiredTeam) {
        this(id, type, value, displayName, requiredTeam, false);
    }

    public CustomizationOption(String id, CustomizationType type, String value, String displayName) {
        this(id, type, value, displayName, null, false);
    }

    public String getId() {
        return id;
    }

    public CustomizationType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public String getRequiredTeam() {
        return requiredTeam;
    }
    
    public boolean isSlimModel() {
        return slimModel;
    }

    public Component getDisplayNameComponent() {
        return Component.literal(displayName);
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("type", type.name());
        tag.putString("value", value);
        tag.putString("displayName", displayName);
        if (requiredTeam != null) {
            tag.putString("requiredTeam", requiredTeam);
        }
        tag.putBoolean("slimModel", slimModel);
        return tag;
    }

    public static CustomizationOption deserialize(CompoundTag tag) {
        String id = tag.getString("id");
        CustomizationType type = CustomizationType.valueOf(tag.getString("type"));
        String value = tag.getString("value");
        String displayName = tag.getString("displayName");
        String requiredTeam = tag.contains("requiredTeam") ? tag.getString("requiredTeam") : null;
        boolean slimModel = tag.contains("slimModel") && tag.getBoolean("slimModel");
        return new CustomizationOption(id, type, value, displayName, requiredTeam, slimModel);
    }

    /**
     * Сериализация в JSON для сохранения в config/customization.json
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("type", type.name());
        json.addProperty("value", value);
        json.addProperty("displayName", displayName);
        if (requiredTeam != null) {
            json.addProperty("requiredTeam", requiredTeam);
        }
        json.addProperty("slimModel", slimModel);
        return json;
    }

    /**
     * Десериализация из JSON для загрузки из config/customization.json
     */
    public static CustomizationOption fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        CustomizationType type = CustomizationType.valueOf(json.get("type").getAsString());
        String value = json.get("value").getAsString();
        String displayName = json.get("displayName").getAsString();
        String requiredTeam = json.has("requiredTeam") ? json.get("requiredTeam").getAsString() : null;
        boolean slimModel = json.has("slimModel") && json.get("slimModel").getAsBoolean();
        return new CustomizationOption(id, type, value, displayName, requiredTeam, slimModel);
    }
}
