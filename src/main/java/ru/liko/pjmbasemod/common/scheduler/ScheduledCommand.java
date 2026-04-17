package ru.liko.pjmbasemod.common.scheduler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * Represents a scheduled command with timing and execution parameters
 */
public class ScheduledCommand {
    private final UUID id;
    private final String command;
    private final long initialDelay; // in ticks (20 ticks = 1 second)
    private final long interval; // in ticks, 0 = one-time execution
    private final boolean repeating;
    private long remainingTicks;
    private boolean paused;
    private final String creatorName;

    public ScheduledCommand(String command, long delayTicks, long intervalTicks, String creatorName) {
        this.id = UUID.randomUUID();
        this.command = command;
        this.initialDelay = delayTicks;
        this.interval = intervalTicks;
        this.repeating = intervalTicks > 0;
        this.remainingTicks = delayTicks;
        this.paused = false;
        this.creatorName = creatorName;
    }

    private ScheduledCommand(UUID id, String command, long initialDelay, long interval,
                            boolean repeating, long remainingTicks, boolean paused, String creatorName) {
        this.id = id;
        this.command = command;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.repeating = repeating;
        this.remainingTicks = remainingTicks;
        this.paused = paused;
        this.creatorName = creatorName;
    }

    /**
     * Updates the timer. Returns true if command should be executed.
     */
    public boolean tick() {
        if (paused) {
            return false;
        }

        remainingTicks--;

        if (remainingTicks <= 0) {
            if (repeating) {
                remainingTicks = interval;
            }
            return true;
        }

        return false;
    }

    /**
     * Executes the scheduled command
     */
    public void execute(MinecraftServer server) {
        if (server == null) return;

        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput(),
            command
        );
    }

    public boolean shouldRemove() {
        return !repeating && remainingTicks <= 0 && !paused;
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
    }

    public UUID getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    public long getRemainingTicks() {
        return remainingTicks;
    }

    public long getRemainingSeconds() {
        return remainingTicks / 20;
    }

    public boolean isRepeating() {
        return repeating;
    }

    public boolean isPaused() {
        return paused;
    }

    public long getInterval() {
        return interval;
    }

    public long getIntervalSeconds() {
        return interval / 20;
    }

    public String getCreatorName() {
        return creatorName;
    }

    // NBT Serialization
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("command", command);
        tag.putLong("initialDelay", initialDelay);
        tag.putLong("interval", interval);
        tag.putBoolean("repeating", repeating);
        tag.putLong("remainingTicks", remainingTicks);
        tag.putBoolean("paused", paused);
        tag.putString("creatorName", creatorName);
        return tag;
    }

    public static ScheduledCommand deserialize(CompoundTag tag) {
        return new ScheduledCommand(
            tag.getUUID("id"),
            tag.getString("command"),
            tag.getLong("initialDelay"),
            tag.getLong("interval"),
            tag.getBoolean("repeating"),
            tag.getLong("remainingTicks"),
            tag.getBoolean("paused"),
            tag.getString("creatorName")
        );
    }

    @Override
    public String toString() {
        String type = repeating ? "Повторяющийся" : "Одноразовый";
        String status = paused ? "ПАУЗА" : "АКТИВЕН";
        String timeInfo = repeating
            ? String.format("Каждые %d сек (осталось %d сек)", getIntervalSeconds(), getRemainingSeconds())
            : String.format("Через %d сек", getRemainingSeconds());

        return String.format("[%s] %s | %s | Команда: /%s",
            status, type, timeInfo, command);
    }
}
