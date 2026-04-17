package ru.liko.pjmbasemod.client.gui.overlay.gamemode;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.client.capture.CaptureUiHelper;
import ru.liko.pjmbasemod.common.gamemode.ControlPointSnapshot;

import java.util.Locale;

/**
 * Modern Squad/Battlefield style capture point overlay.
 * Located in top-right corner.
 */
final class CaptureStatusBar {
    
    private static final int MARGIN_X = 20;
    private static final int MARGIN_Y = 20;
    private static final int ICON_SIZE = 32; // Size of the diamond icon
    
    // Animation states
    private static ControlPointSnapshot lastPoint = null;
    private static long showTime = 0;
    private static long hideTime = 0;
    private static boolean isVisible = false;
    private static final long FADE_IN_DURATION = 300;
    private static final long FADE_OUT_DURATION = 250;
    
    // Progress smoothing
    private static double animatedProgress = 0.0;
    private static long lastFrameTime = 0;
    
    // Smoothing factor - higher = faster response, lower = smoother
    private static final double SMOOTHING_SPEED = 5.0; // units per second

    private CaptureStatusBar() {}

    static void render(GuiGraphics graphics, int screenWidth) {
        ControlPointSnapshot focus = CaptureUiHelper.findPointUnderPlayer();
        long currentTime = System.currentTimeMillis();
        
        updateVisibility(focus, currentTime);
        
        if (!isVisible || lastPoint == null) {
            return;
        }
        
        updateProgress(focus, currentTime);
        
        float alpha = calculateAlpha(focus, currentTime);
        if (alpha <= 0.01f) return;

        renderWidget(graphics, screenWidth, alpha, currentTime);
    }
    
    private static void updateVisibility(ControlPointSnapshot focus, long currentTime) {
        if (focus != null) {
            if (lastPoint == null || !lastPoint.id().equals(focus.id())) {
                showTime = currentTime;
                isVisible = true;
                hideTime = 0;
                animatedProgress = focus.captureProgress();
                lastFrameTime = currentTime;
            }
            lastPoint = focus;
        } else {
            if (isVisible && hideTime == 0) {
                hideTime = currentTime;
            }
        }
        
        if (hideTime > 0 && currentTime - hideTime >= FADE_OUT_DURATION) {
            isVisible = false;
            lastPoint = null;
            hideTime = 0;
        }
    }
    
    private static void updateProgress(ControlPointSnapshot focus, long currentTime) {
        if (focus == null) return;
        
        double targetProgress = focus.captureProgress();
        
        // First frame or after reset - jump to target
        if (lastFrameTime == 0) {
            lastFrameTime = currentTime;
            animatedProgress = targetProgress;
            return;
        }
        
        // Calculate delta time in seconds
        double deltaTime = (currentTime - lastFrameTime) / 1000.0;
        lastFrameTime = currentTime;
        
        // Clamp delta to avoid jumps after pauses
        deltaTime = Math.min(deltaTime, 0.1);
        
        // Exponential smoothing: smoothly approach target
        double smoothFactor = 1.0 - Math.exp(-SMOOTHING_SPEED * deltaTime);
        animatedProgress = animatedProgress + (targetProgress - animatedProgress) * smoothFactor;
        
        // Snap to target if very close
        if (Math.abs(targetProgress - animatedProgress) < 0.001) {
            animatedProgress = targetProgress;
        }
    }
    
    private static float calculateAlpha(ControlPointSnapshot focus, long currentTime) {
        if (focus != null) {
            long elapsed = currentTime - showTime;
            if (elapsed < FADE_IN_DURATION) {
                return easeOutCubic((float) elapsed / FADE_IN_DURATION);
            }
            return 1.0f;
        } else {
            long elapsed = currentTime - hideTime;
            if (elapsed < FADE_OUT_DURATION) {
                return 1.0f - easeInCubic((float) elapsed / FADE_OUT_DURATION);
            }
            return 0.0f;
        }
    }

    private static void renderWidget(GuiGraphics graphics, int screenWidth, float alpha, long currentTime) {
        Font font = Minecraft.getInstance().font;
        
        // Animation offset (slide in from right)
        int slideOffset = (int) ((1.0f - alpha) * 30);
        
        // 1. Render Diamond Icon
        int iconCenterX = screenWidth - MARGIN_X - (ICON_SIZE / 2) + slideOffset;
        int iconCenterY = MARGIN_Y + (ICON_SIZE / 2);
        
        renderDiamondIcon(graphics, iconCenterX, iconCenterY, alpha, currentTime);
        
        // 2. Render Text info
        // Name
        String title = lastPoint.displayName().toUpperCase(Locale.ROOT);
        int titleColor = ((int)(alpha * 255) << 24) | 0xFFFFFF;
        
        // Status Text
        Component statusText;
        int statusColor;
        
        String ownerTeam = lastPoint.ownerTeam();
        String capturingTeam = lastPoint.capturingTeam();
        boolean isCapturing = !capturingTeam.isBlank() && lastPoint.captureProgress() > 0;
        
        if (isCapturing) {
            String teamName = CaptureUiHelper.formatTeamLabel(capturingTeam);
            int percent = (int)(lastPoint.captureProgress() * 100);
            statusText = Component.translatable("overlay.wrb.capture.status", teamName, percent);
            statusColor = CaptureUiHelper.resolveTeamColor(capturingTeam);
        } else {
            if (ownerTeam == null || ownerTeam.isBlank()) {
                // Neutral
                statusText = Component.translatable("overlay.wrb.capture.team.none");
                statusColor = 0xCCCCCC;
            } else {
                // Owned
                String teamName = CaptureUiHelper.formatTeamLabel(ownerTeam);
                statusText = Component.translatable("overlay.wrb.capture.holder", teamName);
                statusColor = CaptureUiHelper.resolveTeamColor(ownerTeam);
            }
        }
        
        // Apply alpha to status color
        statusColor = ((int)(alpha * 255) << 24) | (statusColor & 0x00FFFFFF);
        
        // Right align text to the icon
        int textRightX = iconCenterX - (ICON_SIZE / 2) - 10;
        int titleWidth = font.width(title);
        int statusWidth = font.width(statusText);
        
        graphics.drawString(font, title, textRightX - titleWidth, iconCenterY - font.lineHeight, titleColor, true);
        graphics.drawString(font, statusText, textRightX - statusWidth, iconCenterY + 2, statusColor, true);
        
        // 3. Render Timer (if capturing)
        if (isCapturing) {
             String timeText = calculateTimeRemaining(animatedProgress);
             if (!timeText.isEmpty()) {
                 int timeWidth = font.width(timeText);
                 int timeColor = ((int)(alpha * 200) << 24) | 0xFFFFFF;
                 graphics.drawString(font, timeText, textRightX - timeWidth, iconCenterY + 14, timeColor, true);
             }
        }
    }
    
    private static void renderDiamondIcon(GuiGraphics graphics, int cx, int cy, float alpha, long currentTime) {
        PoseStack pose = graphics.pose();
        
        String ownerTeam = lastPoint.ownerTeam();
        String capturingTeam = lastPoint.capturingTeam();
        boolean isCapturing = !capturingTeam.isBlank() && lastPoint.captureProgress() > 0;
        
        int baseColor = CaptureUiHelper.resolveTeamColor(ownerTeam);
        int captureColor = CaptureUiHelper.resolveTeamColor(capturingTeam);
        
        // Apply Alpha
        int baseColorWithAlpha = ((int)(alpha * 255) << 24) | (baseColor & 0x00FFFFFF);
        int captureColorWithAlpha = ((int)(alpha * 255) << 24) | (captureColor & 0x00FFFFFF);
        
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(45)); // Rotate 45 deg to make diamond
        
        float halfSize = ICON_SIZE / 2.0f;
        
        // Draw Background (Base Owner)
        // Outline
        int border = 0xFF000000 | (int)(alpha * 180) << 24;
        // Inner fill
        fill(graphics, -halfSize, -halfSize, halfSize, halfSize, baseColorWithAlpha);
        
        // Border (Black outline for contrast)
        float borderSize = 1.5f;
        fill(graphics, -halfSize - borderSize, -halfSize - borderSize, halfSize + borderSize, -halfSize, border); // Top
        fill(graphics, -halfSize - borderSize, halfSize, halfSize + borderSize, halfSize + borderSize, border); // Bottom
        fill(graphics, -halfSize - borderSize, -halfSize, -halfSize, halfSize, border); // Left
        fill(graphics, halfSize, -halfSize, halfSize + borderSize, halfSize, border); // Right
        
        // Capture Progress Fill
        if (isCapturing) {
            float progress = (float) Mth.clamp(animatedProgress, 0.0, 1.0);
            
            float fillHeight = (ICON_SIZE) * progress;
            float localTop = halfSize - fillHeight;
            
            fill(graphics, -halfSize, localTop, halfSize, halfSize, captureColorWithAlpha);
            
            // Add a glowing line at the leading edge
            int glowColor = 0xFFFFFFFF | ((int)(alpha * 255) << 24);
            fill(graphics, -halfSize, localTop, halfSize, localTop + 1, glowColor);
        }

        pose.popPose();
    }
    
    private static void fill(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color) {
        graphics.fill((int)x1, (int)y1, (int)x2, (int)y2, color);
    }

    private static String calculateTimeRemaining(double progress) {
        if (progress >= 1.0 || progress <= 0.0) return "";
        
        // Check if point has specific capture time override
        int captureTimeSeconds = -1;
        if (lastPoint != null) {
             captureTimeSeconds = lastPoint.captureTimeSeconds();
        }
        
        // Fallback to global config if override is not set (-1)
        if (captureTimeSeconds <= 0) {
            // Default to AAS time as base (renamed concept but same config for now, or add WAR config)
            // Since we only have WAR, we can use a default constant or existing config.
            // Using AAS config as "Standard War Time" for now to avoid breaking config structure too much.
            captureTimeSeconds = Config.getCaptureTimeSeconds();
        }
        
        double deltaPerTick = 1.0 / (captureTimeSeconds * 20.0);
        double remainingProgress = 1.0 - progress;
        double ticksRemaining = remainingProgress / deltaPerTick;
        double secondsRemaining = ticksRemaining / 20.0;
        
        int totalSeconds = (int) Math.ceil(secondsRemaining);
        
        if (totalSeconds < 60) {
            return String.format(Locale.ROOT, "%dс", totalSeconds);
        } else {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
        }
    }
    
    private static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0 - t, 3);
    }
    
    private static float easeInCubic(float t) {
        return t * t * t;
    }
}

