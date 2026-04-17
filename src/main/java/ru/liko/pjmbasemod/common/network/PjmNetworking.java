package ru.liko.pjmbasemod.common.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.network.packet.*;

/**
 * Network registration for NeoForge 1.21.1.
 * Uses PayloadRegistrar with CustomPacketPayload and StreamCodec.
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class PjmNetworking {

        private static final String PROTOCOL_VERSION = "1.0";

        private PjmNetworking() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
                PayloadRegistrar registrar = event.registrar(Pjmbasemod.MODID)
                                .versioned(PROTOCOL_VERSION);

                // ==================== Server → Client Packets ====================

                // Player data sync
                registrar.playToClient(
                                SyncPjmDataPacket.TYPE,
                                SyncPjmDataPacket.STREAM_CODEC,
                                SyncPjmDataPacket::handle);

                // Class selection (Client → Server request)
                registrar.playToServer(
                                OpenClassSelectionPacket.TYPE,
                                OpenClassSelectionPacket.STREAM_CODEC,
                                OpenClassSelectionPacket::handle);

                registrar.playToClient(
                                SyncClassSelectionDataPacket.TYPE,
                                SyncClassSelectionDataPacket.STREAM_CODEC,
                                SyncClassSelectionDataPacket::handle);

                // Team config
                registrar.playToClient(
                                SyncTeamConfigPacket.TYPE,
                                SyncTeamConfigPacket.STREAM_CODEC,
                                SyncTeamConfigPacket::handle);

                // Death stats
                registrar.playToClient(
                                SyncDeathStatsPacket.TYPE,
                                SyncDeathStatsPacket.STREAM_CODEC,
                                SyncDeathStatsPacket::handle);

                // Team selection
                registrar.playToClient(
                                OpenTeamSelectionPacket.TYPE,
                                OpenTeamSelectionPacket.STREAM_CODEC,
                                OpenTeamSelectionPacket::handle);

                registrar.playToClient(
                                SyncTeamSelectionResultPacket.TYPE,
                                SyncTeamSelectionResultPacket.STREAM_CODEC,
                                SyncTeamSelectionResultPacket::handle);

                // Kits sync
                registrar.playToClient(
                                SyncKitsDataPacket.TYPE,
                                SyncKitsDataPacket.STREAM_CODEC,
                                SyncKitsDataPacket::handle);

                // Chat mode sync
                registrar.playToClient(
                                SyncChatModePacket.TYPE,
                                SyncChatModePacket.STREAM_CODEC,
                                SyncChatModePacket::handle);

                // Control points / Game mode
                registrar.playToClient(
                                SyncControlPointPacket.TYPE,
                                SyncControlPointPacket.STREAM_CODEC,
                                SyncControlPointPacket::handle);

                registrar.playToClient(
                                SyncGameModeDataPacket.TYPE,
                                SyncGameModeDataPacket.STREAM_CODEC,
                                SyncGameModeDataPacket::handle);

                registrar.playToClient(
                                SyncInZoneStatusPacket.TYPE,
                                SyncInZoneStatusPacket.STREAM_CODEC,
                                SyncInZoneStatusPacket::handle);

                // Team balance
                registrar.playToClient(
                                SyncTeamBalancePacket.TYPE,
                                SyncTeamBalancePacket.STREAM_CODEC,
                                SyncTeamBalancePacket::handle);

                // Customization options
                registrar.playToClient(
                                SyncCustomizationOptionsPacket.TYPE,
                                SyncCustomizationOptionsPacket.STREAM_CODEC,
                                SyncCustomizationOptionsPacket::handle);

                // Rank update
                registrar.playToClient(
                                RankUpdatePacket.TYPE,
                                RankUpdatePacket.STREAM_CODEC,
                                RankUpdatePacket::handle);

                // XP Gain
                registrar.playToClient(
                                XpGainPacket.TYPE,
                                XpGainPacket.STREAM_CODEC,
                                XpGainPacket::handle);

                // Match State Sync
                registrar.playToClient(
                                SyncMatchStatePacket.TYPE,
                                SyncMatchStatePacket.STREAM_CODEC,
                                SyncMatchStatePacket::handle);

                // Map Info Sync
                registrar.playToClient(
                                SyncMapInfoPacket.TYPE,
                                SyncMapInfoPacket.STREAM_CODEC,
                                SyncMapInfoPacket::handle);

                // Team Tickets Sync
                registrar.playToClient(
                                SyncTeamTicketsPacket.TYPE,
                                SyncTeamTicketsPacket.STREAM_CODEC,
                                SyncTeamTicketsPacket::handle);

                // Match Stats Screen (Server → Client)
                registrar.playToClient(
                                OpenMatchStatsPacket.TYPE,
                                OpenMatchStatsPacket.STREAM_CODEC,
                                OpenMatchStatsPacket::handle);

                // Map Voting Screen (Server → Client)
                registrar.playToClient(
                                OpenMapVotingPacket.TYPE,
                                OpenMapVotingPacket.STREAM_CODEC,
                                OpenMapVotingPacket::handle);

                // Spawn Menu (Server → Client)
                registrar.playToClient(
                                OpenSpawnMenuPacket.TYPE,
                                OpenSpawnMenuPacket.STREAM_CODEC,
                                OpenSpawnMenuPacket::handle);

                // Match Lobby Screen (Server → Client)
                registrar.playToClient(
                                OpenMatchLobbyPacket.TYPE,
                                OpenMatchLobbyPacket.STREAM_CODEC,
                                OpenMatchLobbyPacket::handle);

                // Radio Event (Server -> Client)
                registrar.playToClient(
                                RadioEventPacket.TYPE,
                                RadioEventPacket.STREAM_CODEC,
                                RadioEventPacket::handle);

                // ==================== Client → Server Packets ====================

                // Class selection
                registrar.playToServer(
                                SelectClassPacket.TYPE,
                                SelectClassPacket.STREAM_CODEC,
                                SelectClassPacket::handle);

                // Ammo refill
                registrar.playToServer(
                                RefillAmmunitionPacket.TYPE,
                                RefillAmmunitionPacket.STREAM_CODEC,
                                RefillAmmunitionPacket::handle);

                // Team selection
                registrar.playToServer(
                                SelectTeamPacket.TYPE,
                                SelectTeamPacket.STREAM_CODEC,
                                SelectTeamPacket::handle);

                // Map voting
                registrar.playToServer(
                                VoteMapPacket.TYPE,
                                VoteMapPacket.STREAM_CODEC,
                                VoteMapPacket::handle);

                // Chat mode change
                registrar.playToServer(
                                ChangeChatModePacket.TYPE,
                                ChangeChatModePacket.STREAM_CODEC,
                                ChangeChatModePacket::handle);

                // Customization selection
                registrar.playToServer(
                                SelectCustomizationPacket.TYPE,
                                SelectCustomizationPacket.STREAM_CODEC,
                                SelectCustomizationPacket::handle);

                // Spawn selection
                registrar.playToServer(
                                SelectSpawnPacket.TYPE,
                                SelectSpawnPacket.STREAM_CODEC,
                                SelectSpawnPacket::handle);

                // Radio switch
                registrar.playToServer(
                                RadioSwitchPacket.TYPE,
                                RadioSwitchPacket.STREAM_CODEC,
                                RadioSwitchPacket::handle);

        }

        // ==================== Utility Methods ====================

        /**
         * Send a packet to a specific player (Server → Client)
         */
        public static void sendToClient(CustomPacketPayload payload, ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, payload);
        }

        /**
         * Send a packet to all players (Server → Client)
         */
        public static void sendToAll(CustomPacketPayload payload) {
                PacketDistributor.sendToAllPlayers(payload);
        }

        /**
         * Send a packet to all players tracking an entity (Server → Client)
         */
        public static void sendToTracking(CustomPacketPayload payload, net.minecraft.world.entity.Entity entity) {
                PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
        }

        /**
         * Send a packet to the server (Client → Server)
         */
        public static void sendToServer(CustomPacketPayload payload) {
                PacketDistributor.sendToServer(payload);
        }
}
