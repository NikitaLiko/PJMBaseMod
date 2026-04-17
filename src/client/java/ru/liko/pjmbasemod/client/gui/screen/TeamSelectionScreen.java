package ru.liko.pjmbasemod.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import ru.liko.pjmbasemod.Pjmbasemod;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.SelectTeamPacket;

import java.util.Map;
public class TeamSelectionScreen extends Screen {
    // Цвета
    private static final int COLOR_BACKGROUND = 0xFF000000; // Черный фон
    private static final int COLOR_PANEL_BORDER = 0x90FFFFFF;
    private static final int COLOR_PANEL_BORDER_HOVER = 0xFFFFFFFF;
    private static final int COLOR_WHITE_TEXT = 0xFFFFFFFF;
    private static final int COLOR_GREY_TEXT = 0xFFAAAAAA;
    private static final int COLOR_GREEN = 0xFF27AE60;
    private static final int COLOR_RED = 0xFFE74C3C;

    private static final int COLOR_RED_TEAM = 0xFFE74C3C; // Team 1 - Red
    private static final int COLOR_BLUE_TEAM = 0xFF3498DB; // Team 2 - Blue

    private static final ResourceLocation TEAM1_BACKGROUND = ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/team_okv_background.png");
    private static final ResourceLocation TEAM2_BACKGROUND = ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/team_oks_background.png");

    private final Map<String, Integer> teamBalanceInfo;
    private final String team1Name;
    private final String team2Name;
    private final boolean allowBack;
    private final int balanceThreshold;
    
    private float fadeIn = 0.0f;
    private long initTime;
    private String errorMessage = null;
    private String successMessage = null;
    private long successTime = 0;
    private static final long SUCCESS_DISPLAY_TIME = 2000; // 2 секунды

    // Панели и отступы
    private int leftPanelX, leftPanelY, panelWidth, panelHeight;
    private int rightPanelX, rightPanelY;
    private int layoutMargin;
    private int panelSpacing;
    private int headerHeight;
    private int footerHeight;
    private int contentAreaTop;
    private int footerAreaTop;
    private int messageY;

    // Анимация
    private float leftPanelHover = 0.0f;
    private float rightPanelHover = 0.0f;

    public TeamSelectionScreen(Map<String, Integer> teamBalanceInfo, String team1Name, String team2Name, boolean allowBack, int balanceThreshold) {
        super(Component.translatable("wrb.team.selection.title"));
        this.teamBalanceInfo = teamBalanceInfo;
        this.team1Name = team1Name != null ? team1Name : "team1";
        this.team2Name = team2Name != null ? team2Name : "team2";
        this.allowBack = allowBack;
        this.balanceThreshold = balanceThreshold;
        this.initTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        
        headerHeight = Math.max(90, (int)(this.height * 0.18f));
        footerHeight = Math.max(80, (int)(this.height * 0.12f));
        contentAreaTop = headerHeight;
        footerAreaTop = this.height - footerHeight;

        layoutMargin = Math.max(20, this.width / 25);
        panelSpacing = Math.max(16, this.width / 40);

        int horizontalSpace = Math.max(200, this.width - layoutMargin * 2);
        int desiredPanelWidth = (horizontalSpace - panelSpacing) / 2;
        int maxPanelWidth = Math.min((int)(this.height * 0.65f), horizontalSpace);
        panelWidth = Math.max(200, Math.min(desiredPanelWidth, maxPanelWidth));

        int totalWidth = panelWidth * 2 + panelSpacing;
        if (totalWidth > horizontalSpace) {
            int overflow = totalWidth - horizontalSpace;
            panelSpacing = Math.max(12, panelSpacing - overflow);
            totalWidth = panelWidth * 2 + panelSpacing;
            if (totalWidth > horizontalSpace) {
                panelWidth = Math.max(180, (horizontalSpace - panelSpacing) / 2);
                totalWidth = panelWidth * 2 + panelSpacing;
            }
        }

        leftPanelX = (this.width - totalWidth) / 2;
        rightPanelX = leftPanelX + panelWidth + panelSpacing;

        int availableHeight = Math.max(160, footerAreaTop - contentAreaTop);
        int maxPanelHeight = Math.max(160, availableHeight - Math.max(20, this.height / 40));
        panelHeight = Math.min(Math.max(220, (int)(this.height * 0.55f)), maxPanelHeight);
        panelHeight = Math.max(160, Math.min(panelHeight, availableHeight));

        int verticalOffset = Math.max(0, (availableHeight - panelHeight) / 2);
        leftPanelY = contentAreaTop + verticalOffset;
        rightPanelY = leftPanelY;

        messageY = footerAreaTop - Math.max(28, footerHeight / 3);

        if (allowBack) {
            int buttonWidth = Math.min(180, Math.max(140, this.width / 8));
            int buttonHeight = Math.max(28, this.height / 18);
            int buttonY = footerAreaTop + Math.max(4, (footerHeight - buttonHeight) / 2);

            this.addRenderableWidget(new TacticalButton(
                layoutMargin, buttonY,
                buttonWidth, buttonHeight,
                Component.translatable("gui.back"),
                button -> onClose()
            ));
        }
    }

    private boolean canJoinTeam(String teamId) {
        int team1Count = teamBalanceInfo.getOrDefault("team1", 0);
        int team2Count = teamBalanceInfo.getOrDefault("team2", 0);
        
        if (this.balanceThreshold <= 0) {
            return true; // Баланс отключен
        }

        int teamToJoinCount = "team1".equals(teamId) ? team1Count : team2Count;
        int otherTeamCount = "team1".equals(teamId) ? team2Count : team1Count;
        
        return teamToJoinCount + 1 <= otherTeamCount + this.balanceThreshold;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            if (isMouseOver(pMouseX, pMouseY, leftPanelX, leftPanelY, panelWidth, panelHeight) && canJoinTeam("team2")) {
                selectTeam("team2");
                return true;
            }
            if (isMouseOver(pMouseX, pMouseY, rightPanelX, rightPanelY, panelWidth, panelHeight) && canJoinTeam("team1")) {
                selectTeam("team1");
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Отключаем ванильный блюр
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.currentTimeMillis() - initTime;
        fadeIn = Math.min(1.0f, elapsed / 500.0f);
        
        leftPanelHover = Mth.lerp(partialTick * 0.5f, leftPanelHover, isMouseOver(mouseX, mouseY, leftPanelX, leftPanelY, panelWidth, panelHeight) ? 1.0f : 0.0f);
        rightPanelHover = Mth.lerp(partialTick * 0.5f, rightPanelHover, isMouseOver(mouseX, mouseY, rightPanelX, rightPanelY, panelWidth, panelHeight) ? 1.0f : 0.0f);

        graphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);

        renderTitle(graphics);

        renderTeamPanel(graphics, leftPanelX, leftPanelY, panelWidth, panelHeight, "team2", this.team2Name, COLOR_BLUE_TEAM, TEAM2_BACKGROUND, leftPanelHover, true);
        renderTeamPanel(graphics, rightPanelX, rightPanelY, panelWidth, panelHeight, "team1", this.team1Name, COLOR_RED_TEAM, TEAM1_BACKGROUND, rightPanelHover, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        renderMessages(graphics);
    }
    
    private void renderTitle(GuiGraphics graphics) {
        int alpha = (int)(fadeIn * 255);
        int titleColor = (alpha << 24) | 0x00FFFFFF;

        Component title = Component.translatable("wrb.team.selection.title");
        int titleWidth = this.font.width(title);

        float scaleBase = Math.max(1.2f, Math.min(2.4f, (float)this.width / 960f * 1.4f));
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(this.width / 2f, headerHeight / 2f, 0);
        poseStack.scale(scaleBase, scaleBase, scaleBase);
        graphics.drawString(this.font, title, -titleWidth / 2, -this.font.lineHeight / 2, titleColor, true);
        poseStack.popPose();
    }
    
    private void renderTeamPanel(GuiGraphics graphics, int x, int y, int width, int height, String teamId, String teamName, int teamColor, ResourceLocation background, float hover, boolean isLeft) {
        PoseStack pose = graphics.pose();
        pose.pushPose();

        float scale = 1.0f + hover * 0.02f;
        pose.translate(x + width / 2f, y + height / 2f, 0);
        pose.scale(scale, scale, scale);
        pose.translate(-width / 2f, -height / 2f, 0);

        int alpha = (int)(fadeIn * 255);

        graphics.setColor(1.0f, 1.0f, 1.0f, fadeIn);
        graphics.blit(background, 0, 0, 0, 0, width, height, width, height);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Свечение сверху вниз цветом команды
        int glowAlpha = (int)(fadeIn * 128); // Полупрозрачность для свечения
        int glowColorTop = (glowAlpha << 24) | (teamColor & 0xFFFFFF);
        int glowColorBottom = (0 << 24) | (teamColor & 0xFFFFFF);
        
        // Рисуем градиент поверх фона, но под текстом
        graphics.fillGradient(0, 0, width, height / 2, glowColorTop, glowColorBottom);
        
        graphics.fillGradient(0, height / 2, width, height, 0x00000000, (alpha << 24) | 0x000000);
        
        int teamR = (teamColor >> 16) & 0xFF;
        int teamG = (teamColor >> 8) & 0xFF;
        int teamB = teamColor & 0xFF;
        int r = (int)Mth.lerp(hover, 255, teamR);
        int g = (int)Mth.lerp(hover, 255, teamG);
        int b = (int)Mth.lerp(hover, 255, teamB);
        int baseAlpha = 200;
        int finalAlpha = (int)(fadeIn * Mth.lerp(hover, baseAlpha, 255));
        int borderColor = (finalAlpha << 24) | (r << 16) | (g << 8) | b;
        graphics.renderOutline(0, 0, width, height, borderColor);

        int stripeColor = (alpha << 24) | teamColor;
        if(isLeft) {
            graphics.fill(0, 0, 25, 2, stripeColor);
        } else {
            graphics.fill(width - 25, 0, width, 2, stripeColor);
        }

        Component teamNameComponent = getTeamDisplayName(teamId, teamName);
        int teamNameWidth = this.font.width(teamNameComponent);
        int textColor = (alpha << 24) | COLOR_WHITE_TEXT;
        graphics.drawString(this.font, teamNameComponent, (width - teamNameWidth) / 2, height - 20, textColor, true);

        int playerCount = this.teamBalanceInfo.getOrDefault(teamId, 0);
        String playerCountText = Component.translatable("wrb.team.selection.players.count", playerCount).getString();
        int playerCountWidth = this.font.width(playerCountText);
        int greyTextColor = (alpha << 24) | COLOR_GREY_TEXT;
        graphics.drawString(this.font, playerCountText, width - playerCountWidth - 10, 10, greyTextColor, true);
        
        pose.popPose();
    }

    private void renderMessages(GuiGraphics graphics) {
        int messageX = this.width / 2;

        if (errorMessage != null && !errorMessage.isEmpty()) {
            int errorColor = (255 << 24) | (COLOR_RED & 0x00FFFFFF);
            graphics.drawCenteredString(this.font, errorMessage, messageX, messageY, errorColor);
        }

        if (successMessage != null && !successMessage.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - successTime < SUCCESS_DISPLAY_TIME) {
                int successColor = (255 << 24) | (COLOR_GREEN & 0x00FFFFFF);
                graphics.drawCenteredString(this.font, successMessage, messageX, messageY, successColor);
            } else {
                this.onClose();
            }
        }
    }
    
    private void selectTeam(String teamId) {
        PjmNetworking.sendToServer(new SelectTeamPacket(teamId));
    }

    public void showError(String message) {
        this.errorMessage = message;
        this.successMessage = null;
    }

    public void showSuccess(String message) {
        this.successMessage = null;
        this.errorMessage = null;
        if (this.minecraft != null && this.minecraft.player != null && message != null && !message.isEmpty()) {
            this.minecraft.player.displayClientMessage(Component.literal(message), false);
        }
        // При успешном выборе команды принудительно закрываем экран
        // (команда уже назначена на сервере, просто может быть еще не синхронизирована с клиентом)
        super.onClose();
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        // Если allowBack = false, нельзя закрыть
        if (!allowBack) {
            return false;
        }
        // Если allowBack = true, но у игрока нет команды, тоже нельзя закрыть
        return hasTeam();
    }

    @Override
    public void onClose() {
        // Проверяем, можно ли закрыть экран
        if (!allowBack || !hasTeam()) {
            // Нельзя закрыть - игрок должен выбрать команду
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Проверяет, есть ли у игрока команда
     */
    private boolean hasTeam() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        return this.minecraft.player.getTeam() != null;
    }
    
    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private Component getTeamDisplayName(String teamId, String configuredName) {
        // 1. Пытаемся получить имя из Scoreboard (/team modify ... displayName)
        if (this.minecraft != null && this.minecraft.level != null) {
            Scoreboard scoreboard = this.minecraft.level.getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam(teamId);
            if (team != null) {
                Component displayName = team.getDisplayName();
                // Если displayName отличается от внутреннего имени команды (костыль, но часто displayName по умолчанию == name)
                if (displayName != null && !displayName.getString().equals(teamId)) {
                   return displayName;
                }
                // Если displayName есть и оно установлено пользователем (обычно оно отличается или стилизовано)
                if (displayName != null) {
                    return displayName;
                }
            }
        }

        String normalized = configuredName != null ? configuredName.trim() : "";
        if ("team1".equals(teamId)) {
            if (normalized.equalsIgnoreCase("team1") || normalized.isEmpty()) {
                return Component.translatable("wrb.team.tag.team1");
            }
        } else if ("team2".equals(teamId)) {
            if (normalized.equalsIgnoreCase("team2") || normalized.isEmpty()) {
                return Component.translatable("wrb.team.tag.team2");
            }
        }
        if (normalized.isEmpty()) {
            return Component.literal(teamId.toUpperCase());
        }
        return Component.literal(normalized);
    }

    private static class TacticalButton extends Button {
        private static final int COLOR_TACTICAL_BG = 0x80000000;
        private static final int COLOR_TACTICAL_BORDER = 0xFF4A4A4A;
        private static final int COLOR_TACTICAL_HOVER = 0xFF1A1A1A;
        private static final int COLOR_TEXT_NORMAL = 0xFFCCCCCC;
        private static final int COLOR_TEXT_HOVER = 0xFFFFFFFF;
        private static final int COLOR_ACCENT = 0xFFE67E22;

        public TacticalButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) {
                return;
            }

            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() 
                          && mouseX < this.getX() + this.width 
                          && mouseY < this.getY() + this.height;

            // Background
            int bgColor = this.isHovered ? COLOR_TACTICAL_HOVER : COLOR_TACTICAL_BG;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

            // Border
            int borderColor = this.isHovered ? COLOR_ACCENT : COLOR_TACTICAL_BORDER;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor); // Top
            graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor); // Bottom
            graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor); // Left
            graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor); // Right

            // Text
            int textColor = this.isHovered ? COLOR_TEXT_HOVER : COLOR_TEXT_NORMAL;
            graphics.drawCenteredString(
                Minecraft.getInstance().font,
                this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                textColor
            );
        }
    }
}

