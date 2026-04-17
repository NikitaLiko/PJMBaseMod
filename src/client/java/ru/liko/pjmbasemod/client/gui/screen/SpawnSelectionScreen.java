package ru.liko.pjmbasemod.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.OpenSpawnMenuPacket;
import ru.liko.pjmbasemod.common.network.packet.SelectSpawnPacket;

import java.util.List;

/**
 * Redesigned Spawn Selection Screen.
 * Squad/Arma style: Dark, clean, responsive.
 * Matches the design language of ClassSelectionScreen.
 */
public class SpawnSelectionScreen extends Screen {

    // Colors (Matched with ClassSelectionScreen)
    private static final int COLOR_BACKGROUND = 0xFF000000;
    private static final int COLOR_PANEL = 0xFF121212;
    private static final int COLOR_PANEL_BORDER = 0xFF2B2B2B;
    private static final int COLOR_ORANGE_ACCENT = 0xFFE67E22;
    private static final int COLOR_BLUE_ACCENT = 0xFF3498DB;
    private static final int COLOR_WHITE_TEXT = 0xFFFFFFFF;
    private static final int COLOR_GREY_TEXT = 0xFFB0B0B0;
    private static final int COLOR_GREEN = 0xFF27AE60;
    private static final int COLOR_RED = 0xFFE74C3C;
    private static final int COLOR_CARD_BG = 0x90101010;
    private static final int COLOR_CARD_HOVER = 0xFF1E1E1E;

    // Layout constants
    private static final int PADDING = 10;
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 50;
    private static final int LIST_ITEM_HEIGHT = 50;
    private static final int LIST_ITEM_SPACING = 6;

    private final List<OpenSpawnMenuPacket.SpawnPoint> spawnPoints;
    private final int respawnCooldown;
    
    private OpenSpawnMenuPacket.SpawnPoint selectedSpawnPoint;
    
    // Scrolling
    private int scrollOffset = 0;
    private int maxScroll = 0;
    
    // Animation
    private float fadeIn = 0.0f;
    private long initTime;

    public SpawnSelectionScreen(OpenSpawnMenuPacket packet) {
        super(Component.literal("Spawn Selection"));
        this.spawnPoints = packet.getSpawnPoints();
        this.respawnCooldown = packet.getRespawnCooldown();
        
        // Auto-select first available
        for (OpenSpawnMenuPacket.SpawnPoint sp : spawnPoints) {
            if (sp.available()) {
                this.selectedSpawnPoint = sp;
                break;
            }
        }
        if (this.selectedSpawnPoint == null && !spawnPoints.isEmpty()) {
            this.selectedSpawnPoint = spawnPoints.get(0);
        }
        
        this.initTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 160;
        int buttonHeight = 30;
        int footerY = this.height - FOOTER_HEIGHT + (FOOTER_HEIGHT - buttonHeight) / 2;
        int rightX = this.width - PADDING - buttonWidth;
        
        // Deploy Button
        this.addRenderableWidget(new TacticalButton(rightX, footerY, buttonWidth, buttonHeight, 
            Component.literal("DEPLOY"), 
            btn -> deploy()));
    }
    
    private void deploy() {
        if (selectedSpawnPoint != null && selectedSpawnPoint.available()) {
            PjmNetworking.sendToServer(new SelectSpawnPacket(selectedSpawnPoint.id()));
            this.onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Отключаем ванильный блюр — свой фон рисуем в render()
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        renderBackdrop(graphics);
        
        // Fade in animation
        long elapsed = System.currentTimeMillis() - initTime;
        fadeIn = Math.min(1.0f, elapsed / 300.0f);
        
        // Layout
        int contentY = HEADER_HEIGHT + PADDING;
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2);
        
        int listWidth = (int)(this.width * 0.35f);
        int detailsWidth = this.width - listWidth - (PADDING * 3);
        
        // Header
        renderHeader(graphics);
        
        // Left Panel: Spawn List
        renderSectionFrame(graphics, PADDING, contentY, listWidth, contentHeight, COLOR_ORANGE_ACCENT);
        renderSpawnList(graphics, PADDING, contentY, listWidth, contentHeight, mouseX, mouseY);
        
        // Right Panel: Details
        renderSectionFrame(graphics, PADDING + listWidth + PADDING, contentY, detailsWidth, contentHeight, COLOR_ORANGE_ACCENT);
        renderSpawnDetails(graphics, PADDING + listWidth + PADDING, contentY, detailsWidth, contentHeight);
        
        // Footer (Cooldown info)
        if (respawnCooldown > 0) {
            String cooldownText = "RESPAWN COOLDOWN: " + respawnCooldown + "s";
            graphics.drawString(this.font, cooldownText, PADDING + 20, this.height - 30, COLOR_RED, false);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderBackdrop(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
        // Header separator
        graphics.fill(0, HEADER_HEIGHT, this.width, HEADER_HEIGHT + 1, COLOR_PANEL_BORDER);
        // Footer separator
        int footerTop = this.height - FOOTER_HEIGHT;
        graphics.fill(0, footerTop, this.width, footerTop + 1, COLOR_PANEL_BORDER);
    }
    
    private void renderHeader(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(PADDING + 10, 12, 0);
        graphics.pose().scale(1.5f, 1.5f, 1.5f);
        graphics.drawString(this.font, "DEPLOYMENT", 0, 0, COLOR_WHITE_TEXT, false);
        graphics.pose().popPose();
        
        graphics.drawString(this.font, "SELECT SPAWN POINT", PADDING + 10, 30, COLOR_GREY_TEXT, false);
    }

    private void renderSpawnList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        int listContentHeight = spawnPoints.size() * (LIST_ITEM_HEIGHT + LIST_ITEM_SPACING);
        maxScroll = Math.max(0, listContentHeight - height + PADDING);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        
        int listY = y + PADDING;
        int listH = height - (PADDING * 2);
        
        graphics.enableScissor(x, listY, x + width, listY + listH);
        
        int currentY = listY - scrollOffset;
        
        for (OpenSpawnMenuPacket.SpawnPoint sp : spawnPoints) {
            if (currentY + LIST_ITEM_HEIGHT > listY && currentY < listY + listH) {
                boolean isSelected = (selectedSpawnPoint == sp);
                boolean isHovered = mouseX >= x + PADDING && mouseX < x + width - PADDING && 
                                    mouseY >= currentY && mouseY < currentY + LIST_ITEM_HEIGHT;
                
                renderSpawnCard(graphics, sp, x + PADDING, currentY, width - (PADDING * 2), LIST_ITEM_HEIGHT, isSelected, isHovered);
            }
            currentY += LIST_ITEM_HEIGHT + LIST_ITEM_SPACING;
        }
        
        graphics.disableScissor();
        
        // Scrollbar
        if (maxScroll > 0) {
            int barX = x + width - 6;
            int barY = listY;
            int barH = listH;
            int thumbH = Math.max(20, (int)((float)barH * (barH / (float)listContentHeight)));
            int thumbY = barY + (int)((float)(barH - thumbH) * (scrollOffset / (float)maxScroll));
            
            graphics.fill(barX, barY, barX + 4, barY + barH, 0xFF222222);
            graphics.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF666666);
        }
    }
    
    private void renderSpawnCard(GuiGraphics graphics, OpenSpawnMenuPacket.SpawnPoint sp, int x, int y, int width, int height, boolean isSelected, boolean isHovered) {
        int bgColor = isSelected ? 0xFF2C3E50 : (isHovered ? COLOR_CARD_HOVER : COLOR_CARD_BG);
        int borderColor = isSelected ? COLOR_BLUE_ACCENT : COLOR_PANEL_BORDER;
        
        // Background
        graphics.fill(x, y, x + width, y + height, bgColor);
        
        // Border
        graphics.renderOutline(x, y, width, height, borderColor);
        
        // Icon Placeholder (Left)
        int iconSize = 24;
        graphics.fill(x + 8, y + (height - iconSize) / 2, x + 8 + iconSize, y + (height + iconSize) / 2, 0xFF333333);
        graphics.drawCenteredString(this.font, getIconChar(sp.type()), x + 8 + iconSize / 2, y + (height - 8) / 2, COLOR_WHITE_TEXT);
        
        // Text
        int textX = x + 40;
        graphics.drawString(this.font, sp.displayName(), textX, y + 8, isSelected ? COLOR_WHITE_TEXT : 0xFFDDDDDD, false);
        
        String typeText = getTypeDisplayName(sp.type());
        graphics.drawString(this.font, typeText, textX, y + 24, COLOR_GREY_TEXT, false);
        
        // Status Indicator (Right)
        int statusColor = sp.available() ? COLOR_GREEN : COLOR_RED;
        String statusText = sp.available() ? "READY" : "LOCKED";
        int statusWidth = this.font.width(statusText);
        
        graphics.drawString(this.font, statusText, x + width - statusWidth - 8, y + (height - 8) / 2, statusColor, false);
    }
    
    private void renderSpawnDetails(GuiGraphics graphics, int x, int y, int width, int height) {
        if (selectedSpawnPoint == null) return;
        
        int padding = 20;
        int currentY = y + padding;
        
        // Big Title
        graphics.pose().pushPose();
        graphics.pose().translate(x + padding, currentY, 0);
        graphics.pose().scale(2.0f, 2.0f, 2.0f);
        graphics.drawString(this.font, selectedSpawnPoint.displayName(), 0, 0, COLOR_WHITE_TEXT, false);
        graphics.pose().popPose();
        currentY += 30;
        
        // Type Badge
        String typeText = getTypeDisplayName(selectedSpawnPoint.type()).toUpperCase();
        int badgeWidth = this.font.width(typeText) + 12;
        graphics.fill(x + padding, currentY, x + padding + badgeWidth, currentY + 16, 0xFF333333);
        graphics.renderOutline(x + padding, currentY, badgeWidth, 16, COLOR_ORANGE_ACCENT);
        graphics.drawString(this.font, typeText, x + padding + 6, currentY + 4, COLOR_ORANGE_ACCENT, false);
        currentY += 30;
        
        // Status
        graphics.drawString(this.font, "STATUS:", x + padding, currentY, COLOR_GREY_TEXT, false);
        String status = selectedSpawnPoint.available() ? "AVAILABLE FOR DEPLOYMENT" : "UNAVAILABLE";
        int statusColor = selectedSpawnPoint.available() ? COLOR_GREEN : COLOR_RED;
        graphics.drawString(this.font, status, x + padding + 50, currentY, statusColor, false);
        currentY += 20;
        
        // Description / Lore
        String desc = getDescriptionForType(selectedSpawnPoint.type());
        graphics.drawString(this.font, desc, x + padding, currentY, COLOR_GREY_TEXT, false);
        
        // Map Preview Placeholder
        int mapBoxY = currentY + 40;
        int mapBoxHeight = height - (mapBoxY - y) - padding;
        if (mapBoxHeight > 50) {
            graphics.fill(x + padding, mapBoxY, x + width - padding, mapBoxY + mapBoxHeight, 0xFF080808);
            graphics.renderOutline(x + padding, mapBoxY, width - (padding * 2), mapBoxHeight, 0xFF333333);
            graphics.drawCenteredString(this.font, "MAP PREVIEW UNAVAILABLE", x + width / 2, mapBoxY + mapBoxHeight / 2, 0xFF444444);
        }
    }
    
    // Helpers
    
    private String getIconChar(String type) {
        return switch (type) {
            case "team_base" -> "⚑";
            case "lobby" -> "⌂";
            case "rally_point" -> "▲";
            case "fob" -> "■";
            case "squad_leader" -> "★";
            default -> "•";
        };
    }
    
    private String getTypeDisplayName(String type) {
        return switch (type) {
            case "team_base" -> "Main Base";
            case "lobby" -> "Lobby / Safe Zone";
            case "rally_point" -> "Rally Point";
            case "fob" -> "Forward Operating Base";
            case "squad_leader" -> "Squad Leader";
            default -> "Unknown Point";
        };
    }
    
    private String getDescriptionForType(String type) {
        return switch (type) {
            case "team_base" -> "Permanent team deployment zone. Safe area.";
            case "lobby" -> "Return to the lobby area. You will leave the combat zone.";
            case "rally_point" -> "Temporary squad deployment point. Vulnerable to enemies.";
            case "fob" -> "Fortified deployment structure. Provides supplies.";
            case "squad_leader" -> "Deploy near your squad leader.";
            default -> "Standard deployment point.";
        };
    }

    private void renderSectionFrame(GuiGraphics graphics, int x, int y, int width, int height, int accentColor) {
        int bgBase = COLOR_PANEL;
        int borderBase = COLOR_PANEL_BORDER;
        
        graphics.fill(x, y, x + width, y + height, withAlpha(bgBase, (int)(fadeIn * 240)));
        graphics.renderOutline(x, y, width, height, withAlpha(borderBase, (int)(fadeIn * 255)));
        
        // Accent top bar
        graphics.fill(x, y, x + width, y + 2, withAlpha(accentColor, (int)(fadeIn * 200)));
    }
    
    private int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle list selection
        int listWidth = (int)(this.width * 0.35f);
        int contentY = HEADER_HEIGHT + PADDING;
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2);
        
        if (mouseX >= PADDING && mouseX <= PADDING + listWidth && 
            mouseY >= contentY + PADDING && mouseY <= contentY + contentHeight - PADDING) {
            
            int relativeY = (int)mouseY - (contentY + PADDING) + scrollOffset;
            int index = relativeY / (LIST_ITEM_HEIGHT + LIST_ITEM_SPACING);
            
            if (index >= 0 && index < spawnPoints.size()) {
                // Check if click is within item height (not in spacing)
                if (relativeY % (LIST_ITEM_HEIGHT + LIST_ITEM_SPACING) <= LIST_ITEM_HEIGHT) {
                    this.selectedSpawnPoint = spawnPoints.get(index);
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listWidth = (int)(this.width * 0.35f);
        if (mouseX <= PADDING + listWidth + PADDING) {
            scrollOffset = (int)Mth.clamp(scrollOffset - scrollY * 20, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
    
    private static class TacticalButton extends Button {
        public TacticalButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            this.isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
            
            int bgColor = isHovered ? 0xFFE67E22 : 0xFF222222;
            if (!this.active) bgColor = 0xFF111111;
            
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
            graphics.renderOutline(getX(), getY(), width, height, 0xFF444444);
            
            int textColor = this.active ? (isHovered ? 0xFFFFFFFF : 0xFFCCCCCC) : 0xFF555555;
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
        }
    }
}
