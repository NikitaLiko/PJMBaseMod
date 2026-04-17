package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.util.TeamBalanceHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /pjm team - Управление командами
 */
public class TeamCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("team")
            .requires(src -> src.hasPermission(2))
            // /pjm team balance
            .then(Commands.literal("balance")
                .executes(ctx -> showBalance(ctx.getSource())))
            // /pjm team swap <player>
            .then(Commands.literal("swap")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> swapPlayer(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
            // /pjm team shuffle
            .then(Commands.literal("shuffle")
                .executes(ctx -> shuffleTeams(ctx.getSource())));
    }

    private static int showBalance(CommandSourceStack source) {
        String team1Name = Config.getTeam1Name();
        String team2Name = Config.getTeam2Name();
        int team1Count = TeamBalanceHelper.getTeamPlayerCount(source.getServer(), team1Name);
        int team2Count = TeamBalanceHelper.getTeamPlayerCount(source.getServer(), team2Name);
        int threshold = Config.getTeamBalanceThreshold();
        
        source.sendSuccess(() -> Component.literal("§6=== Баланс команд ==="), false);
        source.sendSuccess(() -> Component.literal("§e" + team1Name + ": §f" + team1Count + " игроков"), false);
        source.sendSuccess(() -> Component.literal("§e" + team2Name + ": §f" + team2Count + " игроков"), false);
        
        int diff = Math.abs(team1Count - team2Count);
        if (diff > threshold) {
            source.sendSuccess(() -> Component.literal("§cДисбаланс: " + diff + " (порог: " + threshold + ")"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§aКоманды сбалансированы"), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int swapPlayer(CommandSourceStack source, ServerPlayer target) {
        String team1Name = Config.getTeam1Name();
        String team2Name = Config.getTeam2Name();
        
        net.minecraft.world.scores.Team currentTeam = target.getTeam();
        String currentTeamName = currentTeam != null ? currentTeam.getName() : "";
        String newTeamName = currentTeamName.equalsIgnoreCase(team1Name) ? team2Name : team1Name;
        
        // Перемещаем через Scoreboard для корректной работы
        MinecraftServer server = source.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam newTeam = scoreboard.getPlayerTeam(newTeamName);
        
        if (newTeam != null) {
            scoreboard.addPlayerToTeam(target.getScoreboardName(), newTeam);
            // Обновляем данные в PjmPlayerData
            PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
            data.setTeam(newTeamName);
            source.sendSuccess(() -> Component.literal("§aИгрок " + target.getName().getString() + " перемещён в команду §e" + newTeamName), true);
        } else {
            source.sendFailure(Component.literal("§cКоманда " + newTeamName + " не найдена в скорборде!"));
            return 0;
        }
        
        return Command.SINGLE_SUCCESS;
    }

    private static int shuffleTeams(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (server == null) return 0;
        
        String team1Name = Config.getTeam1Name();
        String team2Name = Config.getTeam2Name();
        Scoreboard scoreboard = server.getScoreboard();
        
        // Получаем команды из скорборда
        PlayerTeam scoreboardTeam1 = scoreboard.getPlayerTeam(team1Name);
        PlayerTeam scoreboardTeam2 = scoreboard.getPlayerTeam(team2Name);
        
        if (scoreboardTeam1 == null || scoreboardTeam2 == null) {
            source.sendFailure(Component.literal("§cОшибка: команды " + team1Name + " или " + team2Name + " не найдены в скорборде!"));
            return 0;
        }
        
        // Собираем всех игроков, которые в любой из двух команд
        List<ServerPlayer> allTeamPlayers = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.minecraft.world.scores.Team playerTeam = player.getTeam();
            if (playerTeam != null && 
                (playerTeam.getName().equalsIgnoreCase(team1Name) || playerTeam.getName().equalsIgnoreCase(team2Name))) {
                allTeamPlayers.add(player);
            }
        }
        
        if (allTeamPlayers.isEmpty()) {
            source.sendFailure(Component.literal("§cНет игроков в командах для перемешивания!"));
            return 0;
        }
        
        // Перемешиваем список случайным образом
        Collections.shuffle(allTeamPlayers);
        
        // Распределяем равномерно: первая половина в team1, вторая в team2
        int half = allTeamPlayers.size() / 2;
        int team1Count = 0;
        int team2Count = 0;
        
        for (int i = 0; i < allTeamPlayers.size(); i++) {
            ServerPlayer player = allTeamPlayers.get(i);
            String assignedTeam;
            
            if (i < half) {
                assignedTeam = team1Name;
                team1Count++;
            } else {
                assignedTeam = team2Name;
                team2Count++;
            }
            
            // Обновляем Scoreboard
            PlayerTeam targetTeam = assignedTeam.equals(team1Name) ? scoreboardTeam1 : scoreboardTeam2;
            scoreboard.addPlayerToTeam(player.getScoreboardName(), targetTeam);
            
            // Обновляем PjmPlayerData
            PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
            data.setTeam(assignedTeam);
            
            // Уведомляем игрока
            player.displayClientMessage(
                Component.literal("§eВы были перемещены в команду §a" + assignedTeam + " §e(перемешивание)"), false);
        }
        
        int finalTeam1Count = team1Count;
        int finalTeam2Count = team2Count;
        source.sendSuccess(() -> Component.literal("§aКоманды перемешаны! §e" + team1Name + ": " + finalTeam1Count + ", " + team2Name + ": " + finalTeam2Count), true);
        return Command.SINGLE_SUCCESS;
    }
}
