package ru.liko.pjmbasemod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.pjmbasemod.Config;

/**
 * Mixin для отключения системы голода и естественной регенерации здоровья
 * для реалистичного военного симулятора (милсим)
 */
@Mixin(FoodData.class)
public class FoodDataMixin {

    /**
     * Блокирует работу системы голода (уменьшение уровня голода и насыщения)
     * и предотвращает естественную регенерацию здоровья через голод
     */
    // NeoForge 1.21.1: Use Mojang mappings (official names)
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(Player player, CallbackInfo ci) {
        // Проверяем, включено ли отключение голода в конфиге
        if (!Config.isDisableHunger()) {
            return; // Система голода включена
        }

        // Полностью блокируем работу системы голода
        // Это предотвращает:
        // 1. Уменьшение уровня голода
        // 2. Уменьшение насыщения
        // 3. Естественную регенерацию здоровья через голод
        ci.cancel();
    }

    /**
     * Блокирует истощение (exhaustion) которое используется для регенерации
     * Это ключевой метод - именно через exhaustion происходит естественная регенерация здоровья
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void onAddExhaustion(float exhaustion, CallbackInfo ci) {
        if (!Config.isDisableHunger()) {
            return;
        }
        // Блокируем истощение (это предотвращает систему регенерации через голод)
        ci.cancel();
    }
    
    /**
     * Блокирует использование еды для восстановления голода
     * Перехватываем метод eat, который вызывается при употреблении пищи
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "eat", at = @At("HEAD"), cancellable = true)
    private void onEat(int foodLevel, float saturationModifier, CallbackInfo ci) {
        if (!Config.isDisableHunger()) {
            return;
        }
        // Блокируем восстановление голода через еду
        ci.cancel();
    }
}

