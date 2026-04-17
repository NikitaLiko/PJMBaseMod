package ru.liko.pjmbasemod.client.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.client.gui.screen.ClassSelectionScreen;
import ru.liko.pjmbasemod.client.gui.screen.MapVotingScreen;
import ru.liko.pjmbasemod.client.gui.screen.MatchLobbyScreen;
import ru.liko.pjmbasemod.client.gui.screen.MatchStatsScreen;
import ru.liko.pjmbasemod.client.gui.screen.SpawnSelectionScreen;
import ru.liko.pjmbasemod.client.gui.screen.TeamSelectionScreen;
import ru.liko.pjmbasemod.common.network.packet.*;
import ru.liko.pjmbasemod.common.player.PjmAttachments;

/**
 * Клиентские обработчики пакетов.
 * Эти методы вызываются напрямую из обработчиков пакетов, а не через event bus.
 */
public class ClientPacketHandlers {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Обрабатывает пакет синхронизации данных выбора класса на клиенте
     */
    public static void handleSyncClassSelectionData(SyncClassSelectionDataPacket packet) {
        LOGGER.info("[WRB CLIENT] Received SyncClassSelectionDataPacket");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            LOGGER.warn("[WRB CLIENT] Player is null, cannot open class selection screen");
            return;
        }

        LOGGER.info("[WRB CLIENT] Opening ClassSelectionScreen for team: {}, locked classes: {}", 
            packet.getPlayerTeamName(), packet.getLockedClasses());
        // NeoForge 1.21.1: Use Data Attachments instead of Capabilities
        ru.liko.pjmbasemod.common.player.PjmPlayerData data = minecraft.player.getData(PjmAttachments.PLAYER_DATA);
        if (data != null) {
            // Открываем экран выбора класса с командой игрока и заблокированными классами
            minecraft.setScreen(new ClassSelectionScreen(
                data.getPlayerClass(),
                packet.getClassLimits(),
                packet.getClassPlayerCounts(),
                packet.getPlayerTeamName(),
                packet.getLockedClasses()
            ));
            LOGGER.info("[WRB CLIENT] ClassSelectionScreen opened successfully");
        }
    }

    /**
     * Обрабатывает пакет синхронизации состояния нахождения в зоне
     */
    public static void handleSyncInZoneStatus(SyncInZoneStatusPacket packet) {
        ru.liko.pjmbasemod.client.gui.overlay.HudOverlay.setInZoneStatus(packet.isInZone());
    }

    /**
     * Обрабатывает пакет открытия меню выбора команды
     */
    public static void handleOpenTeamSelection(OpenTeamSelectionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(new TeamSelectionScreen(
            packet.getTeamBalanceInfo(),
            packet.getTeam1Name(),
            packet.getTeam2Name(),
            packet.isAllowBack(),
            packet.getBalanceThreshold()
        ));
    }

    /**
     * Обрабатывает результат выбора команды
     */
    public static void handleSyncTeamSelectionResult(SyncTeamSelectionResultPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TeamSelectionScreen teamScreen) {
            if (packet.isSuccess()) {
                teamScreen.showSuccess(packet.getMessage());
            } else {
                teamScreen.showError(packet.getMessage());
            }
        } else if (!packet.isSuccess()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(packet.getMessage()), false);
            }
        }
    }

    /**
     * Обрабатывает пакет синхронизации баланса команд
     */
    public static void handleSyncTeamBalance(SyncTeamBalancePacket packet) {
        ru.liko.pjmbasemod.client.gui.overlay.HudOverlay.setTeamBalance(
            packet.getTeam1Name(), 
            packet.getTeam1Balance(), 
            packet.getTeam2Name(), 
            packet.getTeam2Balance()
        );
    }

    /**
     * Обрабатывает пакет открытия экрана статистики матча
     */
    public static void handleOpenMatchStats(OpenMatchStatsPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            LOGGER.warn("[PJM CLIENT] Player is null, cannot open match stats screen");
            return;
        }

        LOGGER.info("[PJM CLIENT] Opening MatchStatsScreen. Winner: {}, Players: {}",
            packet.getWinnerTeam(), packet.getPlayerStats().size());
        minecraft.setScreen(new MatchStatsScreen(packet));
    }

    /**
     * Обрабатывает пакет открытия экрана голосования за карту
     */
    public static void handleOpenMapVoting(OpenMapVotingPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            LOGGER.warn("[PJM CLIENT] Player is null, cannot open map voting screen");
            return;
        }

        LOGGER.info("[PJM CLIENT] Opening MapVotingScreen with {} maps", packet.maps().size());
        minecraft.setScreen(new MapVotingScreen(packet));
    }

    /**
     * Обрабатывает пакет открытия экрана выбора точки спавна
     */
    public static void handleOpenSpawnMenu(OpenSpawnMenuPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            LOGGER.warn("[PJM CLIENT] Player is null, cannot open spawn selection screen");
            return;
        }

        LOGGER.info("[PJM CLIENT] Opening SpawnSelectionScreen with {} spawn points", packet.getSpawnPoints().size());
        minecraft.setScreen(new SpawnSelectionScreen(packet));
    }

    /**
     * Обрабатывает пакет открытия экрана лобби матча
     */
    public static void handleOpenMatchLobby(OpenMatchLobbyPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            LOGGER.warn("[PJM CLIENT] Player is null, cannot open match lobby screen");
            return;
        }

        LOGGER.info("[PJM CLIENT] Opening MatchLobbyScreen");
        minecraft.setScreen(new MatchLobbyScreen(minecraft.screen));
    }

}

