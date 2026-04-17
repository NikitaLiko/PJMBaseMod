package ru.liko.pjmbasemod.client.capture;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import ru.liko.pjmbasemod.client.ClientTeamConfig;
import ru.liko.pjmbasemod.common.gamemode.ControlPointSnapshot;

import java.util.List;
import java.util.Locale;

/**
 * Вспомогательные методы для отображения UI захвата.
 */
public final class CaptureUiHelper {
    private static final int TEAM1_COLOR = 0xFF5EAAA8;
    private static final int TEAM2_COLOR = 0xFFE0B74A;
    private static final int NEUTRAL_COLOR = 0xFF9DA5B4;

    private CaptureUiHelper() {}

    public static ControlPointSnapshot findPointUnderPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return null;
        }
        ResourceLocation playerDimension = player.level().dimension().location();
        Vec3 playerPos = player.position();
        List<ControlPointSnapshot> points = ClientCaptureData.getPoints();
        for (ControlPointSnapshot point : points) {
            if (point.dimension() == null || point.min() == null || point.max() == null) {
                continue;
            }
            if (!point.dimension().equals(playerDimension)) {
                continue;
            }
            
            Vec3 min = point.min();
            Vec3 max = point.max();
            
            if (playerPos.x >= min.x && playerPos.x <= max.x &&
                playerPos.y >= min.y && playerPos.y <= max.y &&
                playerPos.z >= min.z && playerPos.z <= max.z) {
                return point;
            }
        }
        return null;
    }

    public static int resolveTeamColor(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return NEUTRAL_COLOR;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Scoreboard scoreboard = mc.level.getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam(teamId);
            if (team != null) {
                ChatFormatting color = team.getColor();
                if (color != null && color.getColor() != null) {
                    return 0xFF000000 | color.getColor();
                }
            }
        }

        return ClientTeamConfig.isTeam2(teamId) ? TEAM2_COLOR : TEAM1_COLOR;
    }

    public static String formatTeamLabel(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return Component.translatable("overlay.wrb.capture.team.none").getString();
        }
        return teamId.toUpperCase(Locale.ROOT);
    }
}

