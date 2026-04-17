package ru.liko.pjmbasemod.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import ru.liko.pjmbasemod.client.ClientTeamConfig;
import ru.liko.pjmbasemod.client.ClientPlayerDataCache;
import ru.liko.pjmbasemod.client.compat.WarBornGuardCompat;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.player.PjmPlayerDataProvider;
import ru.liko.pjmbasemod.common.player.PjmRank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Кастомный TAB-оверлей в стиле Squad/Arma с иконками званий
 */
public class CustomTabOverlay {

    private static final Minecraft minecraft = Minecraft.getInstance();

    // Цвета в стиле Squad/Arma
    private static final int COLOR_BG = 0xCC0F0F0F; // Темный фон
    private static final int COLOR_HEADER_BG = 0xEE050505; // Еще темнее для заголовка
    private static final int COLOR_SUBHEADER_BG = 0x80000000; // Фон заголовков столбцов

    private static final int COLOR_RED_TEAM = 0xFFE74C3C; // Красный (Opfor/VSRF)
    private static final int COLOR_BLUE_TEAM = 0xFF3498DB; // Синий (Blufor/NATO)
    private static final int COLOR_GREEN_TEAM = 0xFF27AE60; // Зеленый (Indep)

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFB0B0B0;
    private static final int COLOR_PING_GOOD = 0xFF27AE60; // Зеленый
    private static final int COLOR_PING_OK = 0xFFF39C12; // Желтый
    private static final int COLOR_PING_BAD = 0xFFE74C3C; // Красный

    // Иконки
    private static final ResourceLocation DEAD_ICON = ResourceLocation.fromNamespaceAndPath("pjmbasemod",
            "textures/icon/dead_icon.png");
    private static final ResourceLocation NATO_ICON = ResourceLocation.fromNamespaceAndPath("pjmbasemod",
            "textures/icon/nato_icon.png");
    private static final ResourceLocation RU_ICON = ResourceLocation.fromNamespaceAndPath("pjmbasemod",
            "textures/icon/ru_icon.png");

    private static final int ROW_HEIGHT = 14;
    private static final int ICON_SIZE = 10;
    private static final int HEADER_HEIGHT = 20;
    private static final int COLUMN_HEADER_HEIGHT = 12;

    /**
     * Рендерит кастомный TAB-список
     */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight, Scoreboard scoreboard,
            Objective objective) {
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return;
        }

        List<PlayerInfo> players = new ArrayList<>(connection.getOnlinePlayers());
        if (players.isEmpty()) {
            return;
        }

        // Сортируем игроков по командам и именам
        players.sort(Comparator
                .comparing((PlayerInfo p) -> getPlayerTeamName(p, scoreboard))
                .thenComparing(p -> p.getProfile().getName()));

        // Разделяем игроков по командам
        List<PlayerInfo> team1Players = new ArrayList<>();
        List<PlayerInfo> team2Players = new ArrayList<>();
        List<PlayerInfo> noTeamPlayers = new ArrayList<>();

        for (PlayerInfo player : players) {
            String teamName = getPlayerTeamName(player, scoreboard);
            String team1Id = ClientTeamConfig.getTeam1Name().toLowerCase();
            String team2Id = ClientTeamConfig.getTeam2Name().toLowerCase();

            if (teamName.equalsIgnoreCase(team1Id)) {
                team1Players.add(player);
            } else if (teamName.equalsIgnoreCase(team2Id)) {
                team2Players.add(player);
            } else {
                noTeamPlayers.add(player);
            }
        }

        // Вычисляем размеры
        int maxPlayers = Math.max(Math.max(team1Players.size(), team2Players.size()), 1);
        int panelWidth = 280;
        int panelHeight = HEADER_HEIGHT + COLUMN_HEADER_HEIGHT + (maxPlayers * ROW_HEIGHT);
        int spacing = 10;

        int totalWidth = (panelWidth * 2) + spacing;
        int startX = (screenWidth - totalWidth) / 2;
        int startY = 40; // Оставляем место для заголовка

        // Рендерим заголовок с информацией о сервере
        renderServerHeader(graphics, screenWidth, players.size());

        // Получаем отображаемые имена команд и цвета
        String team1Id = ClientTeamConfig.getTeam1Name();
        String team2Id = ClientTeamConfig.getTeam2Name();

        String team1DisplayName = ClientTeamConfig.getDisplayName(team1Id).toUpperCase();
        String team2DisplayName = ClientTeamConfig.getDisplayName(team2Id).toUpperCase();

        int team1Color = getTeamColor(team1Id);
        int team2Color = getTeamColor(team2Id);

        // Определяем иконки команд
        ResourceLocation team1Icon = getTeamIcon(team1Id);
        ResourceLocation team2Icon = getTeamIcon(team2Id);

        // Рендерим Team 1
        renderTeamPanel(graphics, startX, startY, panelWidth, panelHeight,
                team1DisplayName, team1Color, team1Players, scoreboard, team1Icon);

        // Рендерим Team 2
        renderTeamPanel(graphics, startX + panelWidth + spacing, startY, panelWidth, panelHeight,
                team2DisplayName, team2Color, team2Players, scoreboard, team2Icon);

        // Рендерим игроков без команды внизу
        if (!noTeamPlayers.isEmpty()) {
            int noTeamY = startY + panelHeight + 10;
            int noTeamWidth = (panelWidth * 2) + spacing;
            int noTeamHeight = HEADER_HEIGHT + COLUMN_HEADER_HEIGHT + (noTeamPlayers.size() * ROW_HEIGHT);
            renderTeamPanel(graphics, startX, noTeamY, noTeamWidth, noTeamHeight,
                    "NO TEAM", COLOR_GRAY, noTeamPlayers, scoreboard, null);
        }
    }

    /**
     * Рендерит заголовок с информацией о сервере
     */
    private static void renderServerHeader(GuiGraphics graphics, int screenWidth, int totalPlayers) {
        String serverName = "PROJECT MINECRAFT SERVER";
        String playerCount = totalPlayers + " PLAYERS ONLINE";

        int headerY = 15;

        // Название сервера
        int serverNameWidth = minecraft.font.width(serverName);
        graphics.drawString(minecraft.font, serverName,
                (screenWidth - serverNameWidth) / 2, headerY, COLOR_WHITE, true);

        // Счетчик игроков
        int playerCountWidth = minecraft.font.width(playerCount);
        graphics.drawString(minecraft.font, playerCount,
                (screenWidth - playerCountWidth) / 2, headerY + 10, COLOR_GRAY, false);
    }

    /**
     * Рендерит панель команды
     */
    private static void renderTeamPanel(GuiGraphics graphics, int x, int y, int width, int height,
            String teamName, int teamColor, List<PlayerInfo> players, Scoreboard scoreboard,
            ResourceLocation teamIcon) {
        // Фон панели
        graphics.fill(x, y, x + width, y + height, COLOR_BG);

        // Заголовок команды
        graphics.fill(x, y, x + width, y + HEADER_HEIGHT, COLOR_HEADER_BG);

        // Линия под заголовком
        graphics.fill(x, y + HEADER_HEIGHT - 1, x + width, y + HEADER_HEIGHT, teamColor);

        // Иконка команды слева от названия
        int iconX = x + 6;
        int iconY = y + 2;
        if (teamIcon != null) {
            RenderSystem.enableBlend();
            graphics.blit(teamIcon, iconX, iconY, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
            iconX += 20; // Смещаем текст после иконки
        }

        String headerText = teamName + " (" + players.size() + ")";
        int headerTextWidth = minecraft.font.width(headerText);
        graphics.drawString(minecraft.font, headerText,
                iconX + ((width - iconX + x) - headerTextWidth) / 2, y + 6, teamColor, false);

        // Фон заголовков столбцов
        graphics.fill(x, y + HEADER_HEIGHT, x + width, y + HEADER_HEIGHT + COLUMN_HEADER_HEIGHT, COLOR_SUBHEADER_BG);

        // Координаты столбцов
        int colRankX = x + 8;
        int colNameX = colRankX + 18;
        int colScoreRight = x + width - 50;
        int colPingRight = x + width - 8;

        int colHeaderY = y + HEADER_HEIGHT + 2;
        int headerColor = 0xAAAAAA;

        // Заголовки
        graphics.drawString(minecraft.font, "RANK", colRankX, colHeaderY, headerColor, false);
        graphics.drawString(minecraft.font, "NAME", colNameX, colHeaderY, headerColor, false);

        String kdHeader = "K/D";
        int kdWidth = minecraft.font.width(kdHeader);
        graphics.drawString(minecraft.font, kdHeader, colScoreRight - kdWidth, colHeaderY, headerColor, false);

        String pingHeader = "PING";
        int pingWidth = minecraft.font.width(pingHeader);
        graphics.drawString(minecraft.font, pingHeader, colPingRight - pingWidth, colHeaderY, headerColor, false);

        // Рендерим игроков
        int currentY = y + HEADER_HEIGHT + COLUMN_HEADER_HEIGHT;
        for (PlayerInfo playerInfo : players) {
            renderPlayerRow(graphics, x, currentY, width, playerInfo, scoreboard);
            currentY += ROW_HEIGHT;
        }
    }

    /**
     * Рендерит строку игрока
     */
    private static void renderPlayerRow(GuiGraphics graphics, int x, int y, int width,
            PlayerInfo playerInfo, Scoreboard scoreboard) {
        // Получаем данные игрока
        PjmRank rank = PjmRank.PRIVATE; // По умолчанию Рядовой
        String playerClass = "";
        UUID playerId = playerInfo.getProfile().getId();
        Player player = minecraft.level != null ? minecraft.level.getPlayerByUUID(playerId) : null;

        // Сначала пытаемся получить данные из кэша (для всех игроков, включая тех, кто
        // не в мире)
        ClientPlayerDataCache.PlayerDataSnapshot cachedData = ClientPlayerDataCache.get(playerId);
        if (cachedData != null && cachedData.getRank() != null) {
            rank = cachedData.getRank();
            var classEnum = cachedData.getPlayerClass();
            if (classEnum != null && classEnum != ru.liko.pjmbasemod.common.player.PjmPlayerClass.NONE) {
                playerClass = classEnum.name().substring(0, 3).toUpperCase(); // ASS, MED, ENG и т.д.
            }
        } else if (player != null) {
            // Если данных нет в кэше, пытаемся получить из Data Attachments (NeoForge
            // 1.21.1)
            PjmPlayerData data = PjmPlayerDataProvider.get(player);
            if (data != null) {
                rank = data.getRank();
                var classEnum = data.getPlayerClass();
                if (classEnum != null && classEnum != ru.liko.pjmbasemod.common.player.PjmPlayerClass.NONE) {
                    playerClass = classEnum.name().substring(0, 3).toUpperCase(); // ASS, MED, ENG и т.д.
                }
                // Обновляем кэш для будущего использования
                ClientPlayerDataCache.update(playerId, rank, classEnum, data.getTeam());
            }
        }

        // Фон строки (чередование)
        // Используем индекс строки для чередования
        int rowIndex = (y - (HEADER_HEIGHT + COLUMN_HEADER_HEIGHT)) / ROW_HEIGHT; // Примерный индекс
        // Но лучше использовать просто координаты для четности
        boolean isEven = (rowIndex % 2) == 0;

        int backgroundColor = isEven ? 0x00000000 : 0x08FFFFFF;
        boolean isLocalPlayer = playerInfo.getProfile().getId().equals(minecraft.player.getGameProfile().getId());
        if (isLocalPlayer) {
            backgroundColor = 0x303498DB; // Подсветка своего игрока
        }
        graphics.fill(x, y, x + width, y + ROW_HEIGHT, backgroundColor);

        int centerY = y + (ROW_HEIGHT - 8) / 2;

        // Координаты столбцов
        int colRankX = x + 8;
        int colNameX = colRankX + 18;
        int colScoreRight = x + width - 50;
        int colPingRight = x + width - 8;

        // 1. Иконка звания (отображаем для всех рангов, включая PRIVATE)
        if (rank != null) {
            ResourceLocation rankIcon = rank.getIconLocation();
            if (rankIcon != null) {
                RenderSystem.enableBlend();
                graphics.blit(rankIcon, colRankX, centerY - 1, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                RenderSystem.disableBlend();
            }
        }

        // 2. Иконка смерти
        if (player != null && player.isDeadOrDying()) {
            RenderSystem.enableBlend();
            graphics.blit(DEAD_ICON, colRankX + 12, centerY - 1, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            RenderSystem.disableBlend();
        }

        // 3. Имя игрока
        String playerName = playerInfo.getProfile().getName();
        int nameMaxWidth = (colScoreRight - 10) - colNameX;

        // Класс игрока
        boolean hasClass = playerClass != null && !playerClass.isEmpty();
        String classTag = hasClass ? "[" + playerClass + "]" : "";
        int classWidth = hasClass ? (minecraft.font.width(" " + classTag) + 2) : 0;

        // WarBornGuard admin status tags
        int flags = 0;
        boolean viewerIsAdmin = minecraft.player != null && minecraft.player.hasPermissions(2);
        if (viewerIsAdmin) {
            flags = WarBornGuardCompat.getFlags(playerInfo.getProfile().getId());
        }
        boolean tagVanish = (flags & WarBornGuardCompat.FLAG_VANISH) != 0;
        boolean tagEsp = (flags & WarBornGuardCompat.FLAG_ESP) != 0;
        String vanishTag = tagVanish ? "[V]" : "";
        String espTag = tagEsp ? "[ESP]" : "";

        int tagsWidth = 0;
        if (!classTag.isEmpty())
            tagsWidth += classWidth;
        if (!vanishTag.isEmpty())
            tagsWidth += minecraft.font.width(" " + vanishTag) + 2;
        if (!espTag.isEmpty())
            tagsWidth += minecraft.font.width(" " + espTag) + 2;

        if (tagsWidth > 0) {
            nameMaxWidth = Math.max(0, nameMaxWidth - tagsWidth);
        }

        String displayName = playerName;
        if (minecraft.font.width(displayName) > nameMaxWidth) {
            displayName = minecraft.font.plainSubstrByWidth(displayName,
                    Math.max(0, nameMaxWidth - minecraft.font.width("..."))) + "...";
        }

        int nameColor = isLocalPlayer ? COLOR_WHITE : 0xFFDDDDDD;
        graphics.drawString(minecraft.font, displayName, colNameX, centerY, nameColor, false);

        int tagX = colNameX + minecraft.font.width(displayName) + 4;
        if (!classTag.isEmpty()) {
            graphics.drawString(minecraft.font, classTag, tagX, centerY, getClassColor(playerClass), false);
            tagX += minecraft.font.width(classTag) + 4;
        }
        if (!vanishTag.isEmpty()) {
            graphics.drawString(minecraft.font, vanishTag, tagX, centerY, 0xFFE67E22, false);
            tagX += minecraft.font.width(vanishTag) + 4;
        }
        if (!espTag.isEmpty()) {
            graphics.drawString(minecraft.font, espTag, tagX, centerY, 0xFF9B59B6, false);
        }

        // 4. K/D
        int score = 0;
        var listObjective = scoreboard.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.LIST);
        if (listObjective != null) {
            var scoreAccess = scoreboard.getOrCreatePlayerScore(
                    net.minecraft.world.scores.ScoreHolder.forNameOnly(playerInfo.getProfile().getName()),
                    listObjective);
            score = scoreAccess.get();
        }
        String scoreText = String.valueOf(score);
        int scoreWidth = minecraft.font.width(scoreText);
        graphics.drawString(minecraft.font, scoreText, colScoreRight - scoreWidth, centerY, COLOR_GRAY, false);

        // 5. Пинг
        int ping = playerInfo.getLatency();
        String pingText = ping + "ms";
        int pingWidth = minecraft.font.width(pingText);
        int pingColor = getPingColor(ping);
        graphics.drawString(minecraft.font, pingText, colPingRight - pingWidth, centerY, pingColor, false);
    }

    private static int getTeamColor(String teamId) {
        if (teamId == null)
            return COLOR_GRAY;
        String lower = teamId.toLowerCase();

        if (lower.contains("vsrf") || lower.contains("ru") || lower.contains("red") || lower.contains("opfor")) {
            return COLOR_RED_TEAM;
        }
        if (lower.contains("nato") || lower.contains("us") || lower.contains("blue") || lower.contains("blufor")) {
            return COLOR_BLUE_TEAM;
        }
        if (lower.contains("indep") || lower.contains("green")) {
            return COLOR_GREEN_TEAM;
        }

        if (lower.equals(ClientTeamConfig.getTeam1Name().toLowerCase())) {
            return COLOR_RED_TEAM;
        }
        if (lower.equals(ClientTeamConfig.getTeam2Name().toLowerCase())) {
            return COLOR_BLUE_TEAM;
        }

        return COLOR_GRAY;
    }

    private static int getClassColor(String classCode) {
        return switch (classCode) {
            case "MED" -> 0xFFE74C3C;
            case "ENG" -> 0xFFF1C40F;
            case "SNI" -> 0xFF2ECC71;
            case "ASS" -> 0xFF3498DB;
            default -> COLOR_GRAY;
        };
    }

    private static String getPlayerTeamName(PlayerInfo playerInfo, Scoreboard scoreboard) {
        PlayerTeam team = scoreboard.getPlayersTeam(playerInfo.getProfile().getName());
        return team != null ? team.getName() : "";
    }

    private static int getPingColor(int ping) {
        if (ping < 50)
            return COLOR_PING_GOOD;
        if (ping < 150)
            return COLOR_PING_OK;
        return COLOR_PING_BAD;
    }

    private static ResourceLocation getTeamIcon(String teamName) {
        if (teamName == null)
            return null;

        String teamLower = teamName.toLowerCase();
        if (teamLower.contains("nato") || teamLower.contains("us") || teamLower.contains("blue")) {
            return NATO_ICON;
        } else if (teamLower.contains("vsrf") || teamLower.contains("ru") || teamLower.contains("red")) {
            return RU_ICON;
        }

        if (teamLower.equals(ClientTeamConfig.getTeam2Name().toLowerCase())) {
            return NATO_ICON;
        } else if (teamLower.equals(ClientTeamConfig.getTeam1Name().toLowerCase())) {
            return RU_ICON;
        }

        return null;
    }
}
