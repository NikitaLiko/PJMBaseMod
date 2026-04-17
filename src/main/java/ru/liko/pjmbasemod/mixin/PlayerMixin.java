package ru.liko.pjmbasemod.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.liko.pjmbasemod.Config;

/**
 * Mixin для блокировки выкидывания предметов игроками
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    /**
     * Перехватывает метод drop() который вызывается при выкидывании предметов (клавиша Q и Ctrl+Q)
     * Это самый надёжный способ блокировки выкидывания, так как перехватывается на уровне логики игрока
     */
    // NeoForge 1.21.1: Use Mojang mappings
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void onDropItem(ItemStack itemStack, boolean dropAround, boolean traceItem, CallbackInfoReturnable<ItemEntity> cir) {
        // Проверяем, включена ли блокировка выкидывания предметов
        if (!Config.isPreventItemDrop()) {
            return; // Блокировка выключена в конфиге
        }

        // Получаем игрока (this - это Player)
        Player player = (Player) (Object) this;

        // Проверяем, что это серверная сторона
        if (player.level().isClientSide) {
            return; // Только на сервере
        }

        // Проверяем, что это ServerPlayer
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return; // Не серверный игрок
        }

        // Проверяем, что игрок в режиме выживания
        if (serverPlayer.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            return; // Разрешено в креативе и других режимах
        }

        // Проверяем, что игрок не является оператором (OP level 1 и выше)
        if (serverPlayer.hasPermissions(1)) {
            return; // Операторы могут выкидывать предметы
        }

        // Блокируем выкидывание предмета
        cir.setReturnValue(null);

        // Отправляем сообщение игроку
        serverPlayer.displayClientMessage(
            Component.translatable("wrb.antigrief.item_drop_disabled"),
            true
        );
    }
}

