package ru.liko.pjmbasemod.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.liko.pjmbasemod.common.player.PjmRank;

/**
 * NeoForge 1.21.1: Use LayeredDraw.Layer instead of IGuiOverlay
 */
public class RankUpdateOverlay {
    
    private static long startTime = 0;
    private static PjmRank currentRank = PjmRank.PRIVATE;
    private static boolean isPromotion = true;
    private static final long DURATION = 5000; // 5 seconds
    
    // Arma Reforger style constants
    private static final int COLOR_BG = 0xAA000000;
    private static final int COLOR_ACCENT_PROMOTION = 0xFFD4AF37; // Gold
    private static final int COLOR_ACCENT_DEMOTION = 0xFFCC3333; // Red
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    
    public static void show(PjmRank rank, boolean promotion) {
        currentRank = rank;
        isPromotion = promotion;
        startTime = System.currentTimeMillis();
    }
    
    // NeoForge 1.21.1: LayeredDraw.Layer uses (graphics, deltaTracker) signature
    public static final LayeredDraw.Layer OVERLAY = (graphics, deltaTracker) -> {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        long time = System.currentTimeMillis();
        long elapsed = time - startTime;
        
        if (elapsed > DURATION || startTime == 0) {
            return;
        }
        
        float progress = (float) elapsed / DURATION;
        float alpha = 1.0f;
        
        // Fade in (0.5s)
        if (elapsed < 500) {
            alpha = elapsed / 500.0f;
        } 
        // Fade out (1.0s at the end)
        else if (elapsed > DURATION - 1000) {
            alpha = (DURATION - elapsed) / 1000.0f;
        }
        
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);
        
        int colorBg = ((int)(0xAA * alpha) << 24) | 0x000000;
        int colorAccent = isPromotion ? COLOR_ACCENT_PROMOTION : COLOR_ACCENT_DEMOTION;
        colorAccent = ((int)((colorAccent >>> 24) * alpha) << 24) | (colorAccent & 0x00FFFFFF);
        int colorText = ((int)(0xFF * alpha) << 24) | 0xFFFFFF;
        
        int width = 200;
        int height = 60;
        int x = (screenWidth - width) / 2;
        int y = screenHeight / 4; // Top quarter
        
        // Background
        graphics.fill(x, y, x + width, y + height, colorBg);
        
        // Side accent bar
        graphics.fill(x, y, x + 4, y + height, colorAccent);
        
        // Rank Icon
        ResourceLocation icon = currentRank.getIconLocation();
        if (icon != null) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            graphics.blit(icon, x + 15, y + 10, 0, 0, 40, 40, 40, 40);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        // Text
        String title = isPromotion ? "ПОВЫШЕНИЕ" : "ПОНИЖЕНИЕ";
        String rankName = currentRank.getDisplayName().getString().toUpperCase();
        
        graphics.drawString(Minecraft.getInstance().font, title, x + 65, y + 15, colorAccent, false);
        graphics.drawString(Minecraft.getInstance().font, rankName, x + 65, y + 30, colorText, false);
    };
}
