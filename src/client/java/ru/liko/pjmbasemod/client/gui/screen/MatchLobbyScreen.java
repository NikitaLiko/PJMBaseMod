package ru.liko.pjmbasemod.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.liko.pjmbasemod.client.ClientMatchData;
import ru.liko.pjmbasemod.client.ClientTeamConfig;
import ru.liko.pjmbasemod.common.match.MatchState;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.SelectTeamPacket;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pre-Match Screen — tactical design (Squad / Arma Reforger).
 * Displays team rosters, map info, countdown, and team selection.
 */
public class MatchLobbyScreen extends Screen {

    // Colors (matched with SpawnSelectionScreen / MapVotingScreen)
    private static final int COLOR_BG = 0xFF000000;
    private static final int COLOR_PANEL = 0xFF121212;
    private static final int COLOR_BORDER = 0xFF2B2B2B;
    private static final int COLOR_ORANGE = 0xFFE67E22;
    private static final int COLOR_BLUE = 0xFF3498DB;
    private static final int COLOR_RED = 0xFFE74C3C;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GREY = 0xFFB0B0B0;
    private static final int COLOR_GREEN = 0xFF27AE60;
    private static final int COLOR_TEAM1 = 0xFF3498DB; // Blue
    private static final int COLOR_TEAM2 = 0xFFE74C3C; // Red

    // Layout
    private static final int HEADER_HEIGHT = 60;
    private static final int FOOTER_HEIGHT = 50;
    private static final int PADDING = 12;
    private static final int PLAYER_ROW_HEIGHT = 18;

    private final Screen lastScreen;
    private float fadeIn = 0.0f;
    private long initTime;

    // Hover state for team join buttons
    private boolean hoverTeam1 = false;
    private boolean hoverTeam2 = false;

    public MatchLobbyScreen(Screen lastScreen) {
        super(Component.literal("Pre-Match"));
        this.lastScreen = lastScreen;
        this.initTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Отключаем ванильный блюр — свой фон рисуем в render()
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.currentTimeMillis() - initTime;
        fadeIn = Math.min(1.0f, elapsed / 300.0f);

        // Background
        g.fill(0, 0, this.width, this.height, COLOR_BG);
        g.fill(0, HEADER_HEIGHT, this.width, HEADER_HEIGHT + 1, COLOR_BORDER);
        int footerTop = this.height - FOOTER_HEIGHT;
        g.fill(0, footerTop, this.width, footerTop + 1, COLOR_BORDER);

        // Header
        renderHeader(g);

        // Content area
        int contentY = HEADER_HEIGHT + PADDING;
        int contentH = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - PADDING * 2;
        int centerGap = 20;
        int panelWidth = (this.width - PADDING * 3 - centerGap) / 2;
        int leftX = PADDING;
        int rightX = PADDING + panelWidth + centerGap + PADDING;

        // Team 1 Panel (left)
        renderTeamPanel(g, leftX, contentY, panelWidth, contentH,
                ClientTeamConfig.getTeam1Name(), COLOR_TEAM1, mouseX, mouseY, true);

        // Team 2 Panel (right)
        renderTeamPanel(g, rightX, contentY, panelWidth, contentH,
                ClientTeamConfig.getTeam2Name(), COLOR_TEAM2, mouseX, mouseY, false);

        // Center: Map info + VS
        renderCenterInfo(g, leftX + panelWidth, contentY, centerGap + PADDING * 2, contentH);

        // Footer
        renderFooter(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics g) {
        // Title
        g.pose().pushPose();
        g.pose().translate(PADDING + 10, 10, 0);
        g.pose().scale(1.5f, 1.5f, 1.5f);
        g.drawString(this.font, "PRE-MATCH LOBBY", 0, 0, COLOR_WHITE, false);
        g.pose().popPose();

        // Map name
        String mapName = ClientMatchData.getCurrentMapDisplayName();
        if (mapName != null && !mapName.isEmpty()) {
            g.drawString(this.font, "MAP: " + mapName.toUpperCase(), PADDING + 10, 38, COLOR_ORANGE, false);
        }

        // State
        MatchState state = ClientMatchData.getState();
        String stateText = switch (state) {
            case WAITING_FOR_PLAYERS -> "WAITING FOR PLAYERS";
            case STARTING -> "MATCH STARTING";
            case IN_PROGRESS -> "IN PROGRESS";
            default -> state.name();
        };
        g.drawString(this.font, stateText, PADDING + 10, 50, COLOR_GREY, false);

        // Timer (right side)
        int timer = ClientMatchData.getTimer();
        int seconds = Math.max(0, timer / 20);
        if (state == MatchState.STARTING && seconds > 0) {
            String timerText = String.valueOf(seconds);
            int timerColor = seconds <= 5 ? COLOR_RED : COLOR_ORANGE;

            g.pose().pushPose();
            int timerW = this.font.width(timerText) * 3;
            g.pose().translate(this.width - PADDING - 10 - timerW, 8, 0);
            g.pose().scale(3.0f, 3.0f, 3.0f);
            g.drawString(this.font, timerText, 0, 0, timerColor, false);
            g.pose().popPose();
        }
    }

    private void renderTeamPanel(GuiGraphics g, int x, int y, int w, int h,
                                  String teamName, int teamColor, int mouseX, int mouseY, boolean isTeam1) {
        // Panel background
        int alpha = (int)(fadeIn * 230);
        g.fill(x, y, x + w, y + h, withAlpha(COLOR_PANEL, alpha));
        g.renderOutline(x, y, w, h, withAlpha(COLOR_BORDER, (int)(fadeIn * 255)));

        // Top accent bar
        g.fill(x, y, x + w, y + 2, teamColor);

        // Team header
        int headerY = y + 8;
        g.pose().pushPose();
        g.pose().translate(x + 12, headerY, 0);
        g.pose().scale(1.3f, 1.3f, 1.3f);
        g.drawString(this.font, teamName.toUpperCase(), 0, 0, teamColor, false);
        g.pose().popPose();

        // Player count
        List<PlayerInfo> teamPlayers = getTeamPlayers(teamName);
        String countText = teamPlayers.size() + " PLAYER(S)";
        g.drawString(this.font, countText, x + w - this.font.width(countText) - 12, headerY + 4, COLOR_GREY, false);

        // Separator
        int sepY = headerY + 22;
        g.fill(x + 8, sepY, x + w - 8, sepY + 1, COLOR_BORDER);

        // Player list
        int listY = sepY + 6;
        int maxVisible = (y + h - listY - 40) / PLAYER_ROW_HEIGHT;

        for (int i = 0; i < Math.min(teamPlayers.size(), maxVisible); i++) {
            PlayerInfo p = teamPlayers.get(i);
            int rowY = listY + i * PLAYER_ROW_HEIGHT;

            // Row highlight on hover
            boolean rowHover = mouseX >= x + 8 && mouseX <= x + w - 8 &&
                    mouseY >= rowY && mouseY <= rowY + PLAYER_ROW_HEIGHT;
            if (rowHover) {
                g.fill(x + 8, rowY, x + w - 8, rowY + PLAYER_ROW_HEIGHT, 0x20FFFFFF);
            }

            // Index
            g.drawString(this.font, String.valueOf(i + 1) + ".", x + 14, rowY + 4, 0xFF555555, false);

            // Player name
            g.drawString(this.font, p.getProfile().getName(), x + 32, rowY + 4, COLOR_WHITE, false);
        }

        if (teamPlayers.size() > maxVisible) {
            g.drawString(this.font, "+" + (teamPlayers.size() - maxVisible) + " more...",
                    x + 14, listY + maxVisible * PLAYER_ROW_HEIGHT + 4, COLOR_GREY, false);
        }

        // Join button at bottom
        int btnW = w - 24;
        int btnH = 24;
        int btnX = x + 12;
        int btnY = y + h - btnH - 8;

        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW &&
                mouseY >= btnY && mouseY <= btnY + btnH;

        if (isTeam1) hoverTeam1 = btnHover;
        else hoverTeam2 = btnHover;

        int btnBg = btnHover ? teamColor : 0xFF1A1A1A;
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
        g.renderOutline(btnX, btnY, btnW, btnH, teamColor);

        int btnTextColor = btnHover ? COLOR_WHITE : teamColor;
        g.drawCenteredString(this.font, "JOIN " + teamName.toUpperCase(),
                btnX + btnW / 2, btnY + (btnH - 8) / 2, btnTextColor);
    }

    private void renderCenterInfo(GuiGraphics g, int x, int y, int w, int h) {
        int centerX = x + w / 2;

        // VS text
        int vsY = y + h / 2 - 20;
        g.pose().pushPose();
        g.pose().translate(centerX - this.font.width("VS") * 1.5f, vsY, 0);
        g.pose().scale(3.0f, 3.0f, 3.0f);
        g.drawString(this.font, "VS", 0, 0, 0xFF444444, false);
        g.pose().popPose();
    }

    private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
        int footerY = this.height - FOOTER_HEIGHT;

        // Left: info
        g.drawString(this.font, "SELECT A TEAM TO JOIN  |  PRESS ESC TO CLOSE",
                PADDING + 10, footerY + (FOOTER_HEIGHT - 8) / 2, COLOR_GREY, false);

        // Right: status indicator
        MatchState state = ClientMatchData.getState();
        String statusText;
        int statusColor;
        if (state == MatchState.STARTING) {
            statusText = "STARTING SOON";
            statusColor = COLOR_ORANGE;
        } else if (state == MatchState.WAITING_FOR_PLAYERS) {
            statusText = "WAITING...";
            statusColor = COLOR_GREY;
        } else {
            statusText = state.name();
            statusColor = COLOR_GREEN;
        }
        int statusWidth = this.font.width(statusText);
        g.drawString(this.font, statusText, this.width - PADDING - 10 - statusWidth,
                footerY + (FOOTER_HEIGHT - 8) / 2, statusColor, false);
    }

    private List<PlayerInfo> getTeamPlayers(String teamName) {
        if (this.minecraft == null || this.minecraft.getConnection() == null) {
            return List.of();
        }
        Collection<PlayerInfo> players = this.minecraft.getConnection().getOnlinePlayers();
        return players.stream()
                .filter(p -> p.getTeam() != null && p.getTeam().getName().equals(teamName))
                .collect(Collectors.toList());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check team join buttons
            String team1Name = ClientTeamConfig.getTeam1Name();
            String team2Name = ClientTeamConfig.getTeam2Name();

            if (hoverTeam1) {
                PjmNetworking.sendToServer(new SelectTeamPacket(team1Name));
                return true;
            }
            if (hoverTeam2) {
                PjmNetworking.sendToServer(new SelectTeamPacket(team2Name));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }
}
