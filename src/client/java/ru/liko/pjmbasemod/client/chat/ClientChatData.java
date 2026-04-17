package ru.liko.pjmbasemod.client.chat;

import ru.liko.pjmbasemod.common.chat.ChatMode;

/**
 * Кэш данных о режиме чата на клиенте
 */
public class ClientChatData {
    private static ChatMode currentChatMode = ChatMode.LOCAL;

    public static ChatMode getChatMode() {
        return currentChatMode;
    }

    public static void setChatMode(ChatMode mode) {
        currentChatMode = mode;
    }

    public static void reset() {
        currentChatMode = ChatMode.LOCAL;
    }
}
