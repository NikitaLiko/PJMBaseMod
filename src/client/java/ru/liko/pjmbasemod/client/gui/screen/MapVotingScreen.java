package ru.liko.pjmbasemod.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.liko.pjmbasemod.client.ClientMatchData;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.OpenMapVotingPacket;
import ru.liko.pjmbasemod.common.network.packet.VoteMapPacket;

import java.util.List;

/**
 * Экран голосования за карту — тактический стиль (Squad / Arma Reforger).
 * Тёмный фон, карточки карт с голосами, таймер, accent bars.
 */
public class MapVotingScreen extends Screen {

    // Colors (matched with SpawnSelectionScreen)
    private static final int COLOR_BACKGROUND = 0xFF000000;
    private static final int COLOR_PANEL = 0xFF121212;
    private static final int COLOR_PANEL_BORDER = 0xFF2B2B2B;
    private static final int COLOR_ORANGE_ACCENT = 0xFFE67E22;
    private static final int COLOR_BLUE_ACCENT = 0xFF3498DB;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GREY = 0xFFB0B0B0;
    private static final int COLOR_GREEN = 0xFF27AE60;
    private static final int COLOR_CARD_BG = 0xFF101010;
    private static final int COLOR_CARD_HOVER = 0xFF1E1E1E;
    private static final int COLOR_CARD_SELECTED = 0xFF2C3E50;

    // Layout
    private static final int HEADER_HEIGHT = 50;
    private static final int FOOTER_HEIGHT = 40;
    private static final int CARD_HEIGHT = 70;
    private static final int CARD_SPACING = 8;
    private static final int PADDING = 16;

    private final List<OpenMapVotingPacket.MapEntry> availableMaps;
    private String selectedMapId = null;
    private float fadeIn = 0.0f;
    private long initTime;

    public MapVotingScreen(OpenMapVotingPacket packet) {
        super(Component.literal("Map Vote"));
        this.availableMaps = packet.maps();
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
        g.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
        g.fill(0, HEADER_HEIGHT, this.width, HEADER_HEIGHT + 1, COLOR_PANEL_BORDER);
        int footerTop = this.height - FOOTER_HEIGHT;
        g.fill(0, footerTop, this.width, footerTop + 1, COLOR_PANEL_BORDER);

        // Header
        renderHeader(g);

        // Map Cards
        int contentY = HEADER_HEIGHT + PADDING;
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - PADDING * 2;
        int cardAreaWidth = Math.min(500, this.width - PADDING * 2);
        int cardX = (this.width - cardAreaWidth) / 2;

        if (availableMaps.isEmpty()) {
            g.drawCenteredString(this.font, "NO MAPS AVAILABLE", this.width / 2, this.height / 2, COLOR_GREY);
        } else {
            // Cards with scroll support
            int totalCardsHeight = availableMaps.size() * (CARD_HEIGHT + CARD_SPACING);
            int startY = contentY + Math.max(0, (contentHeight - totalCardsHeight) / 2);

            g.enableScissor(cardX - 2, contentY, cardX + cardAreaWidth + 2, contentY + contentHeight);
            for (int i = 0; i < availableMaps.size(); i++) {
                OpenMapVotingPacket.MapEntry entry = availableMaps.get(i);
                int cardY = startY + i * (CARD_HEIGHT + CARD_SPACING);

                if (cardY + CARD_HEIGHT >= contentY && cardY <= contentY + contentHeight) {
                    boolean hovered = mouseX >= cardX && mouseX <= cardX + cardAreaWidth &&
                            mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;
                    boolean selected = entry.mapId().equals(selectedMapId);
                    renderMapCard(g, entry, cardX, cardY, cardAreaWidth, CARD_HEIGHT, hovered, selected, i);
                }
            }
            g.disableScissor();
        }

        // Footer — timer & hint
        renderFooter(g);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics g) {
        // Title
        g.pose().pushPose();
        g.pose().translate(PADDING + 10, 10, 0);
        g.pose().scale(1.5f, 1.5f, 1.5f);
        g.drawString(this.font, "MAP VOTE", 0, 0, COLOR_WHITE, false);
        g.pose().popPose();

        // Subtitle
        g.drawString(this.font, "SELECT NEXT BATTLEFIELD", PADDING + 10, 35, COLOR_GREY, false);

        // Timer (right side)
        int timer = ClientMatchData.getTimer();
        int seconds = Math.max(0, timer / 20);
        String timerText = String.format("%02d", seconds);
        int timerColor = seconds <= 5 ? 0xFFE74C3C : COLOR_ORANGE_ACCENT;
        int timerWidth = this.font.width(timerText) * 2;

        g.pose().pushPose();
        g.pose().translate(this.width - PADDING - 10 - timerWidth, 8, 0);
        g.pose().scale(2.0f, 2.0f, 2.0f);
        g.drawString(this.font, timerText, 0, 0, timerColor, false);
        g.pose().popPose();

        g.drawString(this.font, "SECONDS LEFT", this.width - PADDING - 10 - this.font.width("SECONDS LEFT"), 35, COLOR_GREY, false);
    }

    private void renderMapCard(GuiGraphics g, OpenMapVotingPacket.MapEntry entry,
                               int x, int y, int w, int h, boolean hovered, boolean selected, int index) {
        // Background
        int bg = selected ? COLOR_CARD_SELECTED : (hovered ? COLOR_CARD_HOVER : COLOR_CARD_BG);
        int alpha = (int)(fadeIn * 240);
        g.fill(x, y, x + w, y + h, withAlpha(bg, alpha));

        // Border
        int borderColor = selected ? COLOR_BLUE_ACCENT : (hovered ? COLOR_ORANGE_ACCENT : COLOR_PANEL_BORDER);
        g.renderOutline(x, y, w, h, withAlpha(borderColor, (int)(fadeIn * 255)));

        // Left accent bar
        if (selected) {
            g.fill(x, y, x + 3, y + h, COLOR_BLUE_ACCENT);
        } else if (hovered) {
            g.fill(x, y, x + 3, y + h, COLOR_ORANGE_ACCENT);
        }

        // Map index badge
        int badgeSize = 28;
        int badgeX = x + 12;
        int badgeY = y + (h - badgeSize) / 2;
        g.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, 0xFF222222);
        g.renderOutline(badgeX, badgeY, badgeSize, badgeSize, COLOR_ORANGE_ACCENT);
        String indexStr = String.valueOf(index + 1);
        g.drawCenteredString(this.font, indexStr, badgeX + badgeSize / 2, badgeY + (badgeSize - 8) / 2, COLOR_ORANGE_ACCENT);

        // Map name
        int textX = badgeX + badgeSize + 14;
        g.pose().pushPose();
        g.pose().translate(textX, y + 14, 0);
        g.pose().scale(1.2f, 1.2f, 1.2f);
        g.drawString(this.font, entry.displayName(), 0, 0, selected ? COLOR_WHITE : 0xFFDDDDDD, false);
        g.pose().popPose();

        // Map ID (subtitle)
        g.drawString(this.font, "ID: " + entry.mapId(), textX, y + 34, 0xFF777777, false);

        // Vote indicator (right side) — shows checkmark if voted
        if (selected) {
            String voteText = "YOUR VOTE";
            int voteWidth = this.font.width(voteText);
            int voteX = x + w - voteWidth - 16;
            int voteY = y + (h - 16) / 2;

            g.fill(voteX - 6, voteY - 2, voteX + voteWidth + 6, voteY + 12, 0xFF1A3A2A);
            g.renderOutline(voteX - 6, voteY - 2, voteWidth + 12, 14, COLOR_GREEN);
            g.drawString(this.font, voteText, voteX, voteY, COLOR_GREEN, false);
        }

        // Vote count badge placeholder (top-right)
        String numText = "#" + (index + 1);
        int numWidth = this.font.width(numText);
        g.drawString(this.font, numText, x + w - numWidth - 12, y + 8, 0xFF555555, false);
    }

    private void renderFooter(GuiGraphics g) {
        int footerY = this.height - FOOTER_HEIGHT;

        // Left: hint
        g.drawString(this.font, "CLICK A MAP TO VOTE  |  VOTE CHANGES ARE ALLOWED",
                PADDING + 10, footerY + (FOOTER_HEIGHT - 8) / 2, COLOR_GREY, false);

        // Right: total maps
        String mapsText = availableMaps.size() + " MAP(S) AVAILABLE";
        int mapsWidth = this.font.width(mapsText);
        g.drawString(this.font, mapsText, this.width - PADDING - 10 - mapsWidth,
                footerY + (FOOTER_HEIGHT - 8) / 2, COLOR_GREY, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !availableMaps.isEmpty()) {
            int cardAreaWidth = Math.min(500, this.width - PADDING * 2);
            int cardX = (this.width - cardAreaWidth) / 2;
            int contentY = HEADER_HEIGHT + PADDING;
            int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - PADDING * 2;
            int totalCardsHeight = availableMaps.size() * (CARD_HEIGHT + CARD_SPACING);
            int startY = contentY + Math.max(0, (contentHeight - totalCardsHeight) / 2);

            for (int i = 0; i < availableMaps.size(); i++) {
                int cardY = startY + i * (CARD_HEIGHT + CARD_SPACING);
                if (mouseX >= cardX && mouseX <= cardX + cardAreaWidth &&
                        mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT) {
                    OpenMapVotingPacket.MapEntry entry = availableMaps.get(i);
                    selectedMapId = entry.mapId();
                    PjmNetworking.sendToServer(new VoteMapPacket(entry.mapId()));
                    return true;
                }
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
        return false;
    }

    private int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }
}
