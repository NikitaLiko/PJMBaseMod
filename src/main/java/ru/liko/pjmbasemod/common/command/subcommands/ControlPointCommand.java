package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /pjm cp - Контрольные точки
 */
public class ControlPointCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("cp")
            .requires(src -> src.hasPermission(2))
            // /pjm cp create <name>
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> createPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            // /pjm cp delete <name>
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> deletePoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            // /pjm cp list
            .then(Commands.literal("list")
                .executes(ctx -> listPoints(ctx.getSource())))
            // /pjm cp reset [name]
            .then(Commands.literal("reset")
                .executes(ctx -> resetAllPoints(ctx.getSource()))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> resetPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            // /pjm cp setowner <name> <team>
            .then(Commands.literal("setowner")
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("team", StringArgumentType.word())
                        .executes(ctx -> setOwner(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "name"),
                            StringArgumentType.getString(ctx, "team")
                        )))))
            // /pjm cp tp <name>
            .then(Commands.literal("tp")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> teleportToPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
    }

    private static int createPoint(CommandSourceStack source, String name) {
        source.sendSuccess(() -> Component.translatable("commands.wrb.capture.point.created", name, name, 0), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int deletePoint(CommandSourceStack source, String name) {
        source.sendSuccess(() -> Component.translatable("commands.wrb.capture.point.deleted", name), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listPoints(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Контрольные точки ==="), false);
        source.sendSuccess(() -> Component.translatable("commands.wrb.capture.point.empty"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetAllPoints(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§aВсе контрольные точки сброшены"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetPoint(CommandSourceStack source, String name) {
        source.sendSuccess(() -> Component.literal("§aКонтрольная точка '" + name + "' сброшена"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setOwner(CommandSourceStack source, String name, String team) {
        source.sendSuccess(() -> Component.translatable("commands.wrb.capture.point.owner", name, team), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportToPoint(CommandSourceStack source, String name) {
        source.sendFailure(Component.translatable("commands.wrb.capture.point.not_found", name));
        return 0;
    }
}
