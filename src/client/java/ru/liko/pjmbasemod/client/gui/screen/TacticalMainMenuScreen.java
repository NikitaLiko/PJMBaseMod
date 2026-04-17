package ru.liko.pjmbasemod.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import ru.liko.pjmbasemod.Pjmbasemod;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.liko.pjmbasemod.common.init.PjmSounds;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TacticalMainMenuScreen extends Screen {

    // --- Layout Constants ---
    private static final int TOP_BAR_HEIGHT = 50;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 30;
    private static final int BUTTON_SPACING = 10;

    // --- Colors ---
    private static final int COLOR_ACCENT = 0xFFFFAA00; // Amber/Gold
    private static final int COLOR_TOP_BAR_BG = 0xDD000000; // Almost opaque black
    private static final int COLOR_HOVER = 0x44FFAA00;

    // --- Resources ---
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID,
            "textures/icon/pjm_512x512.png");

    // Background Slideshow
    private static final ResourceLocation[] BACKGROUNDS = {
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_1.png"),
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_2.png"),
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_3.png"),
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_4.png"),
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_5.png"),
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_6.png"),
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_7.png"),
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_8.png"),
            ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "textures/gui/menu_background_9.png")
    };
    private static final long SLIDE_DURATION = 10000L;
    private static final long FADE_DURATION = 2000L;

    // Actual texture dimensions for correct blit UV mapping (resized to 640x330)
    private static final int TEX_WIDTH = 640;
    private static final int TEX_HEIGHT = 330;

    // --- Texture Preloading ---
    // Preload one texture per render frame to avoid lag spikes during slideshow transitions.
    // Each blit() on a new ResourceLocation triggers synchronous PNG decode + GPU upload.
    private int preloadIndex = 0;
    private boolean allTexturesPreloaded = false;
    private long slideshowStartTime = 0L;

    // --- State ---
    private boolean isPlayMenuOpen = false;
    private long devMessageUntil = 0L; // For displaying "In Development" message
    private static SoundInstance menuMusicInstance;

    // --- Async Server Status (replaces blocking ServerStatusPinger) ---
    private static final String STATUS_SERVER_HOST = "pl1.hoxen.one";
    private static final int STATUS_SERVER_PORT = 25567;
    private static final long PING_INTERVAL_OK = 15_000L;     // 15s when server is online
    private static final long PING_INTERVAL_FAIL = 30_000L;   // 30s when server is offline (don't spam)
    private static final long INITIAL_PING_DELAY = 2_000L;    // 2s delay before first ping (don't block init)
    private static final int PING_TIMEOUT_MS = 3_000;         // 3s TCP connect timeout
    private final AtomicBoolean serverOnline = new AtomicBoolean(false);
    private final AtomicLong lastPingResult = new AtomicLong(-1); // -1 = unknown
    private long lastPingAttemptMs = 0L;
    private boolean pingInProgress = false;
    private long screenOpenedAt = 0L;

    // --- Widgets ---
    private final List<Button> playSubMenuButtons = new ArrayList<>();

    public TacticalMainMenuScreen() {
        super(Component.literal("Tactical Main Menu"));
    }

    @Override
    protected void init() {
        super.init();
        this.playSubMenuButtons.clear();

        // 1. Top Bar Buttons
        int topY = (TOP_BAR_HEIGHT - BUTTON_HEIGHT) / 2;

        // LOGO (Visual only, handled in render)
        // startX handling removed as it was unused

        // Title text "TEARDOWN" style but PROJ MC handled in render
        // startX handling removed as it was unused

        // Main Buttons: PLAY, MULTIPLAYER, CHARACTER, OPTIONS, QUIT
        // Centered options? Let's anchor them relative to the title or just center them
        // in the bar
        // We'll place them starting from center-ish to right.

        // Actually, let's put them in a row, centered horizontally.
        int totalButtonsWidth = (BUTTON_WIDTH * 5) + (BUTTON_SPACING * 4);
        int buttonsStartX = (this.width - totalButtonsWidth) / 2;
        // Adjust if it overlaps with logo (unlikely on 1080p, but on small screens yes)
        if (buttonsStartX < 180)
            buttonsStartX = 180;

        // PLAY Button
        Button btnPlay = createTopBarButton(buttonsStartX, topY, Component.translatable("menu.pjm.play"), button -> {
            togglePlayMenu();
        });
        addRenderableWidget(btnPlay);

        // MULTIPLAYER Button
        Button btnMulti = createTopBarButton(buttonsStartX + (BUTTON_WIDTH + BUTTON_SPACING), topY,
                Component.translatable("menu.pjm.multiplayer"), button -> {
                    this.minecraft.setScreen(new TacticalMultiplayerScreen(this));
                });
        addRenderableWidget(btnMulti);

        // CHARACTER Button
        Button btnChar = createTopBarButton(buttonsStartX + (BUTTON_WIDTH + BUTTON_SPACING) * 2, topY,
                Component.translatable("menu.pjm.character"), button -> {
                    // Show "In Development" message
                    devMessageUntil = System.currentTimeMillis() + 3000L;
                });
        addRenderableWidget(btnChar);

        // OPTIONS Button
        Button btnOpt = createTopBarButton(buttonsStartX + (BUTTON_WIDTH + BUTTON_SPACING) * 3, topY,
                Component.translatable("menu.pjm.options"), button -> {
                    this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
                });
        addRenderableWidget(btnOpt);

        // QUIT Button
        Button btnQuit = createTopBarButton(buttonsStartX + (BUTTON_WIDTH + BUTTON_SPACING) * 4, topY,
                Component.translatable("menu.pjm.exit"), button -> {
                    this.minecraft.stop();
                });
        addRenderableWidget(btnQuit);

        // 2. Play Sub-Menu Buttons (Hidden by default)
        // Position them below the "Play" button? Or a dedicated panel area?
        // User said "Play button slides out a menu". We can make it appear below the
        // top bar.
        int subMenuX = buttonsStartX; // Align with Play button
        int subMenuY = TOP_BAR_HEIGHT + 20;

        // Connect Main
        Button btnConnectMain = createSubMenuButton(subMenuX, subMenuY, Component.translatable("menu.pjm.connect_main"),
                button -> {
                    ServerData serverData = new ServerData("Project Minecraft Server", "pl1.hoxen.one:25567",
                            ServerData.Type.OTHER);
                    ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(serverData.ip),
                            serverData, false, null);
                });
        addRenderableWidget(btnConnectMain);
        playSubMenuButtons.add(btnConnectMain);
        subMenuY += BUTTON_HEIGHT + 5;

        // Connect RU
        Button btnConnectRU = createSubMenuButton(subMenuX, subMenuY, Component.translatable("menu.pjm.connect_ru"),
                button -> {
                    ServerData serverData = new ServerData("Project Minecraft RU Server", "213.152.43.50:25984",
                            ServerData.Type.OTHER);
                    ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(serverData.ip),
                            serverData, false, null);
                });
        addRenderableWidget(btnConnectRU);
        playSubMenuButtons.add(btnConnectRU);
        subMenuY += BUTTON_HEIGHT + 5;

        // Singleplayer
        Button btnSingle = createSubMenuButton(subMenuX, subMenuY, Component.translatable("menu.pjm.singleplayer"),
                button -> {
                    this.minecraft.setScreen(new SelectWorldScreen(this));
                });
        addRenderableWidget(btnSingle);
        playSubMenuButtons.add(btnSingle);

        // Initialize visibility
        updatePlayMenuVisibility();

        // Music (deferred to avoid blocking init)
        try {
            if (menuMusicInstance == null || !this.minecraft.getSoundManager().isActive(menuMusicInstance)) {
                menuMusicInstance = SimpleSoundInstance.forMusic(PjmSounds.MENU_MUSIC.get());
                this.minecraft.getSoundManager().play(menuMusicInstance);
            }
        } catch (Exception e) {
            // Sound not ready yet, will retry in tick()
        }

        // Server status ping is deferred — starts after INITIAL_PING_DELAY in tick()
        screenOpenedAt = System.currentTimeMillis();
    }

    private void togglePlayMenu() {
        isPlayMenuOpen = !isPlayMenuOpen;
        updatePlayMenuVisibility();
    }

    private void updatePlayMenuVisibility() {
        for (Button btn : playSubMenuButtons) {
            btn.visible = isPlayMenuOpen;
            btn.active = isPlayMenuOpen;
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Async server status ping — fully off render thread
        long now = System.currentTimeMillis();
        if (!pingInProgress && (now - screenOpenedAt > INITIAL_PING_DELAY)) {
            long interval = serverOnline.get() ? PING_INTERVAL_OK : PING_INTERVAL_FAIL;
            if (now - lastPingAttemptMs >= interval) {
                lastPingAttemptMs = now;
                pingInProgress = true;
                pingServerAsync();
            }
        }

        try {
            if (menuMusicInstance != null && !this.minecraft.getSoundManager().isActive(menuMusicInstance)) {
                menuMusicInstance = SimpleSoundInstance.forMusic(PjmSounds.MENU_MUSIC.get());
                this.minecraft.getSoundManager().play(menuMusicInstance);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Fully async TCP ping — runs DNS + connect on ForkJoinPool, never touches render thread.
     * Measures round-trip time via TCP handshake (SYN→SYN-ACK).
     */
    private void pingServerAsync() {
        CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(STATUS_SERVER_HOST, STATUS_SERVER_PORT), PING_TIMEOUT_MS);
                return System.currentTimeMillis() - start;
            } catch (Exception e) {
                return -1L;
            }
        }).thenAcceptAsync(pingMs -> {
            // Update state back on any thread — AtomicBoolean/AtomicLong are thread-safe
            serverOnline.set(pingMs >= 0);
            lastPingResult.set(pingMs);
            pingInProgress = false;
        });
    }

    // --- Animation State ---
    private float playMenuAnim = 0.0f;

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Disable vanilla background (blur)
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderSlideshowBackground(guiGraphics);

        // Update Play Menu Animation
        float targetAnim = isPlayMenuOpen ? 1.0f : 0.0f;

        // Fix for "jerky" close animation:
        float diff = targetAnim - this.playMenuAnim;
        if (Math.abs(diff) < 0.001f) {
            this.playMenuAnim = targetAnim;
        } else {
            // Lerp with frame independence factor logic
            this.playMenuAnim = this.playMenuAnim + diff * 0.25f;
        }

        boolean visible = this.playMenuAnim > 0.01f;

        // "Drawer" Slide -> Changed to Fade In
        int menuTotalHeight = (playSubMenuButtons.size() * (BUTTON_HEIGHT + 5)) + 15;

        // No slide offset (Static position, only fade)
        int slideOffset = 0;

        // Adjust this if you want a tiny movement (e.g. 5 pixels slide down)
        // int slideOffset = (int)((1.0f - this.playMenuAnim) * -10);

        int baseSubY = TOP_BAR_HEIGHT;

        for (int i = 0; i < playSubMenuButtons.size(); i++) {
            Button btn = playSubMenuButtons.get(i);
            btn.visible = visible;
            btn.active = visible;

            int listY = 30 + (i * (BUTTON_HEIGHT + 5));

            int currentY = baseSubY + slideOffset + listY;

            btn.setY(currentY);
            // Alpha for fade
            btn.setAlpha(this.playMenuAnim);
        }

        // Draw SubMenu BG
        if (visible) {
            // We can keep scissor to ensure it doesn't bleed over top bar if we added
            // offset later
            // But for pure fade it's fine.
            int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
            if (scale == 0)
                scale = 1;
            int sW = this.width * scale;

            int topBarPhysical = TOP_BAR_HEIGHT * scale;
            int sH = (this.height * scale) - topBarPhysical;
            if (sH < 0)
                sH = 0;

            RenderSystem.enableScissor(0, 0, sW, sH);

            if (!playSubMenuButtons.isEmpty()) {
                Button first = playSubMenuButtons.get(0);

                int bgY = baseSubY + slideOffset + 20;
                int bgH = menuTotalHeight;

                int startX = first.getX();
                int width = first.getWidth();
                int pad = 10;

                int alphaInt = (int) (this.playMenuAnim * 255);
                int bgAlpha = (int) (this.playMenuAnim * 170);

                int bgColor = (bgAlpha << 24) | 0x000000;
                int outlineColor = (alphaInt << 24) | (COLOR_ACCENT & 0x00FFFFFF);

                guiGraphics.fill(startX - pad, bgY, startX + width + pad, bgY + bgH, bgColor);
                guiGraphics.renderOutline(startX - pad, bgY, width + pad * 2, bgH, outlineColor);
            }

            RenderSystem.disableScissor();
        }

        // Top Bar Background
        guiGraphics.fill(0, 0, this.width, TOP_BAR_HEIGHT, COLOR_TOP_BAR_BG);
        guiGraphics.fill(0, TOP_BAR_HEIGHT, this.width, TOP_BAR_HEIGHT + 1, COLOR_ACCENT);

        // Logo / Title in Top Bar
        // Icon
        RenderSystem.enableBlend();
        guiGraphics.blit(ICON_TEXTURE, 10, 5, 40, 40, 0, 0, 512, 512, 512, 512);
        RenderSystem.disableBlend();

        // Text "PROJECT MINECRAFT"
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(1.2f, 1.2f, 1.2f);
        guiGraphics.drawString(this.font, "PROJECT", 48, 10, COLOR_ACCENT);
        guiGraphics.drawString(this.font, "MINECRAFT", 48, 20, 0xFFFFFFFF);
        guiGraphics.pose().popPose();

        // Render Widgets (Buttons)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // version Bottom Left
        String version = "ver. " + Pjmbasemod.MODID.toUpperCase() + " 0.1";
        guiGraphics.drawString(this.font, version, 10, this.height - 12, 0xAAFFFFFF);

        // Render Dev Message if active
        if (System.currentTimeMillis() < devMessageUntil) {
            String msg = "WORK IN PROGRESS";
            try {
                msg = Component.translatable("menu.pjm.in_dev").getString();
            } catch (Exception ignored) {
            }

            int textW = this.font.width(msg);
            int boxW = textW + 20;
            int boxH = 20;
            int boxX = (this.width - boxW) / 2;
            int boxY = TOP_BAR_HEIGHT + 60;

            long remaining = devMessageUntil - System.currentTimeMillis();
            int alpha = remaining < 500 ? (int) ((remaining / 500.0) * 255) : 255;
            int colorAccent = (alpha << 24) | (COLOR_ACCENT & 0x00FFFFFF);

            if (alpha > 5) {
                guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, (int) (alpha * 0.8) << 24);
                guiGraphics.renderOutline(boxX, boxY, boxW, boxH, colorAccent);
                guiGraphics.drawCenteredString(this.font, msg, this.width / 2, boxY + 6, colorAccent);
            }
        }

        // Server Status Bottom Right
        renderServerStatusBottomRight(guiGraphics);
    }

    private void renderServerStatusBottomRight(GuiGraphics gg) {
        boolean online = serverOnline.get();
        long ping = lastPingResult.get();

        int statusColor = online ? 0xFF00FF00 : 0xFFFF0000; // Green / Red
        String statusText;
        if (ping < 0 && lastPingAttemptMs == 0L) {
            statusText = "...";
            statusColor = 0xFFAAAAAA;
        } else {
            statusText = online ? "ONLINE" : "OFFLINE";
        }

        String label = "SERVER STATUS:";

        int x = this.width - 10;
        int y = this.height - 15;

        int statusW = this.font.width(statusText);
        gg.drawString(this.font, statusText, x - statusW, y, statusColor);

        int labelW = this.font.width(label);
        gg.drawString(this.font, label, x - statusW - labelW - 5, y, 0xFFAAAAAA);

        if (online && ping > 0) {
            String pingText = ping + " MS";
            String pingLabel = "PING:";
            int py = y - 12;

            int pingW = this.font.width(pingText);
            gg.drawString(this.font, pingText, x - pingW, py, 0xFFFFFFFF);

            int pLabelW = this.font.width(pingLabel);
            gg.drawString(this.font, pingLabel, x - pingW - pLabelW - 5, py, 0xFFAAAAAA);
        }
    }

    private Button createTopBarButton(int x, int y, Component text, Button.OnPress onPress) {
        String s = text.getString().toUpperCase();
        boolean isExit = s.contains("EXIT") || s.contains("ВЫХОД");
        boolean isPlay = s.contains("PLAY") || s.contains("ИГРАТЬ");

        TopBarButton btn = new TopBarButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, text, onPress, isExit, isPlay);
        if (isPlay) {
            btn.setExternalActiveCheck((v) -> this.isPlayMenuOpen);
        }
        return btn;
    }

    private Button createSubMenuButton(int x, int y, Component text, Button.OnPress onPress) {
        return new SubMenuButton(x, y, 200, BUTTON_HEIGHT, text, onPress);
    }

    // --- Custom Button Classes ---

    private static class TopBarButton extends Button {
        private float hoverAnim = 0.0f;
        private final boolean isExit;
        private final boolean isPlay;
        // Check function for external active state (e.g. if menu is open)
        public java.util.function.Function<Boolean, Boolean> isExternalActive = null;

        public TopBarButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean isExit,
                boolean isPlay) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.isExit = isExit;
            this.isPlay = isPlay;
        }

        public void setExternalActiveCheck(java.util.function.Function<Boolean, Boolean> check) {
            this.isExternalActive = check;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered();
            boolean externalActive = isExternalActive != null && isExternalActive.apply(true);

            // Logic: Highlighed if Hovered OR (Play Button AND Menu Open)
            boolean activeState = hovered || (isPlay && externalActive);

            // Animation Target
            float target = activeState ? 1.0f : 0.0f;
            this.hoverAnim = Mth.lerp(0.2f, this.hoverAnim, target);

            // Colors
            int targetColor;
            int baseBgColor;

            if (isExit) {
                targetColor = 0xFFFF0000; // Red
                baseBgColor = 0x44FF0000;
            } else if (isPlay) {
                targetColor = 0xFF00FF00; // Green
                baseBgColor = 0x4400FF00;
            } else {
                targetColor = COLOR_ACCENT; // Orange
                baseBgColor = COLOR_HOVER & 0x00FFFFFF;
            }

            // Pulse Effect
            float pulse = 0.0f;
            if (isPlay && activeState) {
                pulse = (float) (Math.sin(System.currentTimeMillis() / 150.0) + 1.0) * 0.5f;
            }

            int alphaMax = 68;
            if (isPlay && activeState) {
                alphaMax = 68 + (int) (pulse * 50);
            }

            int alpha = (int) (this.hoverAnim * alphaMax);
            int bgColor = (alpha << 24) | (baseBgColor & 0x00FFFFFF);

            // Text Color
            int r1 = 255, g1 = 255, b1 = 255;
            int r2 = (targetColor >> 16) & 0xFF;
            int g2 = (targetColor >> 8) & 0xFF;
            int b2 = (targetColor) & 0xFF;

            int r = (int) Mth.lerp(this.hoverAnim, r1, r2);
            int g = (int) Mth.lerp(this.hoverAnim, g1, g2);
            int b = (int) Mth.lerp(this.hoverAnim, b1, b2);

            if (isPlay && activeState) {
                int shine = (int) (pulse * 100);
                r = Math.min(255, r + shine);
                g = Math.min(255, g + shine);
                b = Math.min(255, b + shine);
            }

            int textColor = (0xFF << 24) | (r << 16) | (g << 8) | b;

            // Border Color
            int idleBorderColor = 0x44FFFFFF;
            int br1 = (idleBorderColor >> 16) & 0xFF;
            int bg1 = (idleBorderColor >> 8) & 0xFF;
            int bb1 = (idleBorderColor) & 0xFF;
            int ba1 = (idleBorderColor >> 24) & 0xFF;

            int br2 = r2;
            int bg2 = g2;
            int bb2 = b2;
            int ba2 = 255;

            int br = (int) Mth.lerp(this.hoverAnim, br1, br2);
            int bgg = (int) Mth.lerp(this.hoverAnim, bg1, bg2);
            int bb = (int) Mth.lerp(this.hoverAnim, bb1, bb2);
            int ba = (int) Mth.lerp(this.hoverAnim, ba1, ba2);

            int borderColor = (ba << 24) | (br << 16) | (bgg << 8) | bb;

            // Render
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Highlight Line
            int lineAlpha = (int) (this.hoverAnim * 255);
            int lineColor = (lineAlpha << 24) | (targetColor & 0x00FFFFFF);

            if (lineAlpha > 5) {
                guiGraphics.fill(getX(), getY() + height - 2, getX() + width, getY() + height, lineColor);
                if (isPlay && activeState) {
                    int glowAlpha = (int) (pulse * 150);
                    int glowColor = (glowAlpha << 24) | (targetColor & 0x00FFFFFF);
                    guiGraphics.fill(getX(), getY(), getX() + width, getY() + 2, glowColor);
                }
            }

            // Outline
            guiGraphics.renderOutline(getX(), getY(), width, height, borderColor);

            if (isPlay && activeState) {
                int glowBorderAlpha = (int) (pulse * 100);
                int glowBorderColor = (glowBorderAlpha << 24) | (targetColor & 0x00FFFFFF);
                guiGraphics.renderOutline(getX() + 1, getY() + 1, width - 2, height - 2, glowBorderColor);
            }

            guiGraphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, textColor);
        }
    }

    private static class SubMenuButton extends Button {
        private float hoverAnim = 0.0f;

        public SubMenuButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered();
            float target = hovered ? 1.0f : 0.0f;
            this.hoverAnim = Mth.lerp(0.25f, this.hoverAnim, target);

            // Background
            int r1 = 0x22, g1 = 0x22, b1 = 0x22, a1 = 0xDD;
            int r2 = (COLOR_ACCENT >> 16) & 0xFF;
            int g2 = (COLOR_ACCENT >> 8) & 0xFF;
            int b2 = (COLOR_ACCENT) & 0xFF;
            int a2 = 0xFF;

            int r = (int) Mth.lerp(this.hoverAnim, r1, r2);
            int g = (int) Mth.lerp(this.hoverAnim, g1, g2);
            int b = (int) Mth.lerp(this.hoverAnim, b1, b2);
            int a = (int) Mth.lerp(this.hoverAnim, a1, a2);

            int bgColor = (a << 24) | (r << 16) | (g << 8) | b;

            if (this.alpha < 1.0f) {
                bgColor = (((int) ((bgColor >>> 24) * this.alpha)) << 24) | (bgColor & 0x00FFFFFF);
            }

            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Text color logic can be simpler or same as before
            int tr = (int) Mth.lerp(this.hoverAnim, 255, 0);
            int textColor = (0xFF << 24) | (tr << 16) | (tr << 8) | tr;
            if (this.alpha < 1.0f) {
                textColor = (((int) ((textColor >>> 24) * this.alpha)) << 24) | (textColor & 0x00FFFFFF);
            }

            // Left Indicator
            if (this.hoverAnim < 0.9f) {
                int indAlpha = (int) ((1.0f - this.hoverAnim) * 255);
                if (this.alpha < 1.0f)
                    indAlpha = (int) (indAlpha * this.alpha);
                int indColor = (indAlpha << 24) | (COLOR_ACCENT & 0x00FFFFFF);
                guiGraphics.fill(getX(), getY(), getX() + 4, getY() + height, indColor);
            }

            guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font, getMessage(), getX() + 15,
                    getY() + (height - 8) / 2, textColor, false);
        }
    }

    /**
     * Preloads background textures one per frame to avoid lag spikes.
     * Each blit() call on a new texture triggers synchronous PNG decode + GPU upload.
     * By spreading this across frames, no single frame gets blocked.
     */
    private void preloadNextTexture(GuiGraphics guiGraphics) {
        if (allTexturesPreloaded || preloadIndex >= BACKGROUNDS.length) {
            allTexturesPreloaded = true;
            slideshowStartTime = System.currentTimeMillis();
            return;
        }

        // Render a 1x1 pixel offscreen to force texture load into GPU memory
        try {
            guiGraphics.blit(BACKGROUNDS[preloadIndex], -1, -1, 1, 1, 0.0f, 0.0f, 1, 1, TEX_WIDTH, TEX_HEIGHT);
        } catch (Exception ignored) {
        }
        preloadIndex++;

        if (preloadIndex >= BACKGROUNDS.length) {
            allTexturesPreloaded = true;
            slideshowStartTime = System.currentTimeMillis();
        }
    }

    private void renderSlideshowBackground(GuiGraphics guiGraphics) {
        try {
            // Phase 1: preload textures (1 per frame), show first background as static
            if (!allTexturesPreloaded) {
                preloadNextTexture(guiGraphics);
                // Show first background while preloading the rest
                RenderSystem.disableBlend();
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                guiGraphics.blit(BACKGROUNDS[0], 0, 0, this.width, this.height, 0.0f, 0.0f, TEX_WIDTH, TEX_HEIGHT,
                        TEX_WIDTH, TEX_HEIGHT);
                return;
            }

            // Phase 2: all textures in GPU memory — slideshow runs lag-free
            long elapsed = System.currentTimeMillis() - slideshowStartTime;
            long totalLoop = SLIDE_DURATION * BACKGROUNDS.length;
            long pos = elapsed % totalLoop;
            int currentIndex = (int) (pos / SLIDE_DURATION);
            int nextIndex = (currentIndex + 1) % BACKGROUNDS.length;
            long timeInSlide = pos % SLIDE_DURATION;

            RenderSystem.disableBlend();
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            guiGraphics.blit(BACKGROUNDS[currentIndex], 0, 0, this.width, this.height, 0.0f, 0.0f, TEX_WIDTH, TEX_HEIGHT,
                    TEX_WIDTH, TEX_HEIGHT);

            if (timeInSlide > (SLIDE_DURATION - FADE_DURATION)) {
                float alpha = (float) (timeInSlide - (SLIDE_DURATION - FADE_DURATION)) / FADE_DURATION;
                if (alpha > 1.0f)
                    alpha = 1.0f;

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
                guiGraphics.blit(BACKGROUNDS[nextIndex], 0, 0, this.width, this.height, 0.0f, 0.0f, TEX_WIDTH, TEX_HEIGHT,
                        TEX_WIDTH, TEX_HEIGHT);
                RenderSystem.disableBlend();
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            // Fallback: black background if texture loading fails
            guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
        }
    }
}
