package ru.liko.pjmbasemod.common.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.chat.ChatManager;
import ru.liko.pjmbasemod.common.chat.ChatMode;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmPlayerDataProvider;
import ru.liko.pjmbasemod.common.player.PjmRank;

import java.util.List;

/**
 * Обработчик событий для системы локального/глобального чата
 * NeoForge 1.21.1: Updated event bus annotations
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ChatEvents {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        // Проверяем, включена ли система чата
        if (!Config.isChatSystemEnabled()) {
            return; // Пропускаем обработку, используем стандартный чат
        }

        ServerPlayer sender = event.getPlayer();
        ChatMode mode = ChatManager.getChatMode(sender);

        // Получаем список всех игроков на сервере
        List<ServerPlayer> allPlayers = sender.server.getPlayerList().getPlayers();

        // Фильтруем получателей в зависимости от режима чата
        List<ServerPlayer> recipients = ChatManager.getRecipients(sender, allPlayers);

        // Если нет получателей (кроме отправителя в командном чате), уведомляем
        if (recipients.isEmpty() || (mode == ChatMode.TEAM && recipients.size() == 1)) {
            sender.displayClientMessage(
                Component.literal("§cНикто не может услышать ваше сообщение в этом режиме чата!"),
                false
            );
        }

        // Отменяем стандартную рассылку
        event.setCanceled(true);

        // Получаем звание игрока - NeoForge 1.21.1: Use Data Attachments
        PjmPlayerData playerData = PjmPlayerDataProvider.get(sender);
        PjmRank rank = playerData.getRank();
        
        // Формируем компоненты сообщения
        // 1. Вертикальная черта (цвет режима)
        Component bar = Component.literal("│ ").withStyle(mode.getColor());

        // 2. Префикс режима (цвет режима)
        // getFormattedPrefix уже содержит пробел в конце: "[LOCAL] "
        Component prefix = mode.getFormattedPrefix();
        
        // 3. Звание игрока (если есть) - с иконкой через custom font
        Component rankComponent = Component.empty();
        if (rank != null) {
            String iconChar = rank.getIconChar();
            if (!iconChar.isEmpty()) {
                // Иконка + текст звания
                rankComponent = Component.literal(iconChar + " ")
                    .append(rank.getDisplayName())
                    .append(" ")
                    .withStyle(ChatFormatting.GRAY);
            } else {
                // Только текст звания
                rankComponent = Component.literal("[")
                    .append(rank.getDisplayName())
                    .append("] ")
                    .withStyle(ChatFormatting.GRAY);
            }
        }
        
        // 4. Имя игрока (белый цвет)
        Component name = Component.literal(sender.getName().getString() + " ").withStyle(ChatFormatting.WHITE);
        
        // 5. Разделитель (темно-серый)
        Component separator = Component.literal(">>> ").withStyle(ChatFormatting.DARK_GRAY);
        
        // 6. Само сообщение
        Component originalMessage = event.getMessage();

        // Собираем все вместе: │ [CHANNEL] [Звание] Имя >>> Сообщение
        Component decoratedMessage = Component.empty()
            .append(bar)
            .append(prefix)
            .append(rankComponent)
            .append(name)
            .append(separator)
            .append(originalMessage);

        // Отправляем сообщение только отфильтрованным получателям
        for (ServerPlayer recipient : recipients) {
            recipient.sendSystemMessage(decoratedMessage);
        }

        // Логируем в консоль сервера
        if (Config.isDebugLoggingEnabled()) {
            com.mojang.logging.LogUtils.getLogger().info("[{}] <{}> {}",
                mode.getId().toUpperCase(),
                sender.getName().getString(),
                originalMessage.getString()
            );
        }
    }
}
