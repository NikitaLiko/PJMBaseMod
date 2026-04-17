package ru.liko.pjmbasemod.common.rank;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.RankUpdatePacket;
import ru.liko.pjmbasemod.common.network.packet.SyncPjmDataPacket;
import ru.liko.pjmbasemod.common.network.packet.XpGainPacket;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmRank;

/**
 * Manages player rank progression.
 * NeoForge 1.21.1: Uses Data Attachments instead of Capabilities.
 */
public class RankManager {

    public static void addPoints(ServerPlayer player, int points, String reason) {
        // NeoForge 1.21.1: Use getData() instead of getCapability()
        PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
        
        data.addRankPoints(points);
        
        // Send update packet using factory method
        PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(player.getId(), data), player);
        
        if (points != 0) {
            // Send animated popup packet
            PjmNetworking.sendToClient(new XpGainPacket(points, reason), player);
        }

        checkRankChange(player, data);
    }

    private static void checkRankChange(ServerPlayer player, PjmPlayerData data) {
        PjmRank currentRank = data.getRank();
        int points = data.getRankPoints();

        // Check for promotion
        PjmRank nextRank = currentRank.getNext();
        if (nextRank != currentRank && points >= nextRank.getMinPoints() && nextRank.getMinPoints() > 0) {
            promote(player, data, nextRank);
            return;
        }

        // Check for demotion
        if (currentRank.getMinPoints() > 0 && points < currentRank.getMinPoints()) {
            PjmRank prevRank = currentRank.getPrevious();
            if (prevRank != currentRank) {
                demote(player, data, prevRank);
            }
        }
    }

    private static void promote(ServerPlayer player, PjmPlayerData data, PjmRank newRank) {
        data.setRank(newRank);
        PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(player.getId(), data), player);
        PjmNetworking.sendToTracking(SyncPjmDataPacket.fromPlayerData(player.getId(), data), player);
        
        // Send Animation Packet using factory method
        PjmNetworking.sendToClient(RankUpdatePacket.create(newRank, true), player);
        
        Component message = Component.literal("Вы повышены до звания ")
                .withStyle(ChatFormatting.GOLD)
                .append(newRank.getDisplayName());
        player.displayClientMessage(message, false);
    }

    private static void demote(ServerPlayer player, PjmPlayerData data, PjmRank newRank) {
        data.setRank(newRank);
        PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(player.getId(), data), player);
        PjmNetworking.sendToTracking(SyncPjmDataPacket.fromPlayerData(player.getId(), data), player);
        
        // Send Animation Packet using factory method
        PjmNetworking.sendToClient(RankUpdatePacket.create(newRank, false), player);
        
        Component message = Component.literal("Вы понижены до звания ")
                .withStyle(ChatFormatting.RED)
                .append(newRank.getDisplayName());
        player.displayClientMessage(message, false);
    }
}
