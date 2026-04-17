package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;


/**
 * Пакет для синхронизации конфигурации команд с клиентом.
 * Отправляется сервером клиенту при подключении для синхронизации названий команд.
 */
public record SyncTeamConfigPacket(String team1Id, String team1DisplayName, String team2Id, String team2DisplayName, int balanceThreshold) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncTeamConfigPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_team_config"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTeamConfigPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SyncTeamConfigPacket::team1Id,
        ByteBufCodecs.STRING_UTF8, SyncTeamConfigPacket::team1DisplayName,
        ByteBufCodecs.STRING_UTF8, SyncTeamConfigPacket::team2Id,
        ByteBufCodecs.STRING_UTF8, SyncTeamConfigPacket::team2DisplayName,
        ByteBufCodecs.INT, SyncTeamConfigPacket::balanceThreshold,
        SyncTeamConfigPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTeamConfigPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncTeamConfig(packet.team1Id(), packet.team1DisplayName(),
                    packet.team2Id(), packet.team2DisplayName(), packet.balanceThreshold());
            }
        });
    }

    public String getTeam1Id() {
        return team1Id;
    }
    
    public String getTeam1DisplayName() {
        return team1DisplayName;
    }

    public String getTeam2Id() {
        return team2Id;
    }
    
    public String getTeam2DisplayName() {
        return team2DisplayName;
    }
    
    public int getBalanceThreshold() {
        return balanceThreshold;
    }
}

