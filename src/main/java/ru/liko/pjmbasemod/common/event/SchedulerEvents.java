package ru.liko.pjmbasemod.common.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.scheduler.CommandScheduler;
import ru.liko.pjmbasemod.common.scheduler.SchedulerSaveData;

/**
 * Event handlers for the command scheduler system
 * NeoForge 1.21.1: Updated event bus annotations
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SchedulerEvents {

    /**
     * Initialize scheduler when server starts
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        CommandScheduler.getInstance().setServer(server);

        // Load saved timers
        SchedulerSaveData.get(server);
    }

    /**
     * Tick the scheduler every server tick
     * NeoForge 1.21.1: Use ServerTickEvent.Post instead of TickEvent.ServerTickEvent with Phase.END
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        CommandScheduler.getInstance().tick();
    }
}
