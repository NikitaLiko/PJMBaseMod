package ru.liko.pjmbasemod.common.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * Utility methods for reading vanilla /team scoreboard data.
 */
public final class ScoreboardTeamHelper {
    public static final String NO_TEAM_PLACEHOLDER = "Не назначена";

    private ScoreboardTeamHelper() {}

    private static String resolveTeamName(Team team) {
        if (team == null) {
            return NO_TEAM_PLACEHOLDER;
        }
        String displayName = resolveDisplayComponent(team).getString().trim();
        String teamName = team.getName();
        return displayName.isEmpty() ? teamName : displayName;
    }

    private static Component formatTeamComponent(Team team) {
        if (team != null) {
            String teamName = team.getName();
            Component teamDisplay = resolveDisplayComponent(team);
            return Component.empty()
                .append(Component.literal("§bКоманда: "))
                .append(teamDisplay)
                .append(Component.literal(" (§7" + teamName + "§f)"));
        }
        return Component.literal("§7Команда: §7Не назначена");
    }

    public static String getTeamName(ServerPlayer player) {
        Scoreboard scoreboard = player.server.getScoreboard();
        Team team = scoreboard.getPlayersTeam(player.getScoreboardName());
        return resolveTeamName(team);
    }

    public static String getTeamName(Player player) {
        Team team = player.getTeam();
        return resolveTeamName(team);
    }

    public static Component getTeamInfoComponent(ServerPlayer player) {
        Scoreboard scoreboard = player.server.getScoreboard();
        Team team = scoreboard.getPlayersTeam(player.getScoreboardName());
        return formatTeamComponent(team);
    }

    public static Component getTeamInfoComponent(Player player) {
        return formatTeamComponent(player.getTeam());
    }

    private static Component resolveDisplayComponent(Team team) {
        if (team == null) {
            return Component.literal(NO_TEAM_PLACEHOLDER);
        }

        if (team instanceof PlayerTeam playerTeam) {
            return playerTeam.getDisplayName().copy();
        }

        MutableComponent nameComponent = Component.literal(team.getName());
        ChatFormatting color = team.getColor();
        if (color != null) {
            nameComponent = nameComponent.withStyle(color);
        }
        return nameComponent;
    }
}
