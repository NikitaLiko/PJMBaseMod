package ru.liko.pjmbasemod.common.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.map.MapManager;
import ru.liko.pjmbasemod.common.rank.RankManager;
import ru.liko.pjmbasemod.common.util.ScoreboardTeamHelper;

/**
 * NeoForge 1.21.1: Updated event bus annotations
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class RankProgressionEvents {

    private static final int XP_KILL_ENEMY = 100;
    private static final int XP_KILL_FRIENDLY = -500;
    private static final int XP_HEAL_ALLY = 10;
    private static final int XP_COHESION_TICK = 1; // 1 XP per minute near squad leader? Or just passive gain

    /**
     * Полный запрет урона в лобби-дименшоне (любой источник).
     */
    @SubscribeEvent
    public static void onLobbyDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().dimension().equals(MapManager.LOBBY_DIMENSION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        Entity source = event.getSource().getEntity();
        Entity victim = event.getEntity();

        if (source instanceof ServerPlayer killer && victim instanceof ServerPlayer victimPlayer) {
            // Не начисляем XP за убийства в лобби (подстраховка)
            if (killer.level().dimension().equals(MapManager.LOBBY_DIMENSION)
                    || victimPlayer.level().dimension().equals(MapManager.LOBBY_DIMENSION)) {
                return;
            }
            String killerTeam = ScoreboardTeamHelper.getTeamName(killer);
            String victimTeam = ScoreboardTeamHelper.getTeamName(victimPlayer);

            // Регистрируем kill/death в статистике матча
            ru.liko.pjmbasemod.common.match.MatchManager.get().recordKill(killer, victimPlayer);

            // Отправляем событие убийства на бекенд (async)
            ru.liko.pjmbasemod.common.stats.StatsApi.sendSingleEvent(killer, "kill", 100, null); // Пример урона, можно
                                                                                                 // адаптировать
            // Отправляем событие смерти на бекенд (async)
            ru.liko.pjmbasemod.common.stats.StatsApi.sendSingleEvent(victimPlayer, "death", null, 100);

            if (!killerTeam.isEmpty() && !victimTeam.isEmpty()) {
                if (killerTeam.equals(victimTeam)) {
                    // Team Kill
                    RankManager.addPoints(killer, XP_KILL_FRIENDLY, "Огонь по своим");
                } else {
                    // Enemy Kill
                    RankManager.addPoints(killer, XP_KILL_ENEMY, "Убийство врага");
                }
            } else {
                // No team assigned, treat as enemy or neutral kill
                RankManager.addPoints(killer, XP_KILL_ENEMY, "Убийство");
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        Entity victim = event.getEntity();

        if (attacker instanceof ServerPlayer attackerPlayer && victim instanceof ServerPlayer victimPlayer) {
            ru.liko.pjmbasemod.common.match.MatchManager.get().recordDamage(attackerPlayer, victimPlayer);
        }
    }

    // Note: FirstAid might handle healing differently. If it fires LivingHealEvent,
    // this works.
    // If not, we might need to hook into ItemHealing or similar if accessible.
    // For now, standard healing event:
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        // Placeholder for standard healing events if applicable
    }

    // NeoForge 1.21.1: Use PlayerTickEvent.Post instead of
    // TickEvent.PlayerTickEvent with Phase.END
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide
                && event.getEntity().tickCount % 1200 == 0) { // Every minute (20 * 60)

            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                // Не начисляем сплоченность/прочие бонусы в лобби
                if (serverPlayer.level().dimension().equals(MapManager.LOBBY_DIMENSION)) {
                    return;
                }
                // Simple cohesion check: count nearby teammates
                String myTeam = ScoreboardTeamHelper.getTeamName(serverPlayer);
                if (myTeam.isEmpty())
                    return;

                int nearbyTeammates = 0;
                for (Player other : serverPlayer.level().players()) {
                    if (other != serverPlayer && other.distanceToSqr(serverPlayer) < 256.0) { // 16 blocks radius
                        if (myTeam.equals(ScoreboardTeamHelper.getTeamName(other))) {
                            nearbyTeammates++;
                        }
                    }
                }

                if (nearbyTeammates > 0) {
                    RankManager.addPoints(serverPlayer, 5 * nearbyTeammates, "Сплоченность");
                }
            }
        }
    }
}
