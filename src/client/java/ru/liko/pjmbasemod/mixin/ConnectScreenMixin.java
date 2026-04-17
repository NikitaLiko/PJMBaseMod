package ru.liko.pjmbasemod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.init.PjmSounds;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    @Shadow public volatile Component status;
    @Shadow @Final Screen parent;

    @Unique
    private long pjm_initTime;
    
    @Unique
    private Button pjm_cancelButton;

    @Unique
    private static final int COLOR_BG = 0xFF000000;
    @Unique
    private static final int COLOR_ACCENT = 0xFFFFAA00; // Arma Gold
    @Unique
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    @Unique
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA;
    
    @Unique
    private static final ResourceLocation LOGO_TEXTURE = ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/icon/pjm_512x512.png");
    @Unique
    private static final ResourceLocation PHOTO_TEXTURE = ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_1.png");

    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Clear vanilla widgets (cancel button)
        this.clearWidgets();

        // Only set start time if not already set (prevents fade-in replay on window resize)
        if (this.pjm_initTime == 0) {
            this.pjm_initTime = System.currentTimeMillis();
            // Play loading sound when connection screen initializes
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(PjmSounds.MENU_LOADING.get(), 1.0F));
        }
        
        // Add Custom Cancel Button
        // Top Right position
        int btnW = 100;
        int btnH = 20;
        int btnX = this.width - btnW - 20; 
        int btnY = 20;

        // Local class to access protected constructor and DEFAULT_NARRATION
        class TacticalButton extends Button {
            private final long initTime;

            public TacticalButton(int x, int y, int w, int h, Component message, OnPress onPress, long initTime) {
                super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
                this.initTime = initTime;
            }

            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                // Custom Tactical Button Render
                int color = this.isHoveredOrFocused() ? COLOR_ACCENT : COLOR_TEXT;
                
                long elapsed = System.currentTimeMillis() - this.initTime;
                float alphaVal = Mth.clamp(elapsed / 1000.0f, 0.0f, 1.0f);
                int alphaInt = (int)(alphaVal * 255) << 24;
                
                if (alphaVal < 0.1f) return; // Don't render if too transparent

                int drawColor = (color & 0x00FFFFFF) | alphaInt;
                int drawBg = (0x80000000) & 0x00FFFFFF | (int)(alphaVal * 128) << 24; // Semi-transparent black

                // Background
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, drawBg);
                
                // Border
                guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, drawColor);
                
                // Text
                int textW = Minecraft.getInstance().font.width(this.getMessage());
                guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX() + (this.width - textW) / 2, this.getY() + (this.height - 8) / 2, drawColor);
            }
        }

        this.pjm_cancelButton = new TacticalButton(btnX, btnY, btnW, btnH, Component.translatable("gui.cancel"), (btn) -> {
            if (this.parent != null) {
                this.minecraft.setScreen(this.parent);
            } else {
                this.minecraft.setScreen(null); // Fallback
            }
        }, this.pjm_initTime);
        
        this.addRenderableWidget(this.pjm_cancelButton);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // Calculate fade-in
        long elapsed = System.currentTimeMillis() - pjm_initTime;
        float fadeInDuration = 1000.0f; // 1 second fade in
        float alpha = Mth.clamp(elapsed / fadeInDuration, 0.0f, 1.0f);

        int w = this.width;
        int h = this.height;

        // 1. Background (Black)
        guiGraphics.fill(0, 0, w, h, COLOR_BG);

        // Apply fade-in alpha
        int alphaInt = (int)(alpha * 255);
        int textAlpha = alphaInt << 24;
        
        // Helper to apply alpha to colors
        int colText = (COLOR_TEXT & 0x00FFFFFF) | textAlpha;
        int colAccent = (COLOR_ACCENT & 0x00FFFFFF) | textAlpha;
        int colDim = (COLOR_TEXT_DIM & 0x00FFFFFF) | textAlpha;
        
        if (alpha > 0.05f) {
            // --- HEADER ---
            // Logo (Top Left)
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            // Fix: Scale 512x512 texture to 48x48 quad
            guiGraphics.blit(LOGO_TEXTURE, 40, 30, 48, 48, 0, 0, 512, 512, 512, 512);
            
            // "PROJECT MINECRAFT" Text next to logo
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
            guiGraphics.drawString(this.font, "PROJECT", 65, 26, colText);
            guiGraphics.drawString(this.font, "MINECRAFT", 65, 36, colAccent);
            guiGraphics.pose().popPose();

            // --- CONTENT (Photo + Text) ---
            int contentCenterY = h / 2;
            
            // Photo (Polaroid style) - Left Center
            // Rotate slightly
            guiGraphics.pose().pushPose();
            float rotation = -2.0f;
            int photoW = 300; // 400 * 0.75
            int photoH = 168; // 225 * 0.75
            int photoX = w / 2 - photoW - 20; // Left of center
            int photoY = contentCenterY - photoH / 2;
            
            // Translate to center of photo for rotation
            guiGraphics.pose().translate(photoX + photoW/2.0, photoY + photoH/2.0, 0);
            guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
            guiGraphics.pose().translate(-(photoX + photoW/2.0), -(photoY + photoH/2.0), 0);
            
            // White Border
            guiGraphics.fill(photoX - 10, photoY - 10, photoX + photoW + 10, photoY + photoH + 30, 0x00FFFFFF | textAlpha);
            // Image
            // Fix: Map full texture (0..256 UV space base which maps to 0..1 UV) to the quad size
            guiGraphics.blit(PHOTO_TEXTURE, photoX, photoY, photoW, photoH, 0, 0, 256, 256, 256, 256); 
            
            guiGraphics.pose().popPose();

            // Text Info - Right Center
            int textBlockX = w / 2 + 40;
            int textBlockY = contentCenterY - 60;
            
            // Title
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(2.0f, 2.0f, 2.0f);
            guiGraphics.drawString(this.font, Component.translatable("menu.pjm.connect.title"), (textBlockX) / 2, (textBlockY) / 2, colText);
            guiGraphics.pose().popPose();
            
            textBlockY += 25;
            guiGraphics.drawString(this.font, Component.translatable("menu.pjm.connect.author"), textBlockX, textBlockY, colAccent);
            
            textBlockY += 30;
            Component desc1 = Component.translatable("menu.pjm.connect.desc.1");
            Component desc2 = Component.translatable("menu.pjm.connect.desc.2");
            Component desc3 = Component.translatable("menu.pjm.connect.desc.3");
            
            guiGraphics.drawString(this.font, desc1, textBlockX, textBlockY, colDim);
            guiGraphics.drawString(this.font, desc2, textBlockX, textBlockY + 12, colDim);
            guiGraphics.drawString(this.font, desc3, textBlockX, textBlockY + 24, colDim);

            // --- BOTTOM ---
            // Loading Bar
            int barWidth = w - 100; // Wide bar
            int barHeight = 2;
            int barX = 50;
            int barY = h - 80;
            
            // Bar BG (Grey line)
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x44FFFFFF | (textAlpha & 0xFF000000));
            
            // Progress - Ping Pong Scanner Style
            float time = (System.currentTimeMillis() % 2000) / 2000.0f; // 2 seconds period
            // sine wave 0..1..0
            float wave = (float)(0.5 - 0.5 * Math.cos(time * Math.PI * 2)); 
            
            int scannerWidth = barWidth / 4;
            int scannerX = barX + (int)((barWidth - scannerWidth) * wave);
            
            guiGraphics.fill(scannerX, barY, scannerX + scannerWidth, barY + barHeight, colAccent);
            
            // Status Text under bar (Centered)
            if (this.status != null) {
                guiGraphics.drawString(this.font, this.status, w / 2 - this.font.width(this.status) / 2, barY + 10, colText);
            }
            
            // Hint at bottom left
            // Icon [!] + Text
            guiGraphics.drawString(this.font, Component.translatable("menu.pjm.connect.hint"), 50, h - 30, colText);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
        
        // Render buttons manually since we cancelled super
        for (net.minecraft.client.gui.components.events.GuiEventListener child : this.children()) {
            if (child instanceof net.minecraft.client.gui.components.Renderable renderable) {
                // Only render if visible (alpha check for fade in effect on buttons?)
                // For now, buttons are always visible, but we might want to fade them too.
                // Standard buttons don't support easy alpha fade without custom class.
                // Just render them.
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        
        // Cancel vanilla render
        ci.cancel();
    }
}
