package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.event.PjmCommonEvents;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.permission.PjmPermissions;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.util.TeamBalanceHelper;
import ru.liko.pjmbasemod.common.zone.ClassSelectionZoneManager;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Packet for requesting class selection menu.
 * NeoForge 1.21.1 format.
 */
public record OpenClassSelectionPacket() implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<OpenClassSelectionPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "open_class_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenClassSelectionPacket> STREAM_CODEC =
        StreamCodec.unit(new OpenClassSelectionPacket());

    /** Singleton instance for empty packet */
    public static final OpenClassSelectionPacket INSTANCE = new OpenClassSelectionPacket();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenClassSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                LOGGER.error("OpenClassSelectionPacket: Player is null or not ServerPlayer!");
                return;
            }
            handleForPlayer(player);
        });
    }

    /**
     * Handle class selection for a specific player (called from packet handler or command)
     * @param player the server player to open class selection for
     */
    public static void handleForPlayer(ServerPlayer player) {
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[WRB DEBUG] OpenClassSelectionPacket received from player: {}", player.getName().getString());
        }

        // Check if player is in class selection zone
        ClassSelectionZoneManager zoneManager = PjmCommonEvents.getZoneManager();
        if (zoneManager == null) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.error("[WRB DEBUG] Zone manager is NULL!");
            }
            player.displayClientMessage(Component.translatable("wrb.class.error.no_manager"), true);
            return;
        }

        boolean inZone = zoneManager.isInAnyZone(player.position(), player.level().dimension());

        if (!inZone) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.warn("[WRB DEBUG] Player {} is not in any class selection zone!", player.getName().getString());
            }
            player.displayClientMessage(Component.translatable("wrb.class.error.not_in_zone"), true);
            return;
        }

        // Check if player has a team
        if (!TeamBalanceHelper.hasTeam(player)) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("Player {} has no team - opening team selection", player.getName().getString());
            }
            
            Map<String, Integer> balanceInfo = TeamBalanceHelper.getTeamBalanceInfo(player.getServer());
            
            PjmNetworking.sendToClient(
                OpenTeamSelectionPacket.create(
                    balanceInfo,
                    Config.getTeam1Name(),
                    Config.getTeam2Name(),
                    true,
                    Config.getTeamBalanceThreshold()
                ),
                player
            );
            
            player.displayClientMessage(Component.translatable("wrb.team.error.no_team"), true);
            return;
        }

        // Get player's team
        net.minecraft.world.scores.Team playerTeam = player.getTeam();
        String playerTeamName = playerTeam != null ? playerTeam.getName() : Config.getTeam1Name();

        Map<String, Integer> classPlayerCounts = new HashMap<>();
        Map<String, Integer> classLimits = new HashMap<>();
        
        // Check permissions
        boolean hasSsoPermission = PermissionAPI.getPermission(player, PjmPermissions.CLASS_SSO);
        boolean hasSpnPermission = PermissionAPI.getPermission(player, PjmPermissions.CLASS_SPN);
        
        String team1Name = Config.getTeam1Name();
        boolean isTeam1 = playerTeamName.equalsIgnoreCase(team1Name);
        
        Set<String> lockedClasses = new HashSet<>();
        
        for (PjmPlayerClass playerClass : PjmPlayerClass.values()) {
            if (playerClass.isSelectable()) {
                // Check team restriction
                if (playerClass.isTeam1Only() && !isTeam1) {
                    lockedClasses.add(playerClass.getId());
                } else if (playerClass.requiresPermission()) {
                    boolean hasPermission = playerClass == PjmPlayerClass.SPN ? hasSpnPermission : hasSsoPermission;
                    if (!hasPermission) {
                        lockedClasses.add(playerClass.getId());
                    }
                }
                
                String classId = playerClass.getId();
                
                // Count players using Data Attachments
                long count = player.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> !PjmPermissions.isAdmin(p))
                    .filter(p -> {
                        PjmPlayerData data = p.getData(PjmAttachments.PLAYER_DATA);
                        return data.getPlayerClass().equals(playerClass);
                    })
                    .count();
                
                classPlayerCounts.put(classId, (int) count);
                classLimits.put(classId, Config.getClassLimit(classId));
            }
        }

        // Send class selection data to client
        PjmNetworking.sendToClient(
            SyncClassSelectionDataPacket.create(classLimits, classPlayerCounts, playerTeamName, lockedClasses),
            player
        );
    }
}

