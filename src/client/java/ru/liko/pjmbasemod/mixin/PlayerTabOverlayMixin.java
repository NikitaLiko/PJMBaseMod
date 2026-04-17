package ru.liko.pjmbasemod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import ru.liko.pjmbasemod.client.gui.overlay.CustomTabOverlay;

/**
 * Миксин для замены стандартного TAB-списка на кастомный
 */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    
    @Shadow @Final private Minecraft minecraft;
    
    /**
     * @author WRB-BaseMod
     * @reason Полная замена стандартного TAB-списка на кастомный с иконками званий
     */
    @Overwrite
    public void render(GuiGraphics graphics, int screenWidth, Scoreboard scoreboard, Objective objective) {
        // Рендерим кастомный TAB
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        CustomTabOverlay.render(graphics, screenWidth, screenHeight, scoreboard, objective);
    }
}
