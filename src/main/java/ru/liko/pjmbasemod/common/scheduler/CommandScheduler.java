package ru.liko.pjmbasemod.common.scheduler;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages scheduled commands and their execution
 */
public class CommandScheduler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static CommandScheduler instance;
    private final Map<UUID, ScheduledCommand> scheduledCommands = new ConcurrentHashMap<>();
    private MinecraftServer server;

    private CommandScheduler() {}

    public static CommandScheduler getInstance() {
        if (instance == null) {
            instance = new CommandScheduler();
        }
        return instance;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Schedule a one-time command execution
     * @param command Command to execute (without leading /)
     * @param delaySeconds Delay in seconds
     * @param creatorName Name of player who created this timer
     * @return UUID of the scheduled command
     */
    public UUID scheduleCommand(String command, int delaySeconds, String creatorName) {
        ScheduledCommand scheduled = new ScheduledCommand(command, delaySeconds * 20L, 0, creatorName);
        scheduledCommands.put(scheduled.getId(), scheduled);
        saveScheduler();
        if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
            LOGGER.info("Scheduled one-time command '{}' to execute in {} seconds (ID: {})",
                command, delaySeconds, scheduled.getId());
        }
        return scheduled.getId();
    }

    /**
     * Schedule a repeating command execution
     * @param command Command to execute (without leading /)
     * @param intervalSeconds Interval in seconds
     * @param creatorName Name of player who created this timer
     * @return UUID of the scheduled command
     */
    public UUID scheduleRepeatingCommand(String command, int intervalSeconds, String creatorName) {
        ScheduledCommand scheduled = new ScheduledCommand(command, intervalSeconds * 20L, intervalSeconds * 20L, creatorName);
        scheduledCommands.put(scheduled.getId(), scheduled);
        saveScheduler();
        if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
            LOGGER.info("Scheduled repeating command '{}' to execute every {} seconds (ID: {})",
                command, intervalSeconds, scheduled.getId());
        }
        return scheduled.getId();
    }

    /**
     * Cancel a scheduled command
     * @param id UUID of the command
     * @return true if command was found and cancelled
     */
    public boolean cancelCommand(UUID id) {
        ScheduledCommand removed = scheduledCommands.remove(id);
        if (removed != null) {
            saveScheduler();
            if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                LOGGER.info("Cancelled scheduled command: {}", id);
            }
            return true;
        }
        return false;
    }

    /**
     * Pause a scheduled command
     */
    public boolean pauseCommand(UUID id) {
        ScheduledCommand command = scheduledCommands.get(id);
        if (command != null && !command.isPaused()) {
            command.pause();
            saveScheduler();
            if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                LOGGER.info("Paused scheduled command: {}", id);
            }
            return true;
        }
        return false;
    }

    /**
     * Resume a paused command
     */
    public boolean resumeCommand(UUID id) {
        ScheduledCommand command = scheduledCommands.get(id);
        if (command != null && command.isPaused()) {
            command.resume();
            saveScheduler();
            if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                LOGGER.info("Resumed scheduled command: {}", id);
            }
            return true;
        }
        return false;
    }

    /**
     * Get all scheduled commands
     */
    public List<ScheduledCommand> getAllCommands() {
        return new ArrayList<>(scheduledCommands.values());
    }

    /**
     * Get a specific scheduled command by ID
     */
    public Optional<ScheduledCommand> getCommand(UUID id) {
        return Optional.ofNullable(scheduledCommands.get(id));
    }

    /**
     * Get scheduled command by index (for easier command usage)
     */
    public Optional<ScheduledCommand> getCommandByIndex(int index) {
        List<ScheduledCommand> commands = getAllCommands();
        if (index >= 0 && index < commands.size()) {
            return Optional.of(commands.get(index));
        }
        return Optional.empty();
    }

    /**
     * Clear all scheduled commands
     */
    public void clearAll() {
        scheduledCommands.clear();
        saveScheduler();
        if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
            LOGGER.info("Cleared all scheduled commands");
        }
    }

    /**
     * Main tick method - called every server tick
     */
    public void tick() {
        if (server == null || scheduledCommands.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, ScheduledCommand>> iterator = scheduledCommands.entrySet().iterator();
        boolean needsSave = false;

        while (iterator.hasNext()) {
            Map.Entry<UUID, ScheduledCommand> entry = iterator.next();
            ScheduledCommand command = entry.getValue();

            if (command.tick()) {
                // Command is ready to execute
                try {
                    command.execute(server);
                    if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                        LOGGER.info("Executed scheduled command: /{}", command.getCommand());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error executing scheduled command: /{}", command.getCommand(), e);
                }
            }

            // Remove if it's a one-time command that has executed
            if (command.shouldRemove()) {
                iterator.remove();
                needsSave = true;
                if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
                    LOGGER.info("Removed completed one-time command: {}", entry.getKey());
                }
            }
        }

        if (needsSave) {
            saveScheduler();
        }
    }

    /**
     * Load scheduled commands from save data
     */
    public void loadFromSaveData(List<ScheduledCommand> commands) {
        scheduledCommands.clear();
        for (ScheduledCommand command : commands) {
            scheduledCommands.put(command.getId(), command);
        }
        if (ru.liko.pjmbasemod.Config.isDebugLoggingEnabled()) {
            LOGGER.info("Loaded {} scheduled commands from save data", commands.size());
        }
    }

    /**
     * Trigger save to disk
     */
    private void saveScheduler() {
        if (server != null) {
            SchedulerSaveData.get(server).setDirty();
        }
    }

    /**
     * Get count of active commands
     */
    public int getActiveCount() {
        return (int) scheduledCommands.values().stream()
            .filter(cmd -> !cmd.isPaused())
            .count();
    }

    /**
     * Get count of paused commands
     */
    public int getPausedCount() {
        return (int) scheduledCommands.values().stream()
            .filter(ScheduledCommand::isPaused)
            .count();
    }
}
