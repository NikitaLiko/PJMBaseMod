package ru.liko.pjmbasemod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.pjmbasemod.Config;

import java.util.Map;

/**
 * Mixin для отключения звуков когда игрок мертв
 * Создает полную тишину при смерти для иммерсивности
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    
    // NeoForge 1.21.1: Use Mojang mappings
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    
    @Shadow public abstract void stopAll();
    
    /**
     * Флаг для отслеживания, были ли звуки уже остановлены при текущей смерти
     * Предотвращает многократный вызов stopAll()
     */
    @Unique
    private boolean Pjmbasemod$soundsStopped = false;

    /**
     * Безопасно получает значение конфига, возвращает false если конфиг не загружен
     */
    @Unique
    private boolean Pjmbasemod$isMuteSoundsEnabled() {
        try {
            return Config.isMuteSoundsOnDeath();
        } catch (Exception e) {
            // Конфиг ещё не загружен, возвращаем значение по умолчанию (false)
            return false;
        }
    }
    
    /**
     * Перехватываем воспроизведение звуков
     * Если игрок мертв и настройка включена - блокируем все звуки
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(SoundInstance sound, CallbackInfo ci) {
        if (!Pjmbasemod$isMuteSoundsEnabled()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        
        // Проверяем, что игрок существует и мертв
        if (minecraft.player != null && minecraft.player.isDeadOrDying()) {
            // Блокируем воспроизведение звука
            ci.cancel();
        }
    }
    
    /**
     * Также блокируем playDelayed для отложенных звуков
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void onPlayDelayedSound(SoundInstance sound, int delay, CallbackInfo ci) {
        if (!Pjmbasemod$isMuteSoundsEnabled()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        
        if (minecraft.player != null && minecraft.player.isDeadOrDying()) {
            ci.cancel();
        }
    }

    /**
     * Останавливаем все звуки когда игрок умирает
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(boolean isGamePaused, CallbackInfo ci) {
        if (!Pjmbasemod$isMuteSoundsEnabled()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        
        if (minecraft.player == null) {
            // Сбрасываем флаг когда игрок не существует
            Pjmbasemod$soundsStopped = false;
            return;
        }
        
        // Если игрок мертв и звуки ещё не остановлены
        if (minecraft.player.isDeadOrDying()) {
            if (!Pjmbasemod$soundsStopped && !instanceToChannel.isEmpty()) {
                // Останавливаем ВСЕ звуки
                stopAll();
                Pjmbasemod$soundsStopped = true;
            }
        } else {
            // Игрок жив - сбрасываем флаг для следующей смерти
            Pjmbasemod$soundsStopped = false;
        }
    }
}

