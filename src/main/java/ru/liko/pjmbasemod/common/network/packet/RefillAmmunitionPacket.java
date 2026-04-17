package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmPlayerDataProvider;

/**
 * Пакет для полной перевыдачи кита игрока.
 * Полностью перевыдает все предметы класса, включая оружие, броню и расходники.
 * NeoForge 1.21.1: record implementing CustomPacketPayload.
 */
public record RefillAmmunitionPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RefillAmmunitionPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "refill_ammunition"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RefillAmmunitionPacket> STREAM_CODEC = 
        StreamCodec.unit(new RefillAmmunitionPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RefillAmmunitionPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }

            // Получаем текущий класс игрока
            var data = PjmPlayerDataProvider.get(player);

            PjmPlayerClass playerClass = data.getPlayerClass();
            
            // Проверяем, что класс выбран (не NONE)
            if (!playerClass.isSelectable()) {
                player.displayClientMessage(
                    Component.translatable("wrb.ammunition.error.no_class"),
                    true
                );
                return;
            }

            // Получаем команду игрока
            net.minecraft.world.scores.Team playerTeam = player.getTeam();
            String playerTeamName = playerTeam != null ? playerTeam.getName() : Config.getTeam1Name();

            // Проверяем кулдаун на взятие кита только для обычных игроков (не OP)
            if (!player.hasPermissions(1)) {
                int cooldownSeconds = Config.getKitCooldownSeconds();
                if (!data.canTakeKit(cooldownSeconds)) {
                    int remainingSeconds = data.getRemainingCooldownSeconds(cooldownSeconds);
                    int minutes = remainingSeconds / 60;
                    int seconds = remainingSeconds % 60;
                    String timeStr;
                    if (minutes > 0) {
                        timeStr = String.format("%d мин %d сек", minutes, seconds);
                    } else {
                        timeStr = String.format("%d сек", seconds);
                    }
                    player.displayClientMessage(
                        Component.translatable("wrb.class.error.cooldown", timeStr),
                        true
                    );
                    return;
                }
                // Обновляем время последнего взятия кита для обычных игроков
                data.setLastKitTime(System.currentTimeMillis());
                // Синхронизируем обновленные данные с клиентом
                ru.liko.pjmbasemod.common.network.PjmNetworking.sendToClient(
                    SyncPjmDataPacket.fromPlayerData(player.getId(), data),
                    player
                );
            }

            // Полностью перевыдаем весь кит (все предметы класса)
            // Примечание: Сейчас перевыдается дефолтный кит. 
            java.util.List<String> items = Config.getClassItemStrings(playerClass.getId(), playerTeamName);
            ru.liko.pjmbasemod.common.network.packet.SelectClassPacket.giveClassItems(player, items, true);

            // Сообщение об успешной перевыдаче кита
            player.displayClientMessage(
                Component.translatable("wrb.ammunition.kit_reissued"),
                false
            );
        });
    }
}

