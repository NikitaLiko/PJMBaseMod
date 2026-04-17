package ru.liko.pjmbasemod.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.liko.pjmbasemod.common.network.packet.OpenMatchStatsPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Экран итогов матча.
 * Показывает победителя, статистику всех игроков по командам.
 */
public class MatchStatsScreen extends Screen {

    private final String winnerTeam;
    private final String reason;
    private final int matchDurationSeconds;
    private final List<OpenMatchStatsPacket.PlayerStatsEntry> playerStats;

    // Разделённые списки по командам
    private List<OpenMatchStatsPacket.PlayerStatsEntry> team1Stats = new ArrayList<>();
    private List<OpenMatchStatsPacket.PlayerStatsEntry> team2Stats = new ArrayList<>();
    private String team1Name = "";
    private String team2Name = "";

    // Цвета
    private static final int COLOR_GOLD = 0xFFFFAA00;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    private static final int COLOR_GREEN = 0xFF55FF55;
    private static final int COLOR_RED = 0xFFFF5555;
    private static final int COLOR_YELLOW = 0xFFFFFF55;
    private static final int COLOR_HEADER_BG = 0xAA333333;
    private static final int COLOR_ROW_BG_1 = 0x66222222;
    private static final int COLOR_ROW_BG_2 = 0x66333333;
    private static final int COLOR_WINNER_BG = 0x4400AA00;

    // Scroll offset for long player lists
    private int scrollOffset = 0;
    private int maxVisibleRows = 10;

    public MatchStatsScreen(OpenMatchStatsPacket packet) {
        super(Component.literal("Match Stats"));
        this.winnerTeam = packet.getWinnerTeam();
        this.reason = packet.getReason();
        this.matchDurationSeconds = packet.getMatchDurationSeconds();
        this.playerStats = packet.getPlayerStats();
    }

    /**
     * Конструктор по умолчанию (для совместимости, показывает пустую статистику).
     */
    public MatchStatsScreen() {
        super(Component.literal("Match Stats"));
        this.winnerTeam = "";
        this.reason = "";
        this.matchDurationSeconds = 0;
        this.playerStats = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();

        // Разделяем игроков по командам
        separateTeams();

        // Сортируем по очкам (score) по убыванию
        team1Stats.sort(Comparator.comparingInt(OpenMatchStatsPacket.PlayerStatsEntry::score).reversed());
        team2Stats.sort(Comparator.comparingInt(OpenMatchStatsPacket.PlayerStatsEntry::score).reversed());

        // Рассчитываем максимум видимых строк
        maxVisibleRows = Math.max(3, (this.height - 160) / 16);

        // Кнопка "Продолжить" — закрывает экран
        addRenderableWidget(Button.builder(Component.literal("§aПродолжить"), (btn) -> {
            this.onClose();
        }).bounds(this.width / 2 - 60, this.height - 35, 120, 20).build());
    }

    private void separateTeams() {
        team1Stats.clear();
        team2Stats.clear();

        if (playerStats.isEmpty()) return;

        // Определяем имена команд из первых записей
        java.util.Set<String> teamNames = new java.util.LinkedHashSet<>();
        for (OpenMatchStatsPacket.PlayerStatsEntry entry : playerStats) {
            teamNames.add(entry.team());
        }

        List<String> teams = new ArrayList<>(teamNames);
        team1Name = teams.size() > 0 ? teams.get(0) : "Team 1";
        team2Name = teams.size() > 1 ? teams.get(1) : "Team 2";

        for (OpenMatchStatsPacket.PlayerStatsEntry entry : playerStats) {
            if (entry.team().equals(team1Name)) {
                team1Stats.add(entry);
            } else {
                team2Stats.add(entry);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Отключаем ванильный блюр — свой фон рисуем в render()
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон
        g.fill(0, 0, this.width, this.height, 0xDD000000);

        int centerX = this.width / 2;

        // === ЗАГОЛОВОК ===
        g.drawCenteredString(this.font, "§l§6МАТЧ ЗАВЕРШЁН", centerX, 10, COLOR_GOLD);

        // Победитель
        if (!winnerTeam.isEmpty()) {
            String winnerText = "§eПобедитель: §a§l" + winnerTeam.toUpperCase();
            g.drawCenteredString(this.font, winnerText, centerX, 24, COLOR_GREEN);
        }

        // Причина завершения и длительность
        String infoLine = "";
        if (!reason.isEmpty()) {
            infoLine += "§7" + reason;
        }
        if (matchDurationSeconds > 0) {
            int minutes = matchDurationSeconds / 60;
            int seconds = matchDurationSeconds % 60;
            if (!infoLine.isEmpty()) infoLine += "  §8|  ";
            infoLine += "§7Длительность: §f" + String.format("%d:%02d", minutes, seconds);
        }
        if (!infoLine.isEmpty()) {
            g.drawCenteredString(this.font, infoLine, centerX, 36, COLOR_GRAY);
        }

        // === ТАБЛИЦА СТАТИСТИКИ ===
        int tableTop = 52;
        int halfWidth = this.width / 2 - 10;

        // Команда 1 (левая половина)
        renderTeamTable(g, 5, tableTop, halfWidth, team1Name, team1Stats, team1Name.equals(winnerTeam));

        // Команда 2 (правая половина)
        renderTeamTable(g, centerX + 5, tableTop, halfWidth, team2Name, team2Stats, team2Name.equals(winnerTeam));

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderTeamTable(GuiGraphics g, int x, int y, int tableWidth,
                                  String teamName, List<OpenMatchStatsPacket.PlayerStatsEntry> stats,
                                  boolean isWinner) {
        // Фон заголовка команды
        int headerColor = isWinner ? COLOR_WINNER_BG : COLOR_HEADER_BG;
        g.fill(x, y, x + tableWidth, y + 16, headerColor);

        // Название команды
        String teamTitle = (isWinner ? "§a§l★ " : "§f§l") + teamName.toUpperCase() + (isWinner ? " ★" : "");
        g.drawCenteredString(this.font, teamTitle, x + tableWidth / 2, y + 4, COLOR_WHITE);

        y += 18;

        // Заголовки колонок
        g.fill(x, y, x + tableWidth, y + 12, 0xAA444444);
        int colPlayer = x + 4;
        int colKills = x + tableWidth - 180;
        int colDeaths = x + tableWidth - 140;
        int colAssists = x + tableWidth - 100;
        int colCapture = x + tableWidth - 60;
        int colScore = x + tableWidth - 28;

        g.drawString(this.font, "§7Игрок", colPlayer, y + 2, COLOR_GRAY, false);
        g.drawString(this.font, "§7У", colKills, y + 2, COLOR_GRAY, false);
        g.drawString(this.font, "§7С", colDeaths, y + 2, COLOR_GRAY, false);
        g.drawString(this.font, "§7А", colAssists, y + 2, COLOR_GRAY, false);
        g.drawString(this.font, "§7Зх", colCapture, y + 2, COLOR_GRAY, false);
        g.drawString(this.font, "§7Очки", colScore, y + 2, COLOR_GRAY, false);

        y += 14;

        // Строки игроков
        if (stats.isEmpty()) {
            g.fill(x, y, x + tableWidth, y + 16, COLOR_ROW_BG_1);
            g.drawCenteredString(this.font, "§8Нет игроков", x + tableWidth / 2, y + 4, COLOR_GRAY);
            return;
        }

        int maxRows = Math.min(stats.size(), maxVisibleRows);
        for (int i = 0; i < maxRows; i++) {
            int idx = i + scrollOffset;
            if (idx >= stats.size()) break;

            OpenMatchStatsPacket.PlayerStatsEntry entry = stats.get(idx);

            // Чередующийся фон строк
            int rowBg = (i % 2 == 0) ? COLOR_ROW_BG_1 : COLOR_ROW_BG_2;

            // Подсветка лучшего игрока (MVP — первый по очкам)
            if (i == 0 && scrollOffset == 0) {
                rowBg = 0x44FFAA00; // Золотой для MVP
            }

            g.fill(x, y, x + tableWidth, y + 14, rowBg);

            // Имя игрока (обрезаем если слишком длинное)
            String playerName = entry.playerName();
            int maxNameWidth = colKills - colPlayer - 4;
            if (this.font.width(playerName) > maxNameWidth) {
                while (this.font.width(playerName + "...") > maxNameWidth && playerName.length() > 1) {
                    playerName = playerName.substring(0, playerName.length() - 1);
                }
                playerName += "...";
            }

            // MVP получает золотой цвет
            int nameColor = (i == 0 && scrollOffset == 0) ? COLOR_GOLD : COLOR_WHITE;
            String mvpPrefix = (i == 0 && scrollOffset == 0) ? "§6§l" : "§f";

            g.drawString(this.font, mvpPrefix + playerName, colPlayer, y + 3, nameColor, false);
            g.drawString(this.font, "§a" + entry.kills(), colKills, y + 3, COLOR_GREEN, false);
            g.drawString(this.font, "§c" + entry.deaths(), colDeaths, y + 3, COLOR_RED, false);
            g.drawString(this.font, "§e" + entry.assists(), colAssists, y + 3, COLOR_YELLOW, false);
            g.drawString(this.font, "§b" + entry.capturePoints(), colCapture, y + 3, 0xFF55FFFF, false);
            g.drawString(this.font, "§f" + entry.score(), colScore, y + 3, COLOR_WHITE, false);

            y += 14;
        }

        // Индикатор скролла если игроков больше чем видимых строк
        if (stats.size() > maxVisibleRows) {
            g.drawCenteredString(this.font, "§8▼ ещё " + (stats.size() - maxVisibleRows) + " игроков",
                x + tableWidth / 2, y + 2, COLOR_GRAY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxSize = Math.max(team1Stats.size(), team2Stats.size());
        if (maxSize > maxVisibleRows) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) scrollY, maxSize - maxVisibleRows));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
