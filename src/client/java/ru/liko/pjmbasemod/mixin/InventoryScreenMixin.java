package ru.liko.pjmbasemod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.pjmbasemod.client.ClientTeamConfig;
import ru.liko.pjmbasemod.client.capture.CaptureUiHelper;
import ru.liko.pjmbasemod.client.capture.ClientCaptureData;
import ru.liko.pjmbasemod.client.death.ClientDeathStats;
import ru.liko.pjmbasemod.common.gamemode.ControlPointSnapshot;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmRank;

import java.util.List;
import java.util.Locale;

/**
 * Mixin for modifying the standard inventory screen.
 * Adds a background blur effect and displays player info and control points
 * in a style inspired by Arma Reforger / Squad (clean, tactical, ergonomic).
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    // Aesthetic Constants
    private static final int COLOR_PANEL_BG = 0xAA0F0F0F; // Deep dark gray, semi-transparent
    private static final int COLOR_HEADER_BG = 0xCC050505; // Slightly darker header
    private static final int COLOR_TEXT_PRIMARY = 0xFFEEEEEE; // Off-white
    private static final int COLOR_TEXT_SECONDARY = 0xFFAAAAAA; // Light gray
    private static final int COLOR_TEXT_ACCENT = 0xFFE0E0E0; // Brighter gray for values
    
    // Team Colors (Fallbacks if not resolved from config/scoreboard)
    private static final int COLOR_NEUTRAL = 0xFF95A5A6; // Gray-ish

    @Inject(method = "render", at = @At("TAIL"))
    private void renderPlayerInfo(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null) {
            return;
        }

        if (player.isCreative()) {
            return;
        }

        // NeoForge 1.21.1: Use Data Attachments instead of Capabilities
        ru.liko.pjmbasemod.common.player.PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
        if (data != null) {
            // --- PLAYER INFO CARD (Top Left) ---
            int margin = 20;
            int cardX = margin;
            int cardY = margin;
            int cardWidth = 220;
            
            // Resolve Team Color - используем ClientDeathStats для актуальной команды
            String teamId = data.getTeam();
            // Если data.getTeam() пустой, пробуем получить из ClientDeathStats
            if (teamId == null || teamId.isEmpty()) {
                teamId = ClientDeathStats.getCurrentTeam();
            }
            
            int teamColor = COLOR_NEUTRAL;
            String teamNameDisplay = "НЕТ КОМАНДЫ";
            
            if (teamId != null && !teamId.isEmpty()) {
                teamColor = CaptureUiHelper.resolveTeamColor(teamId);
                // Get display name via helper or config
                if (ClientTeamConfig.getTeam1Name().equals(teamId)) {
                    teamNameDisplay = ClientTeamConfig.getTeam1Name();
                } else if (ClientTeamConfig.getTeam2Name().equals(teamId)) {
                    teamNameDisplay = ClientTeamConfig.getTeam2Name();
                } else {
                    teamNameDisplay = teamId;
                }
            }
            teamNameDisplay = teamNameDisplay.toUpperCase(Locale.ROOT);

            // Calculate height content
            int lineHeight = minecraft.font.lineHeight + 4;
            int headerHeight = 18;
            int contentPadding = 10;
            int contentHeight = contentPadding * 2 + lineHeight * 5; // Name, Rank, Class, Deaths, Team Deaths
            int totalHeight = headerHeight + contentHeight;

            // 1. Main Background
            graphics.fill(cardX, cardY, cardX + cardWidth, cardY + totalHeight, COLOR_PANEL_BG);
            
            // 2. Header Strip (Team Color)
            graphics.fill(cardX, cardY, cardX + 4, cardY + totalHeight, teamColor);
            
            // 3. Team Header Background (Top part)
            graphics.fill(cardX + 4, cardY, cardX + cardWidth, cardY + headerHeight, COLOR_HEADER_BG);
            
            // 4. Team Name Text
            graphics.drawString(minecraft.font, teamNameDisplay, cardX + 10, cardY + 5, teamColor, false);
            
            // 5. Player Details
            int textX = cardX + 12;
            int currentY = cardY + headerHeight + contentPadding;

            // Name & Rank
            graphics.pose().pushPose();
            graphics.pose().translate(textX, currentY, 0);
            graphics.pose().scale(1.1f, 1.1f, 1.1f);
            graphics.drawString(minecraft.font, player.getName().getString(), 0, 0, COLOR_TEXT_PRIMARY, false);
            graphics.pose().popPose();
            currentY += lineHeight + 2;

            // Rank (с иконкой)
            PjmRank rank = data.getRank();
            String rankText = rank.getDisplayName().getString();
            
            // Отрисовка иконки звания
            ResourceLocation rankIcon = rank.getIconLocation();
            if (rankIcon != null) {
                // Рендерим иконку 12x12 перед текстом
                graphics.blit(rankIcon, textX, currentY - 1, 0, 0, 12, 12, 12, 12);
                // Сдвигаем текст вправо
                drawLabelValue(graphics, minecraft, "RANK", rankText, textX + 16, currentY, COLOR_TEXT_SECONDARY, COLOR_TEXT_ACCENT);
            } else {
                drawLabelValue(graphics, minecraft, "RANK", rankText, textX, currentY, COLOR_TEXT_SECONDARY, COLOR_TEXT_ACCENT);
            }
            currentY += lineHeight + 4; // Increased spacing for bar
            
            // Rank Progress Bar
            int barX = textX;
            int barW = 140;
            int barH = 3;
            int barY = currentY; // Position relative to currentY
            
            PjmRank nextRank = rank.getNext();
            int currentPoints = data.getRankPoints();
            int minPoints = rank.getMinPoints();
            int targetPoints = nextRank != rank ? nextRank.getMinPoints() : minPoints;
            
            // Background
            graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF444444);
            
            if (targetPoints > minPoints) {
                float progress = (float)(currentPoints - minPoints) / (float)(targetPoints - minPoints);
                progress = Math.max(0, Math.min(progress, 1.0f));
                int fillW = (int)(barW * progress);
                graphics.fill(barX, barY, barX + fillW, barY + barH, 0xFFD4AF37); // Gold color
                
                String xpText = currentPoints + " / " + targetPoints + " XP";
                graphics.pose().pushPose();
                graphics.pose().translate(barX + barW + 5, barY - 2, 0);
                graphics.pose().scale(0.8f, 0.8f, 0.8f);
                graphics.drawString(minecraft.font, xpText, 0, 0, COLOR_TEXT_SECONDARY, false);
                graphics.pose().popPose();
            } else if (nextRank == rank && rank != PjmRank.NOT_ENLISTED) {
                 // Max rank
                graphics.fill(barX, barY, barX + barW, barY + barH, 0xFFD4AF37);
                graphics.pose().pushPose();
                graphics.pose().translate(barX + barW + 5, barY - 2, 0);
                graphics.pose().scale(0.8f, 0.8f, 0.8f);
                graphics.drawString(minecraft.font, "MAX RANK (" + currentPoints + " XP)", 0, 0, 0xFFD4AF37, false);
                graphics.pose().popPose();
            }
            currentY += 8; // Extra spacing for bar

            // Class
            PjmPlayerClass playerClass = data.getPlayerClass();
            String classText = playerClass.getDisplayName().getString();
            drawLabelValue(graphics, minecraft, "CLASS", classText, textX, currentY, COLOR_TEXT_SECONDARY, COLOR_TEXT_ACCENT);
            currentY += lineHeight;

            // Deaths Stats
            int myDeaths = ClientDeathStats.getPlayerDeaths();
            int myTeamDeaths = ClientDeathStats.getTeamDeaths();
            
            // My Deaths
            drawLabelValue(graphics, minecraft, "DEATHS", String.valueOf(myDeaths), textX, currentY, COLOR_TEXT_SECONDARY, 0xFFFF6B6B);
            currentY += lineHeight;
            
            // Team Deaths
            drawLabelValue(graphics, minecraft, "TEAM DEATHS", String.valueOf(myTeamDeaths), textX, currentY, COLOR_TEXT_SECONDARY, 0xFFFF6B6B);


            // --- SECTOR STATUS (Top Right) ---
            List<ControlPointSnapshot> allPoints = ClientCaptureData.getPoints();
            if (!allPoints.isEmpty()) {
                InventoryScreen screen = (InventoryScreen) (Object) this;
                int pointsWidth = 200;
                int pointsX = screen.width - pointsWidth - margin;
                int pointsY = margin;
                
                // Calculate max items based on screen height
                int listY = pointsY + headerHeight;
                int itemHeight = 18;
                int listPadding = 4;
                int bottomMargin = 20;
                
                int availableHeight = screen.height - listY - bottomMargin;
                int maxItemsByHeight = availableHeight / itemHeight;
                int hardLimit = 12; // Maximum items regardless of height
                
                int maxItems = Math.min(maxItemsByHeight, hardLimit);
                if (maxItems < 1) maxItems = 1;

                boolean overflow = allPoints.size() > maxItems;
                int displayCount = overflow ? maxItems - 1 : allPoints.size(); // Reserve 1 slot for "..." if overflow
                
                List<ControlPointSnapshot> points = allPoints.subList(0, Math.min(allPoints.size(), displayCount));
                
                // Header
                graphics.fill(pointsX, pointsY, pointsX + pointsWidth, pointsY + headerHeight, COLOR_HEADER_BG);
                // Accent top line
                graphics.fill(pointsX, pointsY, pointsX + pointsWidth, pointsY + 2, COLOR_NEUTRAL);
                
                graphics.drawString(minecraft.font, "SECTOR STATUS", pointsX + 6, pointsY + 5, COLOR_TEXT_SECONDARY, false);
                
                // Background for list
                int listHeight = (points.size() + (overflow ? 1 : 0)) * itemHeight + listPadding * 2;
                graphics.fill(pointsX, listY, pointsX + pointsWidth, listY + listHeight, COLOR_PANEL_BG);
                
                int currentPointY = listY + listPadding;
                
                for (ControlPointSnapshot point : points) {
                    String pointName = point.displayName().toUpperCase(Locale.ROOT);
                    if (pointName.length() > 14) pointName = pointName.substring(0, 14) + "..";
                    
                    String ownerTeam = point.ownerTeam();
                    String capturingTeam = point.capturingTeam();
                    boolean isCapturing = capturingTeam != null && !capturingTeam.isEmpty();
                    
                    int pointColor = CaptureUiHelper.resolveTeamColor(ownerTeam);
                    if (isCapturing) {
                        // Blink or show capturing color
                         long time = System.currentTimeMillis();
                         if (time % 1000 < 500) {
                             pointColor = CaptureUiHelper.resolveTeamColor(capturingTeam);
                         }
                    }

                    // Point Indicator (Color Box)
                    graphics.fill(pointsX + 6, currentPointY + 4, pointsX + 14, currentPointY + 12, pointColor);
                    
                    // Point Name
                    graphics.drawString(minecraft.font, pointName, pointsX + 20, currentPointY + 4, COLOR_TEXT_PRIMARY, false);
                    
                    // Capture Status / Percentage
                    if (isCapturing) {
                        int progress = (int)(point.captureProgress() * 100);
                        String progressText = progress + "%";
                        int pWidth = minecraft.font.width(progressText);
                        graphics.drawString(minecraft.font, progressText, pointsX + pointsWidth - pWidth - 6, currentPointY + 4, COLOR_TEXT_ACCENT, false);
                        
                        // Progress Bar Line
                        int barWidth = 60;
                        int barXCapture = pointsX + pointsWidth - barWidth - 35; // Position before text
                        int barYCapture = currentPointY + 14; // Renamed to avoid conflict
                        // bg
                        graphics.fill(barXCapture, barYCapture, barXCapture + barWidth, barYCapture + 2, 0xFF444444);
                        // fill
                        int fillW = (int)(barWidth * point.captureProgress());
                        int capColor = CaptureUiHelper.resolveTeamColor(capturingTeam);
                        graphics.fill(barXCapture, barYCapture, barXCapture + fillW, barYCapture + 2, capColor);
                    } else if (ownerTeam != null && !ownerTeam.isEmpty()) {
                        String ownerText = "OWNED";
                        int oWidth = minecraft.font.width(ownerText);
                        graphics.drawString(minecraft.font, ownerText, pointsX + pointsWidth - oWidth - 6, currentPointY + 4, pointColor, false);
                    } else {
                        String neuText = "NEUTRAL";
                        int nWidth = minecraft.font.width(neuText);
                        graphics.drawString(minecraft.font, neuText, pointsX + pointsWidth - nWidth - 6, currentPointY + 4, COLOR_TEXT_SECONDARY, false);
                    }
                    
                    currentPointY += itemHeight;
                }

                if (overflow) {
                    String moreText = "...";
                    int mWidth = minecraft.font.width(moreText);
                    graphics.drawString(minecraft.font, moreText, pointsX + (pointsWidth - mWidth) / 2, currentPointY + 4, COLOR_TEXT_SECONDARY, false);
                }
            }
        }
    }

    private void drawLabelValue(GuiGraphics graphics, Minecraft mc, String label, String value, int x, int y, int labelColor, int valueColor) {
        String labelText = label + ": ";
        int labelWidth = mc.font.width(labelText);
        graphics.drawString(mc.font, labelText, x, y, labelColor, false);
        graphics.drawString(mc.font, value, x + labelWidth, y, valueColor, false);
    }
}
