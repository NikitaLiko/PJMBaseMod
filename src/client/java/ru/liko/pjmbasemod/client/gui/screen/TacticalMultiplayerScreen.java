package ru.liko.pjmbasemod.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;

public class TacticalMultiplayerScreen extends Screen {

    private final Screen lastScreen;
    private final ServerList servers;
    private EditBox searchBox;

    // Layout
    private static final int WINDOW_WIDTH = 700;
    private static final int WINDOW_HEIGHT = 450;
    private int windowX, windowY;

    public TacticalMultiplayerScreen(Screen lastScreen) {
        super(Component.literal("Multiplayer Session Browser"));
        this.lastScreen = lastScreen;
        this.servers = new ServerList(Minecraft.getInstance());
        this.servers.load();
    }

    @Override
    protected void init() {
        super.init();

        // Window centered
        // Since MINECRAFT usually runs at smaller GUI scales, 700 might be too wide.
        // We'll adapt to width - 100 or something if screen is too small.
        int w = Math.min(this.width - 40, WINDOW_WIDTH);
        int h = Math.min(this.height - 40, WINDOW_HEIGHT);

        this.windowX = (this.width - w) / 2;
        this.windowY = (this.height - h) / 2;

        int topRowY = windowY + 40;

        // Search Box
        this.searchBox = new EditBox(this.font, windowX + 20, topRowY, 200, 20, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search"));
        addRenderableWidget(this.searchBox);

        // Buttons: Filter, Refresh
        // Right aligned
        int btnWidth = 80;
        int rightX = windowX + w - 20;

        addRenderableWidget(Button.builder(Component.literal("Refresh"), (btn) -> refreshServers())
                .bounds(rightX - btnWidth, topRowY, btnWidth, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Filter"), (btn) -> {
        })
                .bounds(rightX - btnWidth * 2 - 10, topRowY, btnWidth, 20)
                .build());

        // Bottom: Create, Join Code, Find
        int bottomY = windowY + h - 35;

        // +Create (Greenish)
        addRenderableWidget(Button.builder(Component.literal("+Create"), (btn) -> {
            // Logic to create server? Or just open Singleplayer? Leave empty for now as
            // requested UI only
        }).bounds(windowX + 20, bottomY, 100, 20).build());

        // Invite Code Input (Visual only)
        EditBox codeBox = new EditBox(this.font, rightX - 160, bottomY, 100, 20, Component.literal("Code"));
        codeBox.setHint(Component.literal("Enter Invite Code..."));
        addRenderableWidget(codeBox);

        // Find
        addRenderableWidget(Button.builder(Component.literal("Find"), (btn) -> {
        })
                .bounds(rightX - 50, bottomY, 50, 20)
                .build());
    }

    private void refreshServers() {
        // Trigger server ping
        // This is complex to do properly without overriding JoinMultiplayerScreen
        // completely which handles the LanServerDetector and pinger.
        // For this task, we assume we just show the static list from ServerList
        this.servers.load();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 1. Dark overlay/Window
        // Main window bg
        int w = Math.min(this.width - 40, WINDOW_WIDTH);
        int h = Math.min(this.height - 40, WINDOW_HEIGHT);

        // Header Bar (Black)
        guiGraphics.fill(windowX, windowY, windowX + w, windowY + 30, 0xFF111111);
        // Header Text
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, windowY + 11, 0xFFFFFFFF);

        // Main Body (Dark Grey)
        guiGraphics.fill(windowX, windowY + 30, windowX + w, windowY + h, 0xEE1E1E1E);

        // Column Headers
        int listY = windowY + 70;
        int col1 = windowX + 20; // Name
        int col2 = windowX + w / 2 - 40; // Players
        int col3 = windowX + w / 2 + 40; // Map
        int col4 = windowX + w - 80; // Ping

        int headerColor = 0xFFAAAAAA;
        guiGraphics.drawString(this.font, "Session Name", col1, listY, headerColor);
        guiGraphics.drawString(this.font, "Players", col2, listY, headerColor);
        guiGraphics.drawString(this.font, "Active Map", col3, listY, headerColor);
        guiGraphics.drawString(this.font, "Ping (ms)", col4, listY, headerColor);

        // Separator Line
        guiGraphics.fill(windowX + 10, listY + 12, windowX + w - 10, listY + 13, 0xFF444444);

        // Render Server List Items
        // We'll just render the server list manually here for the visual
        int itemY = listY + 20;
        for (int i = 0; i < servers.size(); i++) {
            ServerData server = servers.get(i);
            renderServerEntry(guiGraphics, server, itemY, col1, col2, col3, col4);
            itemY += 20;
            if (itemY > windowY + h - 50)
                break; // Clip
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderServerEntry(GuiGraphics gg, ServerData server, int y, int c1, int c2, int c3, int c4) {
        // Name
        gg.drawString(this.font, server.name, c1, y, 0xFFFFFFFF);

        // Players (Using ping info as proxy or just placeholder)
        String players = server.ping < 0 ? "?/?" : "?";
        // Check if message is a component that contains formatted string
        if (server.status != null) {
            // ServerStatus available (1.20+)
            // But ServerData field is 'status'? No, it's just 'status' as component
            // usually? Use 'motd'
        }

        // Simpler: Just show "?" for players as standard ServerData doesn't expose it
        // easily field-wise in 1.21
        // without reflection or parsing the Component 'status'.
        gg.drawString(this.font, players, c2, y, 0xFFDDDDDD);

        // Map (We don't have this data in standard ServerData, so we leave blank or put
        // MOTD)
        String motd = server.motd != null ? server.motd.getString() : "";
        if (motd.length() > 20)
            motd = motd.substring(0, 20) + "...";
        gg.drawString(this.font, motd, c3, y, 0xFFDDDDDD);

        // Ping
        int ping = (int) server.ping;
        String pingText = ping < 0 ? "..." : ping + "";
        gg.drawString(this.font, pingText, c4, y, 0xFFFFFFFF);

        // Dot
        int color = ping < 0 ? 0xFF555555 : (ping < 100 ? 0xFF00FF00 : (ping < 200 ? 0xFFFFAA00 : 0xFFFF0000));
        gg.fill(c4 + 40, y + 2, c4 + 46, y + 8, color);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Blur background
        this.renderTransparentBackground(guiGraphics);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
