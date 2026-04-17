package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /pjm zone - Зоны выбора классов
 */
public class ZoneCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("zone")
            .requires(src -> src.hasPermission(2))
            // /pjm zone create <name>
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> createZone(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            // /pjm zone delete <name>
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> deleteZone(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            // /pjm zone list
            .then(Commands.literal("list")
                .executes(ctx -> listZones(ctx.getSource())))
            // /pjm zone tp <name>
            .then(Commands.literal("tp")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> teleportToZone(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
    }

    private static int createZone(CommandSourceStack source, String name) {
        source.sendSuccess(() -> Component.translatable("commands.wrb.class.zone_created", name), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int deleteZone(CommandSourceStack source, String name) {
        source.sendSuccess(() -> Component.translatable("commands.wrb.class.zone_deleted", name), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listZones(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Зоны выбора классов ==="), false);
        source.sendSuccess(() -> Component.literal("§7Зоны не найдены"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportToZone(CommandSourceStack source, String name) {
        source.sendFailure(Component.literal("Зона '" + name + "' не найдена"));
        return 0;
    }
}
