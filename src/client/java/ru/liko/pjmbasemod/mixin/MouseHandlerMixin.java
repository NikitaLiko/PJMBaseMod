package ru.liko.pjmbasemod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.pjmbasemod.client.mixin.access.DeathScreenMixinAccess;

/**
 * Респаун по клику мыши на чёрном экране смерти.
 * Делать это через MouseHandler надёжнее, чем пытаться инжектиться в mouseClicked у GUI-хэндлеров.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void Pjmbasemod$onPress(long window, int button, int action, int mods, CallbackInfo ci) {
        // Интересуют только нажатия кнопок
        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof DeathScreen)) {
            return;
        }
        if (!(minecraft.screen instanceof DeathScreenMixinAccess access)) {
            return;
        }
        
        // Проверяем, включен ли черный экран и истек ли таймер респауна (300 тиков = 15 секунд)
        int respawnDelay = 300; // Должно совпадать с DeathScreenMixin
        if (!access.Pjmbasemod$isBlackScreenEnabled() || access.Pjmbasemod$getDelayTicker() < respawnDelay) {
            // Блокируем клик, если таймер не истек
            ci.cancel();
            return;
        }

        // Таймер истек - разрешаем респаун
        LocalPlayer player = minecraft.player;
        if (player != null) {
            player.respawn();
            ci.cancel();
        }
    }
}


