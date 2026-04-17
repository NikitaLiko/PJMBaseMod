package ru.liko.pjmbasemod.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.command.subcommands.*;

/**
 * Корневая команда /pjm с иерархической структурой подкоманд.
 * Согласно PORTING_GUIDE_1.21.1_NEOFORGE.md
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class PjmCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> pjm = Commands.literal("pjm")
                .executes(ctx -> showHelp(ctx.getSource()))
                // /pjm reload
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> reloadConfig(ctx.getSource())))
                // /pjm help
                .then(Commands.literal("help")
                        .executes(ctx -> showHelp(ctx.getSource())))
                // Подкоманды
                .then(PlayerCommand.register())
                .then(TeamCommand.register())
                .then(ZoneCommand.register())
                .then(ControlPointCommand.register())
                .then(VehicleCommand.register())
                .then(TimerCommand.register())
                .then(KitCommand.register())
                .then(ConfigCommand.register())
                .then(DebugCommand.register())
                .then(MatchCommand.register())
                .then(DimensionCommand.register())

                // Help text
                ;

        dispatcher.register(pjm);

        // Регистрируем также /chat команду
        ChatCommand.register(dispatcher);
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Project Minecraft Commands ==="), false);
        source.sendSuccess(() -> Component.literal("§e/pjm reload §7- Перезагрузить конфигурацию"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm player <player> §7- Управление игроками"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm team §7- Управление командами"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm zone §7- Зоны выбора классов"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm cp §7- Контрольные точки"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm vehicle §7- Управление техникой"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm timer §7- Таймеры"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm kit §7- Управление китами"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm config §7- Конфигурация"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm debug §7- Отладка (OP)"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm match §7- Администрирование матчей"), false);
        source.sendSuccess(() -> Component.literal("§e/pjm dimension §7- Управление измерениями"), false);
        source.sendSuccess(() -> Component.literal("§e/chat <mode> §7- Смена режима чата"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadConfig(CommandSourceStack source) {
        try {
            Config.reload();
            source.sendSuccess(() -> Component.translatable("wrb.config.reload.success"), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("wrb.config.reload.fail"));
            return 0;
        }
    }
}
