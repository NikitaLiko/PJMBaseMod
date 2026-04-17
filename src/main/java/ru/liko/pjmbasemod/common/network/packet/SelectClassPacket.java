package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Config;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.KitsConfig;
import ru.liko.pjmbasemod.common.KitDefinition;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.permission.PjmPermissions;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmPlayerDataProvider;
import ru.liko.pjmbasemod.common.player.PjmRank;
import ru.liko.pjmbasemod.common.util.ItemParser;
import ru.liko.pjmbasemod.common.util.TeamBalanceHelper;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Пакет для выбора класса игроком.
 * Отправляется клиентом на сервер при выборе класса из меню.
 * Updated to support Ranked Kit Variations.
 * NeoForge 1.21.1: record implementing CustomPacketPayload.
 */
public record SelectClassPacket(String classId, String kitId) implements CustomPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SelectClassPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "select_class"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectClassPacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectClassPacket::classId,
            ByteBufCodecs.STRING_UTF8, SelectClassPacket::kitId,
            SelectClassPacket::new
        );

    // Constructor with default kit
    public SelectClassPacket(String classId) {
        this(classId, "default");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectClassPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }

            // Валидация класса
            PjmPlayerClass selectedClass = PjmPlayerClass.fromIdOrDefault(packet.classId);
            if (!selectedClass.isSelectable()) {
                player.displayClientMessage(
                    Component.translatable("wrb.class.error.invalid_class"),
                    true
                );
                return;
            }
            
            // Проверка разрешения для классов, требующих специальных прав (SSO, SPN)
            if (selectedClass.requiresPermission()) {
                // Выбираем правильный пермишн в зависимости от класса
                net.neoforged.neoforge.server.permission.nodes.PermissionNode<Boolean> permissionNode = 
                    selectedClass == PjmPlayerClass.SPN ? PjmPermissions.CLASS_SPN : PjmPermissions.CLASS_SSO;
                boolean hasPermission = PermissionAPI.getPermission(player, permissionNode);
                LOGGER.debug("Player {} attempting to select class {} - permission check: {}", 
                    player.getName().getString(), selectedClass.getId(), hasPermission);
                
                if (!hasPermission) {
                    LOGGER.warn("Player {} denied access to class {} - no permission", 
                        player.getName().getString(), selectedClass.getId());
                    player.displayClientMessage(
                        Component.translatable("wrb.class.error.no_permission", selectedClass.getDisplayName()),
                        true
                    );
                    return;
                }
            }

            // Проверяем, назначена ли команда игроку
            // Игрок без команды не может выбрать класс
            if (!TeamBalanceHelper.hasTeam(player)) {
                player.displayClientMessage(
                    Component.translatable("wrb.team.error.no_team"),
                    true
                );
                return;
            }

            // Получаем команду игрока
            net.minecraft.world.scores.Team playerTeam = player.getTeam();
            String playerTeamName = playerTeam != null ? playerTeam.getName() : Config.getTeam1Name();

            // Проверяем ограничение по команде (SPN только для Team1)
            if (selectedClass.isTeam1Only()) {
                String team1Name = Config.getTeam1Name();
                if (!playerTeamName.equalsIgnoreCase(team1Name)) {
                    org.slf4j.Logger logger = com.mojang.logging.LogUtils.getLogger();
                    logger.warn("Player {} denied access to class {} - team1 only", 
                        player.getName().getString(), selectedClass.getId());
                    player.displayClientMessage(
                        Component.translatable("wrb.class.error.team1_only", selectedClass.getDisplayName()),
                        true
                    );
                    return;
                }
            }

            // Validate Kit and Rank
            Optional<KitDefinition> kitOpt = KitsConfig.getKit(selectedClass.getId(), playerTeamName, packet.kitId);
            if (kitOpt.isEmpty()) {
                // If specific kit not found, try default
                kitOpt = KitsConfig.getKit(selectedClass.getId(), playerTeamName, "default");
                if (kitOpt.isEmpty()) {
                    // Try first available if default missing
                    List<KitDefinition> allKits = KitsConfig.getKits(selectedClass.getId(), playerTeamName);
                    if (!allKits.isEmpty()) {
                        kitOpt = Optional.of(allKits.get(0));
                    }
                }
            }
            
            if (kitOpt.isEmpty()) {
                player.displayClientMessage(Component.translatable("wrb.class.error.no_kit_found"), true);
                return;
            }
            
            KitDefinition kit = kitOpt.get();
            
            // Check Rank Requirement
            { PjmPlayerData data = PjmPlayerDataProvider.get(player);
                PjmRank playerRank = data.getRank();
                if (playerRank.ordinal() < kit.getMinRank().ordinal() && !PjmPermissions.isAdmin(player)) {
                    player.displayClientMessage(
                        Component.translatable("wrb.class.error.rank_too_low", kit.getMinRank().getDisplayName()),
                        true
                    );
                    return;
                }

                // Проверяем кулдаун на взятие кита только для обычных игроков (не OP)
                if (!player.hasPermissions(1)) {
                    int cooldownSeconds = Config.getKitCooldownSeconds();
                    if (!data.canTakeKit(cooldownSeconds)) {
                        int remainingSeconds = data.getRemainingCooldownSeconds(cooldownSeconds);
                        int minutes = remainingSeconds / 60;
                        int seconds = remainingSeconds % 60;
                        String timeStr;
                        if (minutes > 0) {
                            timeStr = String.format("%d мин %d сек", minutes, seconds);
                        } else {
                            timeStr = String.format("%d сек", seconds);
                        }
                        player.displayClientMessage(
                            Component.translatable("wrb.class.error.cooldown", timeStr),
                            true
                        );
                        return;
                    }
                    // Обновляем время последнего взятия кита для обычных игроков
                    data.setLastKitTime(System.currentTimeMillis());
                }
                
                data.setPlayerClass(selectedClass);
                data.setSelectedKitId(kit.getId()); // Сохраняем выбранный кит

                // Синхронизируем с клиентом
                PjmNetworking.sendToClient(SyncPjmDataPacket.fromPlayerData(player.getId(), data), player);

                // Переключаем игрока из режима спектатора в режим выживания
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    player.setGameMode(GameType.SURVIVAL);
                }

                // Выдаём предметы класса для команды игрока (using specific kit items)
                giveClassItems(player, kit.getItems(), true);

                // Сообщение об успехе
                player.displayClientMessage(
                    Component.translatable("wrb.class.success_kit", selectedClass.getDisplayName(), kit.getDisplayName()), 
                    false
                );
            }
        });
    }

    /**
     * Выдаёт игроку предметы
     * @param clearInventory если true, инвентарь будет полностью очищен перед выдачей предметов
     */
    public static void giveClassItems(ServerPlayer player, List<String> itemStrings, boolean clearInventory) {
        // Очищаем инвентарь игрока, если требуется
        if (clearInventory) {
            player.getInventory().clearContent();
            // Очищаем слоты Curios
            ru.liko.pjmbasemod.common.util.CuriosHelper.clearAllCuriosSlots(player);
        }

        for (String itemStr : itemStrings) {
            // Используем парсер со слотами для правильного распределения предметов
            Optional<ItemParser.ParsedItem> parsedOpt = ItemParser.parseItemStackWithSlot(itemStr);
            
            if (parsedOpt.isPresent()) {
                ItemParser.ParsedItem parsed = parsedOpt.get();
                ItemStack stack = parsed.stack;
                
                // Распределяем предметы по слотам на основе сохраненной информации
                if ("armor".equals(parsed.slotType) && parsed.slotIndex >= 0 && parsed.slotIndex < 4) {
                    // Броня в указанный слот (0=ботинки, 1=поножи, 2=нагрудник, 3=шлем)
                    player.getInventory().armor.set(parsed.slotIndex, stack);
                } else if ("offhand".equals(parsed.slotType) && parsed.slotIndex == 0) {
                    // Предмет в offhand
                    player.getInventory().offhand.set(0, stack);
                } else if ("slot".equals(parsed.slotType) && parsed.slotIndex >= 0 && parsed.slotIndex < 36) {
                    // Предмет в указанный слот инвентаря (0-35)
                    player.getInventory().items.set(parsed.slotIndex, stack);
                } else if ("curios".equals(parsed.slotType) && parsed.curiosSlotType != null) {
                    // Предмет в слот Curios
                    ru.liko.pjmbasemod.common.util.CuriosHelper.setCuriosStack(
                        player, parsed.curiosSlotType, parsed.slotIndex, stack
                    );
                } else {
                    // Обратная совместимость: старый формат без слотов
                    net.minecraft.world.item.Item item = stack.getItem();
                    
                    // Проверяем, является ли предмет броней, и автоматически одеваем
                    if (item instanceof net.minecraft.world.item.ArmorItem armorItem) {
                        switch (armorItem.getType()) {
                            case HELMET:
                                player.getInventory().armor.set(3, stack);
                                break;
                            case CHESTPLATE:
                                player.getInventory().armor.set(2, stack);
                                break;
                            case LEGGINGS:
                                player.getInventory().armor.set(1, stack);
                                break;
                            case BOOTS:
                                player.getInventory().armor.set(0, stack);
                                break;
                        }
                    } else {
                        // Если не броня, добавляем в обычный инвентарь
                        player.getInventory().add(stack);
                    }
                }
            }
            // Если предмет не найден, ItemParser уже залогировал предупреждение
        }
    }

    /**
     * Пополняет только расходные предметы (боеприпасы, еда) без перевыдачи оружия и брони
     * @param player Игрок
     * @param playerClass Класс игрока
     * @param teamName Команда игрока
     */
    public static void refillConsumables(ServerPlayer player, PjmPlayerClass playerClass, String teamName) {
        // Получаем строки предметов из конфига для команды игрока
        List<String> itemStrings = Config.getClassItemStrings(playerClass.getId(), teamName);
        
        int totalRefilled = 0;
        boolean anyRefilled = false;
        
        for (String itemStr : itemStrings) {
            // Используем парсер со слотами для правильного распределения предметов
            Optional<ItemParser.ParsedItem> parsedOpt = ItemParser.parseItemStackWithSlot(itemStr);
            
            if (parsedOpt.isPresent()) {
                ItemParser.ParsedItem parsed = parsedOpt.get();
                ItemStack requiredStack = parsed.stack;
                net.minecraft.world.item.Item item = requiredStack.getItem();
                
                // Пропускаем броню - её не пополняем
                if ("armor".equals(parsed.slotType)) {
                    continue;
                }
                
                // Пропускаем предметы в слотах Curios при пополнении (они обычно не расходуются)
                if ("curios".equals(parsed.slotType)) {
                    continue;
                }
                
                // Пропускаем оружие в основных слотах (slot:0, slot:1 - обычно оружие)
                // Но пополняем расходники в любых слотах
                boolean isWeaponOrTool = item instanceof net.minecraft.world.item.SwordItem ||
                                        item instanceof net.minecraft.world.item.AxeItem ||
                                        item instanceof net.minecraft.world.item.PickaxeItem ||
                                        item instanceof net.minecraft.world.item.ShovelItem ||
                                        item instanceof net.minecraft.world.item.HoeItem ||
                                        item instanceof net.minecraft.world.item.BowItem ||
                                        item instanceof net.minecraft.world.item.CrossbowItem ||
                                        item instanceof net.minecraft.world.item.ProjectileWeaponItem;
                
                // Пропускаем оружие и инструменты (если они в первых двух слотах - обычно это основной и второй слот оружия)
                if (isWeaponOrTool && "slot".equals(parsed.slotType) && parsed.slotIndex < 2) {
                    continue;
                }
                
                // Для остальных предметов (расходники) пополняем количество
                int requiredCount = requiredStack.getCount();
                int currentCount = 0;
                
                // Ищем предмет в инвентаре игрока (включая основной инвентарь и offhand)
                // Используем сравнение по предмету и NBT для корректной работы с модами
                for (int i = 0; i < player.getInventory().items.size(); i++) {
                    ItemStack stack = player.getInventory().items.get(i);
                    if (!stack.isEmpty() && stack.getItem() == item) {
                        // В 1.21.1 используем Data Components вместо NBT
                        if (ItemStack.isSameItemSameComponents(requiredStack, stack)) {
                            currentCount += stack.getCount();
                        } else if (ItemStack.isSameItem(requiredStack, stack)) {
                            // Одинаковый предмет, но разные компоненты - всё равно считаем
                            currentCount += stack.getCount();
                        }
                    }
                }
                
                // Проверяем offhand
                ItemStack offhandStack = player.getInventory().offhand.get(0);
                if (!offhandStack.isEmpty() && offhandStack.getItem() == item) {
                    if (ItemStack.isSameItemSameComponents(requiredStack, offhandStack) || ItemStack.isSameItem(requiredStack, offhandStack)) {
                        currentCount += offhandStack.getCount();
                    }
                }
                
                // Если текущее количество меньше требуемого, пополняем
                if (currentCount < requiredCount) {
                    int needed = requiredCount - currentCount;
                    int refilledForThisItem = 0;
                    
                    // Создаём стеки для пополнения (по 64 предмета максимум в каждом стеке)
                    while (needed > 0) {
                        int stackSize = Math.min(needed, requiredStack.getMaxStackSize());
                        ItemStack toAdd = requiredStack.copy();
                        toAdd.setCount(stackSize);
                        
                        // Добавляем в инвентарь
                        if (player.getInventory().add(toAdd)) {
                            needed -= stackSize;
                            refilledForThisItem += stackSize;
                            anyRefilled = true;
                        } else {
                            // Инвентарь полон, прекращаем пополнение для этого предмета
                            break;
                        }
                    }
                    
                    totalRefilled += refilledForThisItem;
                }
            }
        }
        
        // Если ничего не пополнили, сообщаем об этом
        if (!anyRefilled) {
            player.displayClientMessage(
                Component.translatable("wrb.ammunition.nothing_to_refill"),
                false
            );
        } else if (totalRefilled > 0) {
            player.displayClientMessage(
                Component.translatable("wrb.ammunition.success", totalRefilled),
                false
            );
        }
    }
}

