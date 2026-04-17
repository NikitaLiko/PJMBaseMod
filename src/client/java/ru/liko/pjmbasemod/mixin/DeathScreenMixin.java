package ru.liko.pjmbasemod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.client.mixin.access.DeathScreenMixinAccess;

/**
 * Mixin для замены экрана смерти черным экраном
 * Создает иммерсивный эффект смерти как в Squad/ARMA
 */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen implements DeathScreenMixinAccess {

    @Shadow
    private int delayTicker;
    
    @Shadow
    private Component causeOfDeath;

    @Unique
    private boolean Pjmbasemod$blackScreenEnabled = false;
    
    @Unique
    private Button Pjmbasemod$hiddenRespawnButton = null;
    
    @Unique
    private long Pjmbasemod$timeLived = 0;

    @Unique
    private static final ResourceLocation DEAD_ICON = ResourceLocation.fromNamespaceAndPath("pjmbasemod", "textures/icon/dead_icon.png");

    protected DeathScreenMixin(Component title) {
        super(title);
    }
    
    // Геттеры для доступа к полям из @Inject методов
    @Unique
    private Minecraft Pjmbasemod$getMinecraft() {
        return this.minecraft;
    }

    @Override
    public boolean Pjmbasemod$isBlackScreenEnabled() {
        return Pjmbasemod$blackScreenEnabled;
    }

    @Override
    public int Pjmbasemod$getDelayTicker() {
        return this.delayTicker;
    }

    /**
     * Перехватываем инициализацию экрана смерти
     * Если включен черный экран - добавляем скрытую кнопку респауна
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Pjmbasemod$blackScreenEnabled = Config.isBlackDeathScreen();
        
        if (this.minecraft != null && this.minecraft.player != null) {
             Pjmbasemod$timeLived = this.minecraft.player.tickCount;
        }

        if (Pjmbasemod$blackScreenEnabled) {
            // Скрываем стандартные виджеты
            this.clearWidgets();
            
            // Добавляем скрытую кнопку респауна 
            Pjmbasemod$hiddenRespawnButton = Button.builder(
                Component.translatable("deathScreen.respawn"),
                button -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.respawn();
                        button.active = false;
                    }
                })
                .bounds(this.width / 2 - 100, this.height - 40, 200, 20)
                .build();
            
            Pjmbasemod$hiddenRespawnButton.visible = false;
            Pjmbasemod$hiddenRespawnButton.active = false;
            this.addRenderableWidget(Pjmbasemod$hiddenRespawnButton);
        }
    }

    /**
     * Перехватываем рендеринг экрана смерти
     * Рисуем полностью черный экран вместо стандартного
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Pjmbasemod$blackScreenEnabled) {
            return;
        }

        int width = this.width;
        int height = this.height;

        // 1. Фон: Полностью черный (Tactical Black)
        graphics.fill(0, 0, width, height, 0xFF000000);
        
        // 2. Иконка смерти над заголовком
        if (this.delayTicker > 5) {
            RenderSystem.enableBlend();
            // Большая иконка смерти в центре
            int iconSize = 64;
            int iconX = (width - iconSize) / 2;
            int iconY = (height / 2) - 120; // Выше центра (было -80)
            
            graphics.blit(DEAD_ICON, iconX, iconY, 0, 0, iconSize, iconSize, 64, 64);
            RenderSystem.disableBlend();
        }
        
        // 3. Основной заголовок: "YOU DIED" / "ВЫ ПОГИБЛИ"
        // Делаем его большим и красным
        if (this.delayTicker > 10) {
            graphics.pose().pushPose();
            float scale = 3.0F; // Очень крупно
            graphics.pose().scale(scale, scale, scale);
            
            Component title = Component.translatable("deathScreen.title");
            int textWidth = this.font.width(title);
            // Центрируем с учетом скейла: (ScreenCenter / Scale) - (TextCenter)
            float textX = (width / 2.0f) / scale - (textWidth / 2.0f);
            float textY = (height / 2.0f - 40) / scale; // Чуть выше центра
            
            // Темно-красный цвет (Blood Red)
            graphics.drawString(this.font, title, (int)textX, (int)textY, 0xFF880000, false);
            graphics.pose().popPose();
        }

        // 4. Причина смерти
        // Стандартный шрифт, серый цвет, чуть ниже центра
        if (this.causeOfDeath != null && this.delayTicker > 20) {
            Component cause = this.causeOfDeath;
            int causeWidth = this.font.width(cause);
            int causeX = (width - causeWidth) / 2;
            int causeY = height / 2 + 10; // Исправлено наложение (+10 вместо +20)
            graphics.drawString(this.font, cause, causeX, causeY, 0xFFAAAAAA, false);
        }
        
        // 5. Время в бою (Time Lived)
        if (this.delayTicker > 30) {
            long totalSeconds = Pjmbasemod$timeLived / 20;
            long h = totalSeconds / 3600;
            long m = (totalSeconds % 3600) / 60;
            long s = totalSeconds % 60;
            
            String timeString = String.format("%02d:%02d:%02d", h, m, s);
            Component timeComponent = Component.literal("Время в бою: " + timeString);
            
            int timeWidth = this.font.width(timeComponent);
            int timeX = (width - timeWidth) / 2;
            int timeY = height / 2 + 35;
            
            graphics.drawString(this.font, timeComponent, timeX, timeY, 0xFF888888, false);
        }
        
        // 6. Таймер респауна / Статус
        int respawnDelay = 300; // 15 секунд (300 тиков)
        int currentTick = this.delayTicker;
        
        if (currentTick < respawnDelay) {
            // Показываем таймер
            String loadingText = "RESPAWN IN " + String.format("%.1f", (respawnDelay - currentTick) / 20.0f) + "s";
            int loadWidth = this.font.width(loadingText);
            graphics.drawString(this.font, loadingText, (width - loadWidth) / 2, height - 60, 0xFF555555, false);
            
            // Тонкая полоска прогресса
            int barWidth = 120;
            int barHeight = 1;
            int barX = (width - barWidth) / 2;
            int barY = height - 45;
            float progress = (float) currentTick / respawnDelay;
            
            // Фон полоски (темно-серый)
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            // Активная часть (красная)
            graphics.fill(barX, barY, barX + (int)(barWidth * progress), barY + barHeight, 0xFF880000);
            
        } else {
            // Готов к респауну (таймер истек)
            String readyText = "[ PRESS ANY KEY ]";
            int readyWidth = this.font.width(readyText);
            
            // Пульсирующий эффект (белый -> серый)
            float alpha = (float) (0.7 + 0.3 * Math.sin(System.currentTimeMillis() / 200.0));
            int color = ((int) (alpha * 255) << 24) | 0xFFFFFF; 
            
            graphics.drawString(this.font, readyText, (width - readyWidth) / 2, height - 60, color, false);
            
            // Активируем скрытую кнопку респауна только после истечения таймера
            if (Pjmbasemod$hiddenRespawnButton != null && currentTick >= respawnDelay) {
                Pjmbasemod$hiddenRespawnButton.active = true;
            }
        }
        
        // Отменяем стандартный рендеринг
        ci.cancel();
    }
}
