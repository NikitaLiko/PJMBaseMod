package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.common.chat.ChatManager;
import ru.liko.pjmbasemod.common.chat.ChatMode;

/**
 * /chat <mode> - Смена режима чата
 */
public class ChatCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chat")
            .executes(ctx -> showCurrentMode(ctx.getSource()))
            .then(Commands.argument("mode", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    builder.suggest("local");
                    builder.suggest("global");
                    builder.suggest("team");
                    return builder.buildFuture();
                })
                .executes(ctx -> setChatMode(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))));
    }

    private static int showCurrentMode(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("wrb.chat.player_only"));
            return 0;
        }
        
        ChatMode mode = ChatManager.getChatMode(player);
        source.sendSuccess(() -> Component.translatable("wrb.chat.status").append(Component.literal(" " + mode.getDisplayName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setChatMode(CommandSourceStack source, String modeStr) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("wrb.chat.player_only"));
            return 0;
        }
        
        ChatMode mode = ChatMode.fromString(modeStr);
        if (mode == null) {
            source.sendFailure(Component.translatable("wrb.chat.unknown_mode", modeStr));
            source.sendFailure(Component.translatable("wrb.chat.available_modes"));
            return 0;
        }
        
        ChatManager.setChatMode(player, mode);
        source.sendSuccess(() -> Component.translatable("wrb.chat.changed").append(Component.literal(" " + mode.getDisplayName())), false);
        return Command.SINGLE_SUCCESS;
    }
}
