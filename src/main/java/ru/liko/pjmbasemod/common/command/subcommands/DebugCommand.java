package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.SyncPjmDataPacket;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;

/**
 * /pjm debug - Отладка (OP only)
 */
public class DebugCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("debug")
            .requires(src -> src.hasPermission(4))
            // /pjm debug sync <player>
            .then(Commands.literal("sync")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> syncPlayer(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
            // /pjm debug netstat
            .then(Commands.literal("netstat")
                .executes(ctx -> showNetstat(ctx.getSource())))
            // /pjm debug dump
            .then(Commands.literal("dump")
                .executes(ctx -> dumpState(ctx.getSource())));
    }

    private static int syncPlayer(CommandSourceStack source, ServerPlayer target) {
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        SyncPjmDataPacket packet = SyncPjmDataPacket.fromPlayerData(target.getId(), data);
        PjmNetworking.sendToClient(packet, target);
        source.sendSuccess(() -> Component.literal("§aДанные игрока " + target.getName().getString() + " синхронизированы"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int showNetstat(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Статистика сети ==="), false);
        source.sendSuccess(() -> Component.literal("§7Зарегистрировано пакетов: 21"), false);
        source.sendSuccess(() -> Component.literal("§7Версия протокола: 1.0"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int dumpState(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Дамп состояния ==="), false);
        int playerCount = source.getServer().getPlayerCount();
        source.sendSuccess(() -> Component.literal("§eИгроков онлайн: §f" + playerCount), false);
        return Command.SINGLE_SUCCESS;
    }
}
