package ru.liko.pjmbasemod.client.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.client.ClientKitsCache;
import ru.liko.pjmbasemod.client.ClientMatchData;
import ru.liko.pjmbasemod.client.ClientPlayerDataCache;
import ru.liko.pjmbasemod.client.ClientTeamConfig;
import ru.liko.pjmbasemod.client.capture.ClientCaptureData;
import ru.liko.pjmbasemod.client.chat.ClientChatData;
import ru.liko.pjmbasemod.client.death.ClientDeathStats;
import ru.liko.pjmbasemod.client.gui.overlay.HudOverlay;
import ru.liko.pjmbasemod.client.gui.overlay.RankUpdateOverlay;
import ru.liko.pjmbasemod.client.gui.overlay.XpGainOverlay;
import ru.liko.pjmbasemod.common.KitDefinition;
import ru.liko.pjmbasemod.common.chat.ChatMode;
import ru.liko.pjmbasemod.common.customization.CustomizationManager;
import ru.liko.pjmbasemod.common.customization.CustomizationOption;
import ru.liko.pjmbasemod.common.gamemode.ControlPointSnapshot;
import ru.liko.pjmbasemod.common.match.MatchState;
import ru.liko.pjmbasemod.common.network.ClientPacketProxy;
import ru.liko.pjmbasemod.common.network.packet.*;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmRank;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Клиентская реализация {@link ClientPacketProxy}.
 * Содержит всю логику обработки S2C пакетов на стороне клиента.
 * Регистрируется при загрузке клиента через
 * {@link ClientPacketProxy#setInstance(ClientPacketProxy)}.
 */
public class ClientPacketProxyImpl extends ClientPacketProxy {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void handleSyncPjmData(int entityId, String rankId, int rankPoints,
            String playerClassId, String teamId,
            String activeSkinId, List<String> activeItemIds) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Entity entity = minecraft.level.getEntity(entityId);
            if (entity instanceof Player player) {
                PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
                data.setRank(PjmRank.fromKeyOrDefault(rankId));
                data.setRankPoints(rankPoints);
                data.setPlayerClass(PjmPlayerClass.fromIdOrDefault(playerClassId));
                data.setTeam(teamId);
                data.setActiveSkinId(activeSkinId);
                data.setActiveItemIds(new HashSet<>(activeItemIds));

                ClientPlayerDataCache.update(
                        player.getUUID(),
                        PjmRank.fromKeyOrDefault(rankId),
                        PjmPlayerClass.fromIdOrDefault(playerClassId),
                        teamId);
            }
        }
    }

    @Override
    public void handleSyncMatchState(MatchState state, int timer) {
        ClientMatchData.update(state, timer);
    }

    @Override
    public void handleSyncTeamTickets(String team1, int tickets1, String team2, int tickets2) {
        ClientMatchData.updateTickets(team1, tickets1, team2, tickets2);
    }

    @Override
    public void handleSyncMapInfo(String mapId, String mapDisplayName) {
        ClientMatchData.updateMapInfo(mapId, mapDisplayName);
    }

    @Override
    public void handleSyncChatMode(ChatMode mode) {
        ClientChatData.setChatMode(mode);
    }

    @Override
    public void handleSyncKitsData(Map<String, Map<String, List<KitDefinition>>> kitsData) {
        ClientKitsCache.setKitsData(kitsData);
        LOGGER.info("[PJM] ClientKitsCache обновлён, hasData: {}", ClientKitsCache.hasData());
    }

    @Override
    public void handleSyncTeamConfig(String team1Id, String team1DisplayName,
            String team2Id, String team2DisplayName, int balanceThreshold) {
        ClientTeamConfig.setTeamConfig(team1Id, team1DisplayName, team2Id, team2DisplayName, balanceThreshold);
    }

    @Override
    public void handleSyncControlPoint(ControlPointSnapshot snapshot) {
        ClientCaptureData.updatePoint(snapshot);
    }

    @Override
    public void handleSyncGameModeData(List<ControlPointSnapshot> snapshots) {
        ClientCaptureData.applyFullSync(snapshots);
    }

    @Override
    public void handleSyncDeathStats(String teamId, int playerDeaths, int teamDeaths) {
        ClientDeathStats.update(teamId, playerDeaths, teamDeaths);
    }

    @Override
    public void handleSyncInZoneStatus(boolean inZone) {
        HudOverlay.setInZoneStatus(inZone);
    }

    @Override
    public void handleSyncTeamBalance(SyncTeamBalancePacket packet) {
        HudOverlay.setTeamBalance(
                packet.getTeam1Name(), packet.getTeam1Balance(),
                packet.getTeam2Name(), packet.getTeam2Balance());
    }

    @Override
    public void handleSyncTeamSelectionResult(SyncTeamSelectionResultPacket packet) {
        ClientPacketHandlers.handleSyncTeamSelectionResult(packet);
    }

    @Override
    public void handleSyncClassSelectionData(SyncClassSelectionDataPacket packet) {
        ClientPacketHandlers.handleSyncClassSelectionData(packet);
    }

    @Override
    public void handleOpenTeamSelection(OpenTeamSelectionPacket packet) {
        ClientPacketHandlers.handleOpenTeamSelection(packet);
    }

    @Override
    public void handleOpenMatchStats(OpenMatchStatsPacket packet) {
        ClientPacketHandlers.handleOpenMatchStats(packet);
    }

    @Override
    public void handleOpenMapVoting(OpenMapVotingPacket packet) {
        ClientPacketHandlers.handleOpenMapVoting(packet);
    }

    @Override
    public void handleOpenSpawnMenu(OpenSpawnMenuPacket packet) {
        ClientPacketHandlers.handleOpenSpawnMenu(packet);
    }

    @Override
    public void handleOpenMatchLobby(OpenMatchLobbyPacket packet) {
        ClientPacketHandlers.handleOpenMatchLobby(packet);
    }

    @Override
    public void handleRankUpdate(String rankId, boolean promotion) {
        PjmRank rank = PjmRank.fromKeyOrDefault(rankId);
        RankUpdateOverlay.show(rank, promotion);
    }

    @Override
    public void handleXpGain(int amount, String reason) {
        XpGainOverlay.show(amount, reason);
    }

    @Override
    public void handleSyncCustomizationOptions(List<CustomizationOption> options) {
        CustomizationManager manager = CustomizationManager.getClientInstance();
        manager.clear();
        for (CustomizationOption option : options) {
            manager.addOption(option);
            LOGGER.debug("[PJM] Client loaded option: {} ({})", option.getId(), option.getType());
        }
        LOGGER.info("[PJM] Client CustomizationManager now has {} options", manager.getAllOptions().size());
    }

    @Override
    public void handleRadioEvent(boolean isStart) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (isStart) {
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                        .forUI(ru.liko.pjmbasemod.common.init.PjmSounds.RADIO_START.get(), 1.0F, 1.0F));
                ru.liko.pjmbasemod.client.event.KeyInputHandler.activeTeammateRadios++;
            } else {
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                        .forUI(ru.liko.pjmbasemod.common.init.PjmSounds.RADIO_END.get(), 1.0F, 1.0F));
                ru.liko.pjmbasemod.client.event.KeyInputHandler.activeTeammateRadios--;
                if (ru.liko.pjmbasemod.client.event.KeyInputHandler.activeTeammateRadios < 0) {
                    ru.liko.pjmbasemod.client.event.KeyInputHandler.activeTeammateRadios = 0;
                }
            }
        }
    }
}
