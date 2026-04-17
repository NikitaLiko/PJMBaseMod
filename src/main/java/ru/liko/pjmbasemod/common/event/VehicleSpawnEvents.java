package ru.liko.pjmbasemod.common.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.util.SuperbWarfareVehicleHelper;
import ru.liko.pjmbasemod.common.vehicle.VehicleSpawnSystem;

/**
 * События для системы автоспавна техники SuperbWarfare.
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class VehicleSpawnEvents {

    private VehicleSpawnEvents() {}

    // NeoForge 1.21.1: Use ServerTickEvent.Post instead of TickEvent.ServerTickEvent with Phase.END
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        VehicleSpawnSystem.getInstance().tick();
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        SuperbWarfareVehicleHelper.extractVehicleUUID(entity)
            .ifPresent(uuid -> VehicleSpawnSystem.getInstance().handleVehicleRemoval(uuid));
    }
}

