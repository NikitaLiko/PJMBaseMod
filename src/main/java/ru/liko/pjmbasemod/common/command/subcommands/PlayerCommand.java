package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.common.match.MatchManager;
import ru.liko.pjmbasemod.common.match.PlayerMatchStats;
import ru.liko.pjmbasemod.common.network.packet.OpenClassSelectionPacket;
import ru.liko.pjmbasemod.common.network.packet.OpenMapVotingPacket;
import ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket;
import ru.liko.pjmbasemod.common.network.packet.OpenSpawnMenuPacket;
import ru.liko.pjmbasemod.common.network.packet.OpenTeamSelectionPacket;
import ru.liko.pjmbasemod.common.util.TeamBalanceHelper;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmRank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /pjm player <player> - Управление игроками
 */
public class PlayerCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("player")
            .requires(src -> src.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.player())
                // /pjm player <player> team <team>
                .then(Commands.literal("team")
                    .then(Commands.argument("teamId", StringArgumentType.word())
                        .executes(ctx -> setTeam(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target"),
                            StringArgumentType.getString(ctx, "teamId")
                        ))))
                // /pjm player <player> class <class>
                .then(Commands.literal("class")
                    .then(Commands.argument("classId", StringArgumentType.word())
                        .executes(ctx -> setClass(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target"),
                            StringArgumentType.getString(ctx, "classId")
                        ))))
                // /pjm player <player> rank <rank>
                .then(Commands.literal("rank")
                    .then(Commands.argument("rankId", StringArgumentType.word())
                        .executes(ctx -> setRank(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target"),
                            StringArgumentType.getString(ctx, "rankId")
                        ))))
                // /pjm player <player> points add <amount>
                .then(Commands.literal("points")
                    .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(ctx -> addPoints(
                                ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "amount")
                            ))))
                    .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(ctx -> setPoints(
                                ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "amount")
                            )))))
                // /pjm player <player> info
                .then(Commands.literal("info")
                    .executes(ctx -> showInfo(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "target")
                    )))
                // /pjm player <player> openmenu class
                .then(Commands.literal("openmenu")
                    .then(Commands.literal("class")
                        .executes(ctx -> openClassMenu(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target")
                        )))
                    .then(Commands.literal("team")
                        .executes(ctx -> openTeamMenu(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target")
                        )))
                    .then(Commands.literal("spawn")
                        .executes(ctx -> openSpawnMenu(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target")
                        )))
                    .then(Commands.literal("stats")
                        .executes(ctx -> openStatsMenu(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target")
                        )))
                    .then(Commands.literal("mapvoting")
                        .executes(ctx -> openMapVotingMenu(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target")
                        )))
                    .then(Commands.literal("lobby")
                        .executes(ctx -> openLobbyMenu(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target")
                        ))))
            );
    }

    private static int setTeam(CommandSourceStack source, ServerPlayer target, String teamId) {
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        data.setTeam(teamId);
        source.sendSuccess(() -> Component.translatable("commands.wrb.team.changed", target.getName().getString(), teamId), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setClass(CommandSourceStack source, ServerPlayer target, String classId) {
        PjmPlayerClass playerClass = PjmPlayerClass.fromId(classId).orElse(null);
        if (playerClass == null) {
            source.sendFailure(Component.translatable("commands.wrb.class.invalid_class", classId));
            return 0;
        }
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        data.setPlayerClass(playerClass);
        source.sendSuccess(() -> Component.literal("§aКласс игрока " + target.getName().getString() + " установлен: " + playerClass.getDisplayName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setRank(CommandSourceStack source, ServerPlayer target, String rankId) {
        PjmRank rank = PjmRank.fromKeyOrDefault(rankId);
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        data.setRank(rank);
        source.sendSuccess(() -> Component.translatable("commands.wrb.rank.changed", target.getName().getString()).append(Component.literal(" " + rank.getDisplayName())), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int addPoints(CommandSourceStack source, ServerPlayer target, int amount) {
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        data.addRankPoints(amount);
        source.sendSuccess(() -> Component.literal("§aДобавлено " + amount + " очков игроку " + target.getName().getString() + ". Всего: " + data.getRankPoints()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPoints(CommandSourceStack source, ServerPlayer target, int amount) {
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        data.setRankPoints(amount);
        source.sendSuccess(() -> Component.literal("§aОчки игрока " + target.getName().getString() + " установлены: " + amount), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int showInfo(CommandSourceStack source, ServerPlayer target) {
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        source.sendSuccess(() -> Component.literal("§6=== Игрок: " + target.getName().getString() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§eКоманда: §f" + data.getTeam()), false);
        source.sendSuccess(() -> Component.literal("§eКласс: §f" + data.getPlayerClass().getDisplayName()), false);
        source.sendSuccess(() -> Component.literal("§eРанг: §f" + data.getRank().getDisplayName()), false);
        source.sendSuccess(() -> Component.literal("§eОчки: §f" + data.getRankPoints()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int openClassMenu(CommandSourceStack source, ServerPlayer target) {
        // Вызываем серверную логику напрямую вместо отправки C2S пакета
        OpenClassSelectionPacket.handleForPlayer(target);
        source.sendSuccess(() -> Component.literal("§aОткрыто меню выбора класса для " + target.getName().getString()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openTeamMenu(CommandSourceStack source, ServerPlayer target) {
        var balanceInfo = TeamBalanceHelper.getTeamBalanceInfo(source.getServer());
        var packet = OpenTeamSelectionPacket.create(
            balanceInfo,
            Config.getTeam1Name(),
            Config.getTeam2Name(),
            true,
            Config.getTeamBalanceThreshold()
        );
        PacketDistributor.sendToPlayer(target, packet);
        source.sendSuccess(() -> Component.literal("§aОткрыто меню выбора команды для " + target.getName().getString()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openSpawnMenu(CommandSourceStack source, ServerPlayer target) {
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        String teamName = data.getTeam();
        
        // Формируем список доступных точек спавна
        List<OpenSpawnMenuPacket.SpawnPoint> spawnPoints = new ArrayList<>();
        
        // База команды (доступна если у игрока есть команда и матч активен)
        boolean hasTeam = teamName != null && !teamName.isEmpty();
        boolean matchActive = ru.liko.pjmbasemod.common.match.MatchManager.get().getState().isGameActive();
        
        spawnPoints.add(new OpenSpawnMenuPacket.SpawnPoint(
            "team_base",
            "База команды" + (hasTeam ? " (" + teamName + ")" : ""),
            "team_base",
            hasTeam && matchActive
        ));
        
        // Лобби (всегда доступно)
        spawnPoints.add(new OpenSpawnMenuPacket.SpawnPoint(
            "lobby",
            "Лобби",
            "lobby",
            true
        ));
        
        OpenSpawnMenuPacket packet = new OpenSpawnMenuPacket(spawnPoints, Config.getSpawnCooldownSeconds());
        PacketDistributor.sendToPlayer(target, packet);
        
        source.sendSuccess(() -> Component.literal("§aОткрыто меню спавна для " + target.getName().getString()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openStatsMenu(CommandSourceStack source, ServerPlayer target) {
        MatchManager matchManager = MatchManager.get();

        // Собираем статистику всех игроков
        List<OpenMatchStatsPacket.PlayerStatsEntry> statsEntries = new ArrayList<>();

        if (source.getServer() != null) {
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                PlayerMatchStats stats = matchManager.getStatsFor(uuid);
                PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
                String team = data.getTeam();

                statsEntries.add(new OpenMatchStatsPacket.PlayerStatsEntry(
                    player.getName().getString(),
                    team,
                    stats.getKills(),
                    stats.getDeaths(),
                    stats.getAssists(),
                    stats.getCapturePoints(),
                    stats.getScore()
                ));
            }
        }

        // Определяем "победителя" по сумме очков команд
        String winnerTeam = determineWinnerTeam(statsEntries);

        // Вычисляем длительность матча (в секундах)
        int duration = 0;
        if (matchManager.getState().isGameActive()) {
            // Если матч активен — считаем прошедшее время
            // stateTimer считает вниз, начальное значение зависит от конфига карты
            // Для простоты — 0 (неизвестно)
        }

        OpenMatchStatsPacket packet = new OpenMatchStatsPacket(
            winnerTeam,
            "Просмотр статистики",
            duration,
            statsEntries
        );

        PacketDistributor.sendToPlayer(target, packet);
        source.sendSuccess(() -> Component.literal("§aОткрыт экран статистики матча для " + target.getName().getString()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openMapVotingMenu(CommandSourceStack source, ServerPlayer target) {
        PacketDistributor.sendToPlayer(target, OpenMapVotingPacket.fromServer());
        source.sendSuccess(() -> Component.literal("§aОткрыт экран голосования за карту для " + target.getName().getString()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openLobbyMenu(CommandSourceStack source, ServerPlayer target) {
        PacketDistributor.sendToPlayer(target, new ru.liko.pjmbasemod.common.network.packet.OpenMatchLobbyPacket());
        source.sendSuccess(() -> Component.literal("§aОткрыт экран лобби матча для " + target.getName().getString()), true);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Определяет победившую команду по сумме очков.
     */
    private static String determineWinnerTeam(List<OpenMatchStatsPacket.PlayerStatsEntry> stats) {
        Map<String, Integer> teamScores = new java.util.HashMap<>();
        for (OpenMatchStatsPacket.PlayerStatsEntry entry : stats) {
            teamScores.merge(entry.team(), entry.score(), Integer::sum);
        }

        return teamScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");
    }
}
