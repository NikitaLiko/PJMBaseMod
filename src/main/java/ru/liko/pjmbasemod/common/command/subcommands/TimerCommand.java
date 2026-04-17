package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /pjm timer - Таймеры
 */
public class TimerCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("timer")
            .requires(src -> src.hasPermission(2))
            // /pjm timer start <seconds> [message]
            .then(Commands.literal("start")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                    .executes(ctx -> startTimer(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds"), null))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> startTimer(
                            ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "seconds"),
                            StringArgumentType.getString(ctx, "message")
                        )))))
            // /pjm timer stop
            .then(Commands.literal("stop")
                .executes(ctx -> stopTimer(ctx.getSource())))
            // /pjm timer broadcast <message>
            .then(Commands.literal("broadcast")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> broadcast(ctx.getSource(), StringArgumentType.getString(ctx, "message")))));
    }

    private static int startTimer(CommandSourceStack source, int seconds, String message) {
        String msg = message != null ? message : "Таймер завершён!";
        source.sendSuccess(() -> Component.literal("§aТаймер запущен на " + seconds + " секунд. Сообщение: " + msg), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopTimer(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§aТаймер остановлен"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int broadcast(CommandSourceStack source, String message) {
        source.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§6[Объявление] §f" + message), false);
        return Command.SINGLE_SUCCESS;
    }
}
