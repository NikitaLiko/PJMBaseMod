package ru.liko.pjmbasemod.common.gamemode;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import ru.liko.pjmbasemod.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер режима захвата точек.
 */
public final class CaptureGameModeController {

    private CaptureGameModeController() {}

    public static void tick(ServerLevel level) {
        tickCaptureLogic(level);
        
        // Синхронизируем контрольные точки для всех измерений, не только OVERWORLD
        if (level.getServer().getTickCount() % 20 == 0) {
            CaptureNetwork.syncAll(level);
        }
    }
    
    private static void tickCaptureLogic(ServerLevel level) {
        if (!Config.isCaptureSystemEnabled()) return;
        
        ControlPointManager manager = ControlPointManager.get(level);
        if (manager.isEmpty()) return;
        
        boolean anyChanged = false;
        
        for (ControlPoint point : manager.getAll()) {
            if (!point.getDimension().equals(level.dimension())) continue;
            
            if (tickSinglePoint(level, point)) {
                anyChanged = true;
            }
        }
        
        if (anyChanged) {
            manager.markDirty();
        }
    }
    
    private static boolean tickSinglePoint(ServerLevel level, ControlPoint point) {
        Map<String, Integer> teamCounts = new HashMap<>();
        
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;
            
            Vec3 playerPos = player.position();
            if (!point.isInside(playerPos)) continue;
            
            String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
            if (teamName.isEmpty()) continue;
            
            teamCounts.merge(teamName, 1, Integer::sum);
        }
        
        String owner = point.getOwnerTeam();
        String capturing = point.getCapturingTeam();
        double progress = point.getCaptureProgress();
        int minPlayers = point.getMinPlayersToCap();
        
        // Check if point is neutral (no owner)
        boolean isNeutral = owner == null || owner.isEmpty();
        
        // Find dominant team
        String dominantTeam = null;
        int dominantCount = 0;
        int ownerCount = isNeutral ? 0 : teamCounts.getOrDefault(owner, 0);
        
        // For neutral points, find the team with most players
        // For owned points, find the team with most players excluding owner
        for (Map.Entry<String, Integer> entry : teamCounts.entrySet()) {
            String team = entry.getKey();
            int count = entry.getValue();
            
            // Skip owner team for owned points (they are defending, not capturing)
            if (!isNeutral && team.equals(owner)) continue;
            
            if (count > dominantCount) {
                dominantTeam = team;
                dominantCount = count;
            }
        }
        
        // Calculate capture time
        int captureTimeSeconds = point.getCaptureTimeSeconds();
        if (captureTimeSeconds <= 0) {
            captureTimeSeconds = Config.getCaptureTimeSeconds();
        }
        double deltaPerTick = 1.0 / (captureTimeSeconds * 20.0);
        
        boolean changed = false;
        
        // Case 1: Contested - owner is defending (only for owned points)
        if (!isNeutral && dominantTeam != null && ownerCount > 0) {
            // Point is contested - no progress change
            // Could add "contested" visual feedback here
            return false;
        }
        
        // Case 2: Attackers are capturing (or capturing neutral point)
        if (dominantTeam != null && dominantCount >= minPlayers) {
            // Chain capture check: team must own at least one neighbor (only for owned points)
            // Neutral points can be captured by any team without chain requirement
            if (!isNeutral && !canTeamCapture(level, point, dominantTeam)) {
                return false; // Cannot capture - no adjacent owned points
            }
            // Check if we're switching capturing team
            if (!dominantTeam.equals(capturing)) {
                // If there was progress by another team, first decay that
                if (!capturing.isEmpty() && progress > 0) {
                    progress -= deltaPerTick;
                    if (progress <= 0) {
                        progress = 0;
                        point.setCapturingTeam(dominantTeam);
                    }
                } else {
                    point.setCapturingTeam(dominantTeam);
                }
                changed = true;
            } else {
                // Same team continues capture
                progress += deltaPerTick;
                changed = true;
                
                // Capture complete!
                if (progress >= 1.0) {
                    progress = 0.0;
                    String oldOwner = point.getOwnerTeam();
                    point.setOwnerTeam(dominantTeam);
                    point.setCapturingTeam("");
                    
                    // Announce capture
                    announceCaptured(level, point, dominantTeam, oldOwner);
                    
                    // Award XP to capturers
                    awardCaptureXp(level, point, dominantTeam);

                    // Записываем в match-статистику
                    ru.liko.pjmbasemod.common.match.MatchManager matchMgr =
                            ru.liko.pjmbasemod.common.match.MatchManager.get();
                    for (ServerPlayer p : level.players()) {
                        if (p.isSpectator() || p.isCreative()) continue;
                        String pTeam = p.getTeam() != null ? p.getTeam().getName() : "";
                        if (dominantTeam.equals(pTeam) && point.isInside(p.position())) {
                            matchMgr.getStatsFor(p.getUUID()).addCapturePoints(50);
                        }
                    }
                }
            }
            point.setCaptureProgress(progress);
            return changed;
        }
        
        // Case 3: No attackers - decay capture progress
        if (progress > 0) {
            progress -= deltaPerTick * 0.5; // Decay at half speed
            if (progress <= 0) {
                progress = 0;
                point.setCapturingTeam("");
            }
            point.setCaptureProgress(progress);
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a team can capture a point based on chain/frontline logic.
     * A team can capture if:
     * - The point has no neighbors (standalone point)
     * - The team owns at least one neighboring point
     */
    private static boolean canTeamCapture(ServerLevel level, ControlPoint point, String team) {
        // If point has no neighbors, it's always capturable (e.g., starting point)
        if (point.getNeighbors().isEmpty()) {
            return true;
        }
        
        ControlPointManager manager = ControlPointManager.get(level);
        
        // Check if team owns at least one neighbor
        for (String neighborId : point.getNeighbors()) {
            Optional<ControlPoint> neighbor = manager.get(neighborId);
            if (neighbor.isPresent() && team.equals(neighbor.get().getOwnerTeam())) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void announceCaptured(ServerLevel level, ControlPoint point, String newOwner, String oldOwner) {
        Component message = Component.literal("⚑ ")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.literal(point.getDisplayName())
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" захвачена командой ")
                .withStyle(ChatFormatting.GRAY))
            .append(Component.literal(newOwner.toUpperCase())
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.literal(" !")
                .withStyle(ChatFormatting.GOLD));
        
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.displayClientMessage(message, false);
        }
    }

    private static void awardCaptureXp(ServerLevel level, ControlPoint point, String capturingTeam) {
        int xpReward = 300; // Points for capture
        
        for (ServerPlayer player : level.players()) {
             if (player.isSpectator() || player.isCreative()) continue;
             
             // Check if player is on capturing team AND inside the zone
             String playerTeam = player.getTeam() != null ? player.getTeam().getName() : "";
             if (capturingTeam.equals(playerTeam) && point.isInside(player.position())) {
                 ru.liko.pjmbasemod.common.rank.RankManager.addPoints(player, xpReward, "Захват точки");
             }
        }
    }
}

