package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet for opening team selection menu on client.
 * NeoForge 1.21.1 format.
 */
public record OpenTeamSelectionPacket(
    int team1Count,
    int team2Count,
    String team1Name,
    String team2Name,
    boolean allowBack,
    int balanceThreshold
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenTeamSelectionPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "open_team_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTeamSelectionPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenTeamSelectionPacket::team1Count,
            ByteBufCodecs.VAR_INT, OpenTeamSelectionPacket::team2Count,
            ByteBufCodecs.STRING_UTF8, OpenTeamSelectionPacket::team1Name,
            ByteBufCodecs.STRING_UTF8, OpenTeamSelectionPacket::team2Name,
            ByteBufCodecs.BOOL, OpenTeamSelectionPacket::allowBack,
            ByteBufCodecs.VAR_INT, OpenTeamSelectionPacket::balanceThreshold,
            OpenTeamSelectionPacket::new
        );

    /** Factory method for creating packet from balance info map */
    public static OpenTeamSelectionPacket create(Map<String, Integer> balanceInfo, String team1Name, String team2Name, boolean allowBack, int balanceThreshold) {
        int team1Count = balanceInfo != null ? balanceInfo.getOrDefault("team1", 0) : 0;
        int team2Count = balanceInfo != null ? balanceInfo.getOrDefault("team2", 0) : 0;
        return new OpenTeamSelectionPacket(team1Count, team2Count, team1Name, team2Name, allowBack, balanceThreshold);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTeamSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
                if (proxy != null) {
                    proxy.handleOpenTeamSelection(packet);
                }
            }
        });
    }

    /** Get balance info as map for compatibility */
    public Map<String, Integer> getTeamBalanceInfo() {
        Map<String, Integer> info = new HashMap<>();
        info.put("team1", team1Count);
        info.put("team2", team2Count);
        return info;
    }

    /** Compatibility getter */
    public String getTeam1Name() {
        return team1Name;
    }

    /** Compatibility getter */
    public String getTeam2Name() {
        return team2Name;
    }

    /** Compatibility getter */
    public boolean isAllowBack() {
        return allowBack;
    }

    /** Compatibility getter */
    public int getBalanceThreshold() {
        return balanceThreshold;
    }
}

