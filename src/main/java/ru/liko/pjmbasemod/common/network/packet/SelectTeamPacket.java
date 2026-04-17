package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmPlayerDataProvider;
import ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper;
import ru.liko.pjmbasemod.common.util.TeamBalanceHelper;

import java.util.List;

/**
 * Пакет для выбора команды игроком.
 * Отправляется клиентом на сервер при выборе команды из меню.
 * NeoForge 1.21.1: record implementing CustomPacketPayload.
 */
public record SelectTeamPacket(String teamId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectTeamPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "select_team"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectTeamPacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectTeamPacket::teamId,
            SelectTeamPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectTeamPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }

            // Определяем название команды
            String teamName;
            if ("team1".equals(packet.teamId)) {
                teamName = Config.getTeam1Name();
            } else if ("team2".equals(packet.teamId)) {
                teamName = Config.getTeam2Name();
            } else {
                // Неверный ID команды
                PjmNetworking.sendToClient(
                    new SyncTeamSelectionResultPacket(false, Component.translatable("wrb.team.error.invalid_team").getString()),
                    player
                );
                return;
            }

            // Проверяем баланс команд
            int balanceThreshold = Config.getTeamBalanceThreshold();
            if (!TeamBalanceHelper.canJoinTeam(player.getServer(), teamName, balanceThreshold)) {
                // Команда переполнена или баланс нарушен - отправляем ошибку (не закрываем экран)
                PjmNetworking.sendToClient(
                    new SyncTeamSelectionResultPacket(false, Component.translatable("wrb.team.error.balance").getString()),
                    player
                );
                return;
            }

            // Добавляем игрока в команду через scoreboard
            Scoreboard scoreboard = player.getServer().getScoreboard();
            
            // Получаем команду по имени (используем getAllPlayerTeams и ищем по имени)
            net.minecraft.world.scores.PlayerTeam team = null;
            for (net.minecraft.world.scores.PlayerTeam existingTeam : scoreboard.getPlayerTeams()) {
                if (existingTeam.getName().equals(teamName)) {
                    team = existingTeam;
                    break;
                }
            }
            
            if (team == null) {
                // Команда не существует, создаем её
                team = scoreboard.addPlayerTeam(teamName);
                if (team != null) {
                    // Устанавливаем цвет команды
                    if ("team1".equals(packet.teamId)) {
                        team.setColor(net.minecraft.ChatFormatting.RED); // ОКВ - красная
                    } else {
                        team.setColor(net.minecraft.ChatFormatting.YELLOW); // ОКС - желтая
                    }
                }
            }
            
            if (team != null) {
                // Удаляем игрока из старой команды (если была)
                net.minecraft.world.scores.Team oldTeam = scoreboard.getPlayersTeam(player.getScoreboardName());
                if (oldTeam != null && oldTeam instanceof net.minecraft.world.scores.PlayerTeam oldPlayerTeam) {
                    scoreboard.removePlayerFromTeam(player.getScoreboardName(), oldPlayerTeam);
                }
                
                // Добавляем игрока в новую команду
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
            }

            // Обновляем данные игрока
            { PjmPlayerData data = PjmPlayerDataProvider.get(player);
                // Сохраняем название команды
                data.setTeam(teamName);
                
                // Регистрируем игрока в MatchManager, чтобы метки работали
                ru.liko.pjmbasemod.common.match.MatchManager.get().playerJoinTeam(player, teamName);
                
                // Если у игрока был выбран класс - сбрасываем его
                if (data.getPlayerClass() != null && data.getPlayerClass() != PjmPlayerClass.NONE) {
                    data.setPlayerClass(PjmPlayerClass.NONE);
                    // Очищаем инвентарь
                    player.getInventory().clearContent();
                }
                
                // Синхронизируем данные с клиентом
                PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(player.getId(), data), player);
            }

            executeTeamJoinCommands(player, packet.teamId, teamName);

            // Переключаем игрока из режима спектатора в режим выживания
            if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                player.setGameMode(GameType.SURVIVAL);
            }

            // Отправляем успешный результат
            String teamDisplayName = ScoreboardTeamHelper.getTeamName(player);
            PjmNetworking.sendToClient(
                new SyncTeamSelectionResultPacket(true, Component.translatable("wrb.team.success", teamDisplayName).getString()),
                player
            );
        });
    }

    private static void executeTeamJoinCommands(ServerPlayer player, String teamId, String teamName) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        List<String> commands = "team1".equalsIgnoreCase(teamId)
            ? ru.liko.pjmbasemod.common.PjmServerConfig.getTeam1JoinCommands()
            : ru.liko.pjmbasemod.common.PjmServerConfig.getTeam2JoinCommands();
        
        if (commands == null || commands.isEmpty()) {
            return;
        }

        CommandSourceStack source = player.createCommandSourceStack()
            .withPermission(4);

        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String command = raw.trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            command = command
                .replace("%player%", player.getGameProfile().getName())
                .replace("%uuid%", player.getUUID().toString())
                .replace("%team%", teamName)
                .replace("%team_id%", teamId.toLowerCase());

            server.getCommands().performPrefixedCommand(source, command);
        }
    }
}

