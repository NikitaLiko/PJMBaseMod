package ru.liko.pjmbasemod.common.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.common.KitDefinition;
import ru.liko.pjmbasemod.common.KitsConfig;
import ru.liko.pjmbasemod.common.network.packet.SelectClassPacket;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * /pjm kit - Управление китами
 */
public class KitCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("kit")
            .requires(src -> src.hasPermission(2))
            // /pjm kit list [class] [team]
            .then(Commands.literal("list")
                .executes(ctx -> listKits(ctx.getSource(), null, null))
                .then(Commands.argument("class", StringArgumentType.word())
                    .executes(ctx -> listKits(ctx.getSource(), StringArgumentType.getString(ctx, "class"), null))
                    .then(Commands.argument("team", StringArgumentType.word())
                        .executes(ctx -> listKits(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "class"),
                            StringArgumentType.getString(ctx, "team")
                        )))))
            // /pjm kit reload
            .then(Commands.literal("reload")
                .executes(ctx -> reloadKits(ctx.getSource())))
            // /pjm kit give <player> <kitId>
            .then(Commands.literal("give")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("kitId", StringArgumentType.word())
                        .executes(ctx -> giveKit(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "target"),
                            StringArgumentType.getString(ctx, "kitId")
                        )))));
    }

    private static int listKits(CommandSourceStack source, String classFilter, String teamFilter) {
        KitsConfig.init(); // Ensure initialized
        Map<String, Map<String, List<KitDefinition>>> allKits = KitsConfig.getAllKitsData();
        
        if (allKits.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§cКиты не загружены. Используйте §e/pjm kit reload"), false);
            return Command.SINGLE_SUCCESS;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Список китов ==="), false);
        
        if (classFilter != null) {
            source.sendSuccess(() -> Component.literal("§7Фильтр: класс=§e" + classFilter + 
                (teamFilter != null ? "§7, команда=§e" + teamFilter : "")), false);
        }
        
        int totalKits = 0;
        
        Set<String> classIds = classFilter != null ? 
            (allKits.containsKey(classFilter) ? Set.of(classFilter) : Set.of()) : 
            allKits.keySet();
        
        for (String classId : classIds) {
            Map<String, List<KitDefinition>> teamKits = allKits.get(classId);
            if (teamKits == null) continue;
            
            // Получаем отображаемое имя класса
            String classDisplayName = PjmPlayerClass.fromId(classId)
                .map(pc -> pc.getId())
                .orElse(classId);
            
            Set<String> teamIds = teamFilter != null ?
                (teamKits.containsKey(teamFilter) ? Set.of(teamFilter) : Set.of()) :
                teamKits.keySet();
            
            for (String teamId : teamIds) {
                List<KitDefinition> kits = teamKits.get(teamId);
                if (kits == null || kits.isEmpty()) continue;
                
                String header = "§e" + classDisplayName + " §7[" + teamId + "]§f:";
                source.sendSuccess(() -> Component.literal(header), false);
                
                for (KitDefinition kit : kits) {
                    totalKits++;
                    String kitInfo = "  §a" + kit.getId() + " §7- §f" + kit.getDisplayName() + 
                        " §7(ранг: §e" + kit.getMinRank().getDisplayName() + "§7, предметов: §e" + kit.getItems().size() + "§7)";
                    source.sendSuccess(() -> Component.literal(kitInfo), false);
                }
            }
        }
        
        if (totalKits == 0) {
            if (classFilter != null) {
                source.sendSuccess(() -> Component.literal("§7Киты не найдены для указанных фильтров"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§7Киты не найдены"), false);
            }
        } else {
            int finalTotal = totalKits;
            source.sendSuccess(() -> Component.literal("§7Всего китов: §f" + finalTotal), false);
        }
        
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadKits(CommandSourceStack source) {
        boolean success = KitsConfig.reload();
        if (success) {
            Map<String, Map<String, List<KitDefinition>>> allKits = KitsConfig.getAllKitsData();
            int totalKits = allKits.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToInt(List::size)
                .sum();
            source.sendSuccess(() -> Component.literal("§aКонфигурация китов перезагружена! Загружено китов: §e" + totalKits), true);
        } else {
            source.sendFailure(Component.literal("§cОшибка перезагрузки конфигурации китов!"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int giveKit(CommandSourceStack source, ServerPlayer target, String kitId) {
        PjmPlayerData data = target.getData(PjmAttachments.PLAYER_DATA);
        String classId = data.getPlayerClass().getId();
        
        // Определяем команду игрока
        net.minecraft.world.scores.Team playerTeam = target.getTeam();
        String teamName = playerTeam != null ? playerTeam.getName() : Config.getTeam1Name();
        
        // Если класс не выбран — пытаемся найти кит во всех классах
        if (classId.equals("none")) {
            // Ищем кит по ID во всех классах
            Map<String, Map<String, List<KitDefinition>>> allKits = KitsConfig.getAllKitsData();
            for (var classEntry : allKits.entrySet()) {
                Map<String, List<KitDefinition>> teamKits = classEntry.getValue();
                for (var teamEntry : teamKits.entrySet()) {
                    for (KitDefinition kit : teamEntry.getValue()) {
                        if (kit.getId().equals(kitId)) {
                            // Нашли кит — выдаём
                            SelectClassPacket.giveClassItems(target, kit.getItems(), true);
                            String foundClass = classEntry.getKey();
                            source.sendSuccess(() -> Component.literal(
                                "§aКит §e" + kit.getDisplayName() + " §a(§7" + foundClass + "/" + teamEntry.getKey() + "§a) выдан игроку §e" + target.getName().getString()), true);
                            target.displayClientMessage(
                                Component.literal("§aВам выдан кит: §e" + kit.getDisplayName()), false);
                            return Command.SINGLE_SUCCESS;
                        }
                    }
                }
            }
            
            source.sendFailure(Component.literal("§cКит '" + kitId + "' не найден! У игрока не выбран класс. Укажите корректный kitId."));
            return 0;
        }
        
        // Ищем кит для текущего класса и команды игрока
        Optional<KitDefinition> kitOpt = KitsConfig.getKit(classId, teamName, kitId);
        
        if (kitOpt.isEmpty()) {
            // Пробуем найти в другой команде
            String otherTeam = teamName.equalsIgnoreCase(Config.getTeam1Name()) ? Config.getTeam2Name() : Config.getTeam1Name();
            kitOpt = KitsConfig.getKit(classId, otherTeam, kitId);
            
            if (kitOpt.isEmpty()) {
                source.sendFailure(Component.literal("§cКит '" + kitId + "' не найден для класса " + classId + "!"));
                return 0;
            }
        }
        
        KitDefinition kit = kitOpt.get();
        
        // Выдаём предметы (с очисткой инвентаря)
        SelectClassPacket.giveClassItems(target, kit.getItems(), true);
        
        source.sendSuccess(() -> Component.literal(
            "§aКит §e" + kit.getDisplayName() + " §a(" + kitId + ") выдан игроку §e" + target.getName().getString()), true);
        target.displayClientMessage(
            Component.literal("§aВам выдан кит: §e" + kit.getDisplayName()), false);
        
        return Command.SINGLE_SUCCESS;
    }
}
