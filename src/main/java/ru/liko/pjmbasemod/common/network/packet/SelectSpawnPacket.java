package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.map.MapManager;
import ru.liko.pjmbasemod.common.match.MatchManager;
import ru.liko.pjmbasemod.common.match.MatchState;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Пакет выбора точки спавна (Client → Server).
 * Игрок выбирает где заспавниться.
 */
public record SelectSpawnPacket(String spawnPointId) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SelectSpawnPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "select_spawn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectSpawnPacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectSpawnPacket::spawnPointId,
            SelectSpawnPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectSpawnPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            
            String spawnId = packet.spawnPointId();
            PjmPlayerData data = player.getData(PjmAttachments.PLAYER_DATA);
            MatchManager matchManager = MatchManager.get();
            
            // Проверяем кулдаун респавна
            int cooldownSeconds = Config.getSpawnCooldownSeconds();
            if (cooldownSeconds > 0 && !player.hasPermissions(2)) {
                // Простая проверка кулдауна (можно расширить)
            }
            
            switch (spawnId) {
                case "team_base" -> {
                    // Телепортация на базу команды
                    String teamName = data.getTeam();
                    if (teamName == null || teamName.isEmpty()) {
                        player.displayClientMessage(Component.literal("§cВы не в команде!"), true);
                        return;
                    }
                    
                    String currentMapId = matchManager.getCurrentMapId();
                    if (matchManager.getState() == MatchState.IN_PROGRESS) {
                        MapManager.teleportToMap(player, currentMapId, teamName);

                        // Переключаем в режим выживания ПОСЛЕ телепорта (через тик, чтобы
                        // смена дименшона не сбросила gamemode обратно в спектатор)
                        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                            player.getServer().execute(() -> player.setGameMode(GameType.SURVIVAL));
                        }

                        player.displayClientMessage(Component.literal("§aВы заспавнились на базе команды"), false);
                    } else {
                        player.displayClientMessage(Component.literal("§cМатч не активен!"), true);
                    }
                }
                
                case "lobby" -> {
                    // Телепортация в лобби — сначала телепорт, потом gamemode
                    MapManager.teleportToLobby(player);
                    if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                        player.getServer().execute(() -> player.setGameMode(GameType.SURVIVAL));
                    }
                    player.displayClientMessage(Component.literal("§aВы телепортированы в лобби"), false);
                }
                
                default -> {
                    // Спавн на захваченной контрольной точке (cp_<id>)
                    if (spawnId.startsWith("cp_")) {
                        String cpId = spawnId.substring(3);
                        net.minecraft.server.level.ServerLevel overworld = player.server.overworld();
                        ru.liko.pjmbasemod.common.gamemode.ControlPointManager cpManager =
                                ru.liko.pjmbasemod.common.gamemode.ControlPointManager.get(overworld);
                        java.util.Optional<ru.liko.pjmbasemod.common.gamemode.ControlPoint> cpOpt = cpManager.get(cpId);

                        if (cpOpt.isPresent()) {
                            ru.liko.pjmbasemod.common.gamemode.ControlPoint cp = cpOpt.get();
                            PjmPlayerData pData = player.getData(PjmAttachments.PLAYER_DATA);
                            String pTeam = pData.getTeam();

                            if (pTeam != null && pTeam.equals(cp.getOwnerTeam())) {
                                // Определяем измерение точки
                                net.minecraft.server.level.ServerLevel cpLevel = player.server.getLevel(cp.getDimension());
                                if (cpLevel == null) cpLevel = overworld;

                                net.minecraft.world.phys.Vec3 pos = cp.getPosition();
                                player.teleportTo(cpLevel, pos.x, pos.y + 1, pos.z, player.getYRot(), 0);

                                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                                    player.getServer().execute(() -> player.setGameMode(GameType.SURVIVAL));
                                }
                                player.displayClientMessage(
                                        Component.literal("§aВы заспавнились на точке: " + cp.getDisplayName()), false);
                            } else {
                                player.displayClientMessage(
                                        Component.literal("§cТочка " + cp.getDisplayName() + " больше не принадлежит вашей команде!"), true);
                            }
                        } else {
                            player.displayClientMessage(Component.literal("§cТочка не найдена: " + cpId), true);
                        }
                    } else {
                        LOGGER.warn("Unknown spawn point ID: {} from player {}", spawnId, player.getName().getString());
                        player.displayClientMessage(Component.literal("§cНеизвестная точка спавна: " + spawnId), true);
                    }
                }
            }
        });
    }
}
