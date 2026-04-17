package ru.liko.pjmbasemod.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.pjmbasemod.Config;

/**
 * Mixin для блокировки подбора предметов игроками
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    /**
     * Перехватывает метод playerTouch(), который вызывается когда игрок касается предмета (подбирает его)
     * Это самый надёжный способ блокировки подбора, так как перехватывается на уровне сущности предмета
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {
        // Проверяем, включена ли блокировка подбора предметов
        if (!Config.isPreventItemPickup()) {
            return; // Блокировка выключена в конфиге
        }

        // Проверяем, что это серверная сторона
        if (player.level().isClientSide) {
            return; // Только на сервере
        }

        // Проверяем, что это ServerPlayer
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return; // Не серверный игрок
        }

        // Проверяем, что игрок в режиме выживания (опционально, можно убрать если нужно блокировать во всех режимах)
        if (serverPlayer.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            return; // Разрешено в креативе и других режимах
        }

        // Проверяем, что игрок не является оператором (OP level 1 и выше)
        if (serverPlayer.hasPermissions(1)) {
            return; // Операторы могут подбирать предметы
        }

        // Блокируем подбор предмета
        ci.cancel();

        // Отправляем сообщение игроку (не слишком часто, чтобы не спамить)
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (itemEntity.tickCount % 40 == 0) { // Раз в 2 секунды (40 тиков)
            serverPlayer.displayClientMessage(
                Component.translatable("wrb.antigrief.item_pickup_disabled"),
                true
            );
        }
    }
}

