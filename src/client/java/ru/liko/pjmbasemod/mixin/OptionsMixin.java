package ru.liko.pjmbasemod.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.pjmbasemod.client.mixin.access.DeathScreenMixinAccess;

/**
 * Mixin для блокировки F3+B (hitboxes) для игроков без OP прав
 * Перехватывает обработку клавиш в KeyboardHandler
 */
@Mixin(KeyboardHandler.class)
public abstract class OptionsMixin {

    /**
     * Перехватывает обработку клавиш для блокировки F3+B (hitboxes)
     * Проверяет нажатие клавиши B при удержании F3 и блокирует для не-OP игроков
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), cancellable = true)
    private void onKeyPressF3B(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        // Респаун по любой клавише (кроме ESC) для чёрного экрана смерти
        // Делать это здесь надёжнее, чем через Screen/AbstractContainerEventHandler: это централизованный ввод.
        if (action == GLFW.GLFW_PRESS) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof DeathScreen && minecraft.screen instanceof DeathScreenMixinAccess access) {
                int respawnDelay = 300; // Должно совпадать с DeathScreenMixin (15 секунд)
                // Разрешаем респаун только после истечения таймера
                if (access.Pjmbasemod$isBlackScreenEnabled() && access.Pjmbasemod$getDelayTicker() >= respawnDelay && key != GLFW.GLFW_KEY_ESCAPE) {
                    LocalPlayer player = minecraft.player;
                    if (player != null) {
                        player.respawn();
                        ci.cancel();
                        return;
                    }
                } else if (access.Pjmbasemod$isBlackScreenEnabled() && access.Pjmbasemod$getDelayTicker() < respawnDelay) {
                    // Блокируем нажатия клавиш, если таймер не истек (кроме ESC для закрытия экрана)
                    if (key != GLFW.GLFW_KEY_ESCAPE) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        // Проверяем, что нажата клавиша B и удерживается F3
        if (key == GLFW.GLFW_KEY_B && action == GLFW.GLFW_PRESS) {
            boolean f3Pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;
            
            if (f3Pressed) {
                Minecraft minecraft = Minecraft.getInstance();
                LocalPlayer player = minecraft.player;
                
                // Проверяем, что игрок существует и в игре
                if (player != null && minecraft.level != null) {
                    // Проверяем, что игрок не является оператором (OP level 1 и выше)
                    if (!player.hasPermissions(1)) {
                        // Блокируем обработку F3+B
                        ci.cancel();
                        
                        // Отправляем сообщение игроку
                        player.displayClientMessage(
                            Component.translatable("wrb.debug.f3b_disabled"),
                            true
                        );
                    }
                }
            }
        }
    }
}

