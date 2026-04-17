package ru.liko.pjmbasemod.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;

/**
 * NeoForge 1.21.1: Use LayeredDraw.Layer instead of IGuiOverlay
 */
public class XpGainOverlay {
    
    private static long popupTime = 0;
    private static int lastXpAmount = 0;
    private static String lastReason = "";
    private static final long DURATION = 2000; // 2 seconds
    
    public static void show(int amount, String reason) {
        lastXpAmount = amount;
        lastReason = reason;
        popupTime = System.currentTimeMillis();
    }
    
    // NeoForge 1.21.1: LayeredDraw.Layer uses (graphics, deltaTracker) signature
    public static final LayeredDraw.Layer OVERLAY = (graphics, deltaTracker) -> {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        long time = System.currentTimeMillis();
        long elapsed = time - popupTime;
        
        if (elapsed > DURATION || popupTime == 0) {
            return;
        }
        
        float alpha = 1.0f;
        // Fade out in last 0.5s
        if (elapsed > DURATION - 500) {
            alpha = (DURATION - elapsed) / 500.0f;
        }
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);
        
        int color = lastXpAmount > 0 ? 0xFFD4AF37 : 0xFFFF5555; // Gold or Red
        int textAlpha = (int)(255 * alpha) << 24;
        int colorWithAlpha = (color & 0x00FFFFFF) | textAlpha;
        
        String text = (lastXpAmount > 0 ? "+" : "") + lastXpAmount + " XP";
        if (lastReason != null && !lastReason.isEmpty()) {
            text += " (" + lastReason + ")";
        }
        
        int textWidth = Minecraft.getInstance().font.width(text);
        
        // Position: Right side of the screen, vertically centered
        int x = screenWidth - textWidth - 20; 
        int y = screenHeight / 2;
        
        // Move up slightly animation
        y -= (int)(elapsed / 50.0f); 
        
        RenderSystem.enableBlend();
        graphics.drawString(Minecraft.getInstance().font, text, x, y, colorWithAlpha, true); // true for shadow
        RenderSystem.disableBlend();
    };
}
