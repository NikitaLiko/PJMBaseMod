package ru.liko.pjmbasemod.common.network;

import ru.liko.pjmbasemod.common.KitDefinition;
import ru.liko.pjmbasemod.common.chat.ChatMode;
import ru.liko.pjmbasemod.common.customization.CustomizationOption;
import ru.liko.pjmbasemod.common.gamemode.ControlPointSnapshot;
import ru.liko.pjmbasemod.common.match.MatchState;
import ru.liko.pjmbasemod.common.network.packet.*;

import java.util.List;
import java.util.Map;

/**
 * Прокси для клиентских обработчиков пакетов.
 * Реализация регистрируется на клиенте через
 * {@link #setInstance(ClientPacketProxy)}.
 * На dedicated сервере instance остаётся null и обработчики не вызываются.
 */
public abstract class ClientPacketProxy {

    private static ClientPacketProxy instance;

    public static void setInstance(ClientPacketProxy proxy) {
        instance = proxy;
    }

    public static ClientPacketProxy get() {
        return instance;
    }

    // ==================== S2C Packet Handlers ====================

    /** SyncPjmDataPacket */
    public abstract void handleSyncPjmData(int entityId, String rankId, int rankPoints,
            String playerClassId, String teamId,
            String activeSkinId, List<String> activeItemIds);

    /** SyncMatchStatePacket */
    public abstract void handleSyncMatchState(MatchState state, int timer);

    /** SyncTeamTicketsPacket */
    public abstract void handleSyncTeamTickets(String team1, int tickets1, String team2, int tickets2);

    /** SyncMapInfoPacket */
    public abstract void handleSyncMapInfo(String mapId, String mapDisplayName);

    /** SyncChatModePacket */
    public abstract void handleSyncChatMode(ChatMode mode);

    /** SyncKitsDataPacket */
    public abstract void handleSyncKitsData(Map<String, Map<String, List<KitDefinition>>> kitsData);

    /** SyncTeamConfigPacket */
    public abstract void handleSyncTeamConfig(String team1Id, String team1DisplayName,
            String team2Id, String team2DisplayName, int balanceThreshold);

    /** SyncControlPointPacket */
    public abstract void handleSyncControlPoint(ControlPointSnapshot snapshot);

    /** SyncGameModeDataPacket */
    public abstract void handleSyncGameModeData(List<ControlPointSnapshot> snapshots);

    /** SyncDeathStatsPacket */
    public abstract void handleSyncDeathStats(String teamId, int playerDeaths, int teamDeaths);

    /** SyncInZoneStatusPacket */
    public abstract void handleSyncInZoneStatus(boolean inZone);

    /** SyncTeamBalancePacket */
    public abstract void handleSyncTeamBalance(SyncTeamBalancePacket packet);

    /** SyncTeamSelectionResultPacket */
    public abstract void handleSyncTeamSelectionResult(SyncTeamSelectionResultPacket packet);

    /** SyncClassSelectionDataPacket */
    public abstract void handleSyncClassSelectionData(SyncClassSelectionDataPacket packet);

    /** OpenTeamSelectionPacket */
    public abstract void handleOpenTeamSelection(OpenTeamSelectionPacket packet);

    /** OpenMatchStatsPacket */
    public abstract void handleOpenMatchStats(OpenMatchStatsPacket packet);

    /** OpenMapVotingPacket */
    public abstract void handleOpenMapVoting(OpenMapVotingPacket packet);

    /** OpenSpawnMenuPacket */
    public abstract void handleOpenSpawnMenu(OpenSpawnMenuPacket packet);

    /** OpenMatchLobbyPacket */
    public abstract void handleOpenMatchLobby(OpenMatchLobbyPacket packet);

    /** RankUpdatePacket */
    public abstract void handleRankUpdate(String rankId, boolean promotion);

    /** XpGainPacket */
    public abstract void handleXpGain(int amount, String reason);

    /** SyncCustomizationOptionsPacket */
    public abstract void handleSyncCustomizationOptions(List<CustomizationOption> options);

    /** RadioEventPacket */
    public abstract void handleRadioEvent(boolean isStart);
}
