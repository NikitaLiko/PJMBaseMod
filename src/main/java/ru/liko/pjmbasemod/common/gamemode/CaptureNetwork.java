package ru.liko.pjmbasemod.common.gamemode;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.SyncControlPointPacket;
import ru.liko.pjmbasemod.common.network.packet.SyncGameModeDataPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилиты рассылки сетевых пакетов для режимов захвата.
 */
public final class CaptureNetwork {

    private CaptureNetwork() {}

    public static void syncAll(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ControlPointManager manager = ControlPointManager.get(level);

        List<ControlPointSnapshot> snapshots = new ArrayList<>();
        manager.getAll().forEach(point -> snapshots.add(ControlPointSnapshot.from(point)));

        SyncGameModeDataPacket packet = new SyncGameModeDataPacket(snapshots);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PjmNetworking.sendToClient(packet, player);
        }
    }

    public static void syncPoint(ServerLevel level, ControlPoint point) {
        ControlPointSnapshot snapshot = ControlPointSnapshot.from(point);
        SyncControlPointPacket packet = new SyncControlPointPacket(snapshot);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            PjmNetworking.sendToClient(packet, player);
        }
    }

    public static void syncAllTo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ControlPointManager manager = ControlPointManager.get(level);
        List<ControlPointSnapshot> snapshots = new ArrayList<>();
        manager.getAll().forEach(point -> snapshots.add(ControlPointSnapshot.from(point)));
        
        PjmNetworking.sendToClient(new SyncGameModeDataPacket(snapshots), player);
    }
}

