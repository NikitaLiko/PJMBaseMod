package ru.liko.pjmbasemod.client.gui.overlay.gamemode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ru.liko.pjmbasemod.Config;

/**
 * NeoForge 1.21.1: Use LayeredDraw.Layer instead of IGuiOverlay
 */
@OnlyIn(Dist.CLIENT)
public final class GameModeHudOverlay {
    // NeoForge 1.21.1: LayeredDraw.Layer uses (graphics, deltaTracker) signature
    public static final LayeredDraw.Layer OVERLAY = (graphics, deltaTracker) -> {
        if (!Config.isCaptureSystemEnabled()) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        CaptureStatusBar.render(graphics, width);
    };

    private GameModeHudOverlay() {}
}

