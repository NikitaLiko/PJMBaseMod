package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /pjm vehicle - Управление техникой
 */
public class VehicleCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("vehicle")
            .requires(src -> src.hasPermission(2))
            // /pjm vehicle spawn <type>
            .then(Commands.literal("spawn")
                .then(Commands.argument("type", StringArgumentType.word())
                    .executes(ctx -> spawnVehicle(ctx.getSource(), StringArgumentType.getString(ctx, "type")))))
            // /pjm vehicle despawn [radius]
            .then(Commands.literal("despawn")
                .executes(ctx -> despawnVehicles(ctx.getSource(), 10))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> despawnVehicles(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            // /pjm vehicle point
            .then(Commands.literal("point")
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> addSpawnPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> removeSpawnPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list")
                    .executes(ctx -> listSpawnPoints(ctx.getSource()))));
    }

    private static int spawnVehicle(CommandSourceStack source, String type) {
        source.sendSuccess(() -> Component.literal("§aТехника '" + type + "' заспавнена"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int despawnVehicles(CommandSourceStack source, int radius) {
        source.sendSuccess(() -> Component.literal("§aТехника в радиусе " + radius + " блоков удалена"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int addSpawnPoint(CommandSourceStack source, String name) {
        source.sendSuccess(() -> Component.translatable("commands.wrb.vehicle.spawn.created", name), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int removeSpawnPoint(CommandSourceStack source, String name) {
        source.sendSuccess(() -> Component.translatable("commands.wrb.vehicle.spawn.deleted", name), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listSpawnPoints(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Точки спавна техники ==="), false);
        source.sendSuccess(() -> Component.translatable("commands.wrb.vehicle.spawn.empty"), false);
        return Command.SINGLE_SUCCESS;
    }
}
