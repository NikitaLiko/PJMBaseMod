package ru.liko.pjmbasemod.common.scheduler;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving and loading of scheduled commands to world data
 * NeoForge 1.21.1: Updated SavedData API with Factory pattern.
 */
public class SchedulerSaveData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "Pjmbasemod_scheduler";

    public SchedulerSaveData() {
        super();
    }

    // NeoForge 1.21.1: SavedData uses Factory pattern
    private static final SavedData.Factory<SchedulerSaveData> FACTORY = new SavedData.Factory<>(
        SchedulerSaveData::new,
        (tag, provider) -> load(tag),
        null // No data fixer
    );

    /**
     * Get or create the save data for the server
     */
    public static SchedulerSaveData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(FACTORY, DATA_NAME);
    }

    /**
     * Load saved data from NBT
     */
    public static SchedulerSaveData load(CompoundTag tag) {
        SchedulerSaveData data = new SchedulerSaveData();

        ListTag commandsList = tag.getList("ScheduledCommands", Tag.TAG_COMPOUND);
        List<ScheduledCommand> commands = new ArrayList<>();

        for (int i = 0; i < commandsList.size(); i++) {
            CompoundTag commandTag = commandsList.getCompound(i);
            try {
                ScheduledCommand command = ScheduledCommand.deserialize(commandTag);
                commands.add(command);
            } catch (Exception e) {
                // Skip corrupted entries
                LOGGER.error("Failed to load scheduled command: {}", e.getMessage());
            }
        }

        // Load into scheduler
        CommandScheduler.getInstance().loadFromSaveData(commands);

        return data;
    }

    /**
     * Save data to NBT
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag commandsList = new ListTag();

        for (ScheduledCommand command : CommandScheduler.getInstance().getAllCommands()) {
            commandsList.add(command.serialize());
        }

        tag.put("ScheduledCommands", commandsList);
        return tag;
    }
}
