package ru.liko.pjmbasemod.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import com.mojang.blaze3d.platform.Lighting;
import ru.liko.pjmbasemod.common.KitDefinition;
import ru.liko.pjmbasemod.client.ClientKitsCache;
import ru.liko.pjmbasemod.common.player.PjmRank;
import ru.liko.pjmbasemod.common.customization.CustomizationManager;
import ru.liko.pjmbasemod.common.customization.CustomizationOption;
import ru.liko.pjmbasemod.common.customization.CustomizationType;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.OpenTeamSelectionPacket;
import ru.liko.pjmbasemod.common.network.packet.RefillAmmunitionPacket;
import ru.liko.pjmbasemod.common.network.packet.SelectClassPacket;
import ru.liko.pjmbasemod.common.network.packet.SelectCustomizationPacket;
import ru.liko.pjmbasemod.common.player.PjmPlayerClass;
import ru.liko.pjmbasemod.common.player.PjmAttachments;
import ru.liko.pjmbasemod.common.player.PjmPlayerData;
import ru.liko.pjmbasemod.common.util.ItemParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

/**
 * Redesigned Class Selection Screen with Tabs (Deployment / Customization).
 * Squad/Arma style: Dark, clean, responsive.
 */
public class ClassSelectionScreen extends Screen {
    // Colors
    private static final int COLOR_BACKGROUND = 0xFF000000; // Черный фон
    private static final int COLOR_PANEL = 0xFF121212;
    private static final int COLOR_PANEL_BORDER = 0xFF2B2B2B;
    private static final int COLOR_ORANGE_ACCENT = 0xFFE67E22;
    private static final int COLOR_BLUE_ACCENT = 0xFF3498DB;
    private static final int COLOR_WHITE_TEXT = 0xFFFFFFFF;
    private static final int COLOR_GREY_TEXT = 0xFFB0B0B0;
    private static final int COLOR_GREEN = 0xFF27AE60;
    private static final int COLOR_RED = 0xFFE74C3C;
    private static final int COLOR_CARD_BG = 0x90101010;
    private static final int COLOR_CARD_HOVER = 0xFF1E1E1E;
    private static final int COLOR_CARD_LOCKED = 0x55101010;

    // Dimensions constants
    private static final int TAB_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 40;
    private static final int PADDING = 10;
    
    // Class Card constants
    private static final int CLASS_CARD_MIN_WIDTH = 150;
    private static final int CLASS_CARD_MIN_HEIGHT = 60; // Fixed height for consistency
    private static final int CLASS_CARD_SPACING = 8;
    
    private static final ResourceLocation CLASS_ICON = ResourceLocation.fromNamespaceAndPath("pjmbasemod", "textures/icon/class.png");
    
    // Loadout constants
    private static final int LOADOUT_ROW_HEIGHT = 28;
    private static final int LOADOUT_ICON_SIZE = 20;
    private static final int LOADOUT_ROW_SPACING = 2;
    private static final int CLASS_SCROLL_STEP = 20;
    private static final int LOADOUT_SCROLL_STEP = 20;


    // State
    private enum Tab { DEPLOYMENT, CUSTOMIZATION }
    private Tab currentTab = Tab.DEPLOYMENT;
    private final PjmPlayerClass currentClass;
    private final Map<String, Integer> classLimits;
    private final Map<String, Integer> classPlayerCounts;
    private final String playerTeamName;
    private final Set<String> lockedClasses;
    
    // Tooltip State
    private String hoveredTooltip = null;
    private int tooltipX = 0;
    private int tooltipY = 0;
    
    private PjmPlayerClass selectedClass;
    private KitDefinition selectedKit;
    private final List<ClassCard> classCards = new ArrayList<>();
    
    // Customization State
    private CustomizationType selectedCustomizationType = CustomizationType.SKIN;
    private int customizationScrollOffset = 0;
    private int maxCustomizationScroll = 0;
    
    // 3D Model State
    private float modelRotation = 180;
    private boolean isDraggingModel = false;
    private float lastDragX;
    
    // Pending State
    private CustomizationOption pendingSkinOption;
    public static CustomizationOption previewSkin; // Exposed for Mixin

    // Layout variables
    private int contentX, contentY, contentWidth, contentHeight;
    private int classPanelWidth, detailsPanelWidth, loadoutPanelWidth;
    
    // Scrolling
    private int classScrollOffset;
    private int maxClassScroll;
    private int loadoutScrollOffset;
    private int maxLoadoutScroll;
    private int loadoutListHeight;
    private int loadoutContentHeight;
    
    // Kit List Scrolling
    private int kitScrollOffset;
    private int maxKitScroll;

    // Animation
    private float fadeIn = 0.0f;
    private long initTime;

    public ClassSelectionScreen(PjmPlayerClass currentClass,
                                Map<String, Integer> classLimits,
                                Map<String, Integer> classPlayerCounts,
                                String playerTeamName) {
        this(currentClass, classLimits, classPlayerCounts, playerTeamName, new HashSet<>());
    }

    public ClassSelectionScreen(PjmPlayerClass currentClass,
                                Map<String, Integer> classLimits,
                                Map<String, Integer> classPlayerCounts,
                                String playerTeamName,
                                Set<String> lockedClasses) {
        super(Component.translatable("wrb.class.title"));
        this.currentClass = currentClass;
        this.classLimits = classLimits;
        this.classPlayerCounts = classPlayerCounts;
        this.playerTeamName = playerTeamName;
        this.lockedClasses = lockedClasses != null ? lockedClasses : new HashSet<>();
        this.selectedClass = determineInitialSelection();
        this.initTime = System.currentTimeMillis();
    }

    private PjmPlayerClass determineInitialSelection() {
        if (currentClass != null && currentClass.isSelectable() && !lockedClasses.contains(currentClass.getId())) {
            return currentClass;
        }
        for (PjmPlayerClass playerClass : PjmPlayerClass.values()) {
            if (playerClass != PjmPlayerClass.NONE && playerClass.isSelectable() && !lockedClasses.contains(playerClass.getId())) {
                return playerClass;
            }
        }
        return PjmPlayerClass.ASSAULT;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        computeLayout();
        
        // Add Footer Buttons
        int buttonWidth = 120;
        int buttonHeight = 24;
        int buttonsY = this.height - FOOTER_HEIGHT + (FOOTER_HEIGHT - buttonHeight) / 2;
        int rightX = this.width - PADDING - buttonWidth;
        
        // Confirm Button
        this.addRenderableWidget(new TacticalButton(rightX, buttonsY, buttonWidth, buttonHeight, 
            Component.translatable("wrb.class.confirm"), 
            btn -> confirmSelection()));
            
        // Refill Button
        rightX -= (buttonWidth + PADDING);
        this.addRenderableWidget(new TacticalButton(rightX, buttonsY, buttonWidth, buttonHeight, 
            Component.translatable("wrb.class.refill_ammunition"), 
            btn -> refillAmmunition()));
            
        // Change Team Button
        int leftX = PADDING;
        this.addRenderableWidget(new TacticalButton(leftX, buttonsY, 140, buttonHeight, 
            Component.translatable("wrb.team.selection.change_team"), 
            btn -> changeTeam()));

        // Add Tab Buttons (Top Left)
        int tabY = PADDING;
        int tabX = PADDING;
        int tabWidth = 120;
        
        this.addRenderableWidget(new TabButton(tabX, tabY, tabWidth, TAB_HEIGHT, 
            Component.translatable("wrb.customization.tab.deployment"), 
            Tab.DEPLOYMENT, 
            this::setTab));
            
        tabX += tabWidth + 4;
        this.addRenderableWidget(new TabButton(tabX, tabY, tabWidth, TAB_HEIGHT, 
            Component.translatable("wrb.customization.tab.customization"), 
            Tab.CUSTOMIZATION, 
            this::setTab));
            
        buildClassCards();
    }

    private void setTab(Tab tab) {
        this.currentTab = tab;
    }

    private void computeLayout() {
        // Content area below tabs and above footer
        contentY = PADDING + TAB_HEIGHT + PADDING;
        contentHeight = this.height - contentY - FOOTER_HEIGHT;
        contentX = PADDING;
        contentWidth = this.width - PADDING * 2;
        
        // Deployment Layout: 3 Columns [Classes 25%] [Details 45%] [Loadout 30%]
        classPanelWidth = (int)(contentWidth * 0.25f);
        loadoutPanelWidth = (int)(contentWidth * 0.30f);
        detailsPanelWidth = contentWidth - classPanelWidth - loadoutPanelWidth - (PADDING * 2);
    }
    
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Отключаем ванильный блюр
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        renderBackdrop(graphics);
        
        // Fade in
        long elapsed = System.currentTimeMillis() - initTime;
        fadeIn = Math.min(1.0f, elapsed / 500.0f);
        
        // Render current tab content
        if (currentTab == Tab.DEPLOYMENT) {
            renderDeploymentTab(graphics, mouseX, mouseY);
        } else {
            renderCustomizationTab(graphics, mouseX, mouseY);
        }
        
        // Render Header/Tabs/Footer background details if needed (though buttons are widgets)
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Render tooltip last (on top of everything)
        if (hoveredTooltip != null) {
            renderAAA_Tooltip(graphics, hoveredTooltip, tooltipX, tooltipY);
        }
        
        // Reset tooltip for next frame
        hoveredTooltip = null;
    }
    
    private void renderBackdrop(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
        // Header separator
        int headerBottom = PADDING + TAB_HEIGHT + PADDING / 2;
        graphics.fill(0, headerBottom, this.width, headerBottom + 1, COLOR_PANEL_BORDER);
        // Footer separator
        int footerTop = this.height - FOOTER_HEIGHT;
        graphics.fill(0, footerTop, this.width, footerTop + 1, COLOR_PANEL_BORDER);
    }

    private void renderDeploymentTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = contentX;
        // 1. Class Selection Panel (Left)
        renderSectionFrame(graphics, x, contentY, classPanelWidth, contentHeight, getTeamAccentColor());
        renderClassList(graphics, x, contentY, classPanelWidth, contentHeight, mouseX, mouseY);
        
        x += classPanelWidth + PADDING;
        // 2. Details Panel (Center) - Now includes Kit Selection
        renderSectionFrame(graphics, x, contentY, detailsPanelWidth, contentHeight, getTeamAccentColor());
        renderClassDetailsAndKits(graphics, x, contentY, detailsPanelWidth, contentHeight, mouseX, mouseY);
        
        x += detailsPanelWidth + PADDING;
        // 3. Loadout Panel (Right)
        renderSectionFrame(graphics, x, contentY, loadoutPanelWidth, contentHeight, getSecondaryAccentColor());
        renderLoadout(graphics, x, contentY, loadoutPanelWidth, contentHeight, mouseX, mouseY);
    }

    private void renderClassList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Header
        int headerHeight = 24;
        
        // Icon
        graphics.blit(CLASS_ICON, x + 8, y + 4, 0, 0, 16, 16, 16, 16);
        
        graphics.drawString(this.font, Component.translatable("wrb.class.available"), x + 28, y + 8, COLOR_GREY_TEXT, false);
        
        int listY = y + headerHeight;
        int listHeight = height - headerHeight - 4;
        
        // Render Cards
        if (classCards.isEmpty()) return;
        
        // Update Scroll bounds
        int totalContentHeight = (classCards.size() * (CLASS_CARD_MIN_HEIGHT + CLASS_CARD_SPACING));
        maxClassScroll = Math.max(0, totalContentHeight - listHeight);
        classScrollOffset = Mth.clamp(classScrollOffset, 0, maxClassScroll);
        
        graphics.enableScissor(x + 4, listY, x + width - 4, listY + listHeight);
        
        int currentY = listY - classScrollOffset;
        for (ClassCard card : classCards) {
            // Update card position relative to container
            card.x = x + 8;
            card.y = currentY;
            card.width = width - 16;
            card.height = CLASS_CARD_MIN_HEIGHT;
            
            if (currentY + card.height > listY && currentY < listY + listHeight) {
                card.render(graphics, mouseX, mouseY);
            }
            
            currentY += card.height + CLASS_CARD_SPACING;
        }
        
        graphics.disableScissor();
        
        // Scrollbar
        renderScrollBar(graphics, x + width - 6, listY, 4, listHeight, classScrollOffset, maxClassScroll);
    }

    private void renderClassDetailsAndKits(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        int padding = 16;
        int currentY = y + padding;
        
        // --- Class Header ---
        // Title
        graphics.pose().pushPose();
        graphics.pose().translate(x + padding, currentY, 0);
        graphics.pose().scale(1.5f, 1.5f, 1.5f);
        graphics.drawString(this.font, selectedClass.getDisplayName(), 0, 0, COLOR_WHITE_TEXT, false);
        graphics.pose().popPose();
        currentY += 24;
        
        // Subtitle/Role
        graphics.drawString(this.font, Component.translatable("wrb.class.subtitle"), x + padding, currentY, COLOR_GREY_TEXT, false);
        currentY += 20;
        
        // Badges/Tags
        int badgeX = x + padding;
        
        // Team Tag
        badgeX += drawBadge(graphics, badgeX, currentY, Component.translatable("wrb.class.team", getTeamDisplayName()), getTeamAccentColor()) + 6;
        
        // Limit Tag
        int limit = classLimits.getOrDefault(selectedClass.getId(), 0);
        int current = classPlayerCounts.getOrDefault(selectedClass.getId(), 0);
        int limitColor = (limit > 0 && current >= limit) ? COLOR_RED : COLOR_GREEN;
        Component limitText = limit > 0 
            ? Component.translatable("wrb.class.limit", current, limit) 
            : Component.translatable("wrb.class.unlimited");
        badgeX += drawBadge(graphics, badgeX, currentY, limitText, limitColor) + 6;
        
        if (selectedClass == currentClass) {
            badgeX += drawBadgeEnhanced(graphics, badgeX, currentY, Component.translatable("wrb.class.current"), getSecondaryAccentColor()) + 6;
        }
        
        currentY += 24;
        
        // Description
        List<String> descLines = wrapText(selectedClass.getDescription().getString(), width - (padding * 2));
        for (String line : descLines) {
            graphics.drawString(this.font, line, x + padding, currentY, COLOR_GREY_TEXT, false);
            currentY += 12;
        }
        currentY += 12;
        
        // Separator
        graphics.fill(x + padding, currentY, x + width - padding, currentY + 1, 0xFF333333);
        currentY += 12;
        
        // --- Kit Selection Area ---
        graphics.drawString(this.font, Component.translatable("wrb.class.kits_title").getString(), x + padding, currentY, COLOR_WHITE_TEXT, false);
        currentY += 16;
        
        int kitsListY = currentY;
        int kitsListHeight = (y + height) - kitsListY - padding;
        
        renderKitList(graphics, x + padding, kitsListY, width - (padding * 2), kitsListHeight, mouseX, mouseY);
    }

    private void renderKitList(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        List<KitDefinition> kits = ClientKitsCache.getKits(selectedClass.getId(), resolveTeamName());
        
        if (kits.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("wrb.class.no_kits").getString(), x, y, COLOR_GREY_TEXT, false);
            return;
        }
        
        int cardHeight = 40;
        int spacing = 4;
        int contentH = kits.size() * (cardHeight + spacing);
        
        maxKitScroll = Math.max(0, contentH - height);
        kitScrollOffset = Mth.clamp(kitScrollOffset, 0, maxKitScroll);
        
        graphics.enableScissor(x, y, x + width, y + height);
        
        int currentY = y - kitScrollOffset;
        
        // Get player rank for locking logic
        PjmRank playerRank = PjmRank.PRIVATE;
        if (this.minecraft.player != null) {
            PjmPlayerData data = this.minecraft.player.getData(PjmAttachments.PLAYER_DATA);
            playerRank = data.getRank();
        }
        
        for (KitDefinition kit : kits) {
            if (currentY + cardHeight > y && currentY < y + height) {
                boolean isLocked = playerRank.ordinal() < kit.getMinRank().ordinal();
                boolean isSelected = (selectedKit != null && selectedKit.getId().equals(kit.getId()));
                boolean isHovered = mouseX >= x && mouseX < x + width && mouseY >= currentY && mouseY < currentY + cardHeight;
                
                renderKitCard(graphics, kit, x, currentY, width, cardHeight, isSelected, isLocked, isHovered, mouseX, mouseY);
            }
            currentY += cardHeight + spacing;
        }
        
        graphics.disableScissor();
        
        renderScrollBar(graphics, x + width + 2, y, 4, height, kitScrollOffset, maxKitScroll);
    }
    
    private void renderKitCard(GuiGraphics graphics, KitDefinition kit, int x, int y, int width, int height, boolean isSelected, boolean isLocked, boolean isHovered, int mouseX, int mouseY) {
        int bgColor;
        if (isLocked) {
            bgColor = 0x44101010; // Dark dimmed
        } else if (isSelected) {
            bgColor = 0xFF2C3E50; // Selected blue-ish
        } else if (isHovered) {
            bgColor = 0xFF1E1E1E; // Hover
        } else {
            bgColor = 0xFF121212; // Normal
        }
        
        graphics.fill(x, y, x + width, y + height, bgColor);
        
        int borderColor = isSelected ? COLOR_BLUE_ACCENT : (isLocked ? COLOR_RED : 0xFF333333);
        graphics.renderOutline(x, y, width, height, borderColor);
        
        // Name
        int textColor = isLocked ? COLOR_GREY_TEXT : COLOR_WHITE_TEXT;
        graphics.drawString(this.font, kit.getDisplayName(), x + 6, y + 6, textColor, false);
        
        // Rank Requirement (с иконкой)
        int rankColor = isLocked ? COLOR_RED : COLOR_GREEN;
        PjmRank minRank = kit.getMinRank();
        String rankText = minRank.getDisplayName().getString();
        if (isLocked) {
            rankText = Component.translatable("wrb.class.kit_locked", rankText).getString();
        }
        
        // Отрисовка иконки звания
        ResourceLocation rankIcon = minRank.getIconLocation();
        if (rankIcon != null) {
            // Рендерим иконку 10x10 перед текстом
            graphics.blit(rankIcon, x + 6, y + 21, 0, 0, 10, 10, 10, 10);
            // Сдвигаем текст вправо
            graphics.drawString(this.font, rankText, x + 20, y + 22, rankColor, false);
        } else {
            graphics.drawString(this.font, rankText, x + 6, y + 22, rankColor, false);
        }
        
        // Selection Indicator
        if (isSelected) {
            graphics.drawString(this.font, "✓", x + width - 16, y + 12, COLOR_GREEN, false);
        } else if (isLocked) {
            graphics.drawString(this.font, "🔒", x + width - 16, y + 12, COLOR_RED, false);
        }
        
        // Tooltip on hover
        if (isHovered && !isLocked) {
            String tooltipText = kit.getDisplayName() + "\n" + Component.translatable("wrb.class.kit_rank_required", kit.getMinRank().getDisplayName().getString()).getString();
            ClassSelectionScreen.this.setTooltip(tooltipText, mouseX, mouseY);
        }
    }

    private void renderLoadout(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        int padding = 12;
        graphics.drawString(this.font, Component.translatable("wrb.class.equipment"), x + padding, y + padding, COLOR_GREY_TEXT, false);
        
        int listY = y + 30;
        int listHeight = height - 40;
        
        List<String> items;
        if (selectedKit != null) {
            items = selectedKit.getItems();
        } else {
            items = ru.liko.pjmbasemod.client.ClientKitsCache.getKitItemStrings(selectedClass.getId(), resolveTeamName());
        }
        
        if (items.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("wrb.class.loadout.empty"), x + padding, listY, COLOR_GREY_TEXT, false);
            return;
        }
        
        loadoutContentHeight = items.size() * (LOADOUT_ROW_HEIGHT + LOADOUT_ROW_SPACING);
        maxLoadoutScroll = Math.max(0, loadoutContentHeight - listHeight);
        loadoutScrollOffset = Mth.clamp(loadoutScrollOffset, 0, maxLoadoutScroll);
        
        graphics.enableScissor(x + padding, listY, x + width - padding, listY + listHeight);
        
        int rowY = listY - loadoutScrollOffset;
        for (int i = 0; i < items.size(); i++) {
            if (rowY + LOADOUT_ROW_HEIGHT > listY && rowY < listY + listHeight) {
                renderLoadoutItem(graphics, items.get(i), x + padding, rowY, width - (padding * 2), i % 2 == 0);
            }
            rowY += LOADOUT_ROW_HEIGHT + LOADOUT_ROW_SPACING;
        }
        
        graphics.disableScissor();
        
        renderScrollBar(graphics, x + width - 6, listY, 4, listHeight, loadoutScrollOffset, maxLoadoutScroll);
    }


    private void renderLoadoutItem(GuiGraphics graphics, String itemString, int x, int y, int width, boolean alternate) {
        int bg = alternate ? 0xFF181818 : 0xFF121212;
        graphics.fill(x, y, x + width, y + LOADOUT_ROW_HEIGHT, bg);
        
        Optional<ItemParser.ParsedItem> parsedOpt = ItemParser.parseItemStackWithSlot(itemString);
        if (parsedOpt.isPresent()) {
            ItemStack stack = parsedOpt.get().stack;
            if (stack != null && !stack.isEmpty()) {
                graphics.renderItem(stack, x + 4, y + 6);
                String name = stack.getHoverName().getString();
                if (stack.getCount() > 1) name += " x" + stack.getCount();
                graphics.drawString(this.font, shortenForWidth(name, width - 30), x + 24, y + 10, COLOR_WHITE_TEXT, false);
            }
        } else {
            graphics.drawString(this.font, shortenForWidth(itemString, width - 10), x + 4, y + 10, COLOR_GREY_TEXT, false);
        }
    }

    // ============ CUSTOMIZATION TAB ============

    private void renderCustomizationTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = contentX;
        // 1. Types Panel (Left)
        int typesWidth = 150;
        renderSectionFrame(graphics, x, contentY, typesWidth, contentHeight, getTeamAccentColor());
        renderCustomizationTypes(graphics, x, contentY, typesWidth, contentHeight);
        
        x += typesWidth + PADDING;
        
        // 2. Main Content (Center)
        int contentW = contentWidth - typesWidth - PADDING;
        renderSectionFrame(graphics, x, contentY, contentW, contentHeight, getTeamAccentColor());
        
        if (selectedCustomizationType == CustomizationType.SKIN) {
            renderSkinSelection(graphics, x, contentY, contentW, contentHeight, mouseX, mouseY);
        } else {
            renderCustomizationOptions(graphics, x, contentY, contentW, contentHeight, mouseX, mouseY);
        }
    }
    
    @Override
    public void onClose() {
        previewSkin = null;
        super.onClose();
    }

    private void renderSkinSelection(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Find current skin options
        CustomizationManager manager = CustomizationManager.getClientInstance();
        List<CustomizationOption> allOptions = manager.getOptionsByType(CustomizationType.SKIN);
        String currentTeam = resolveTeamName();
        List<CustomizationOption> options = new ArrayList<>();
        
        for (CustomizationOption opt : allOptions) {
            if (opt.getRequiredTeam() == null || opt.getRequiredTeam().isEmpty() || 
                normalize(opt.getRequiredTeam()).equals(normalize(currentTeam))) {
                options.add(opt);
            }
        }
        
        if (options.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("wrb.customization.no_skins"), x + width / 2, y + height / 2, COLOR_GREY_TEXT);
            return;
        }
        
        // Determine current pending index
        int currentIndex = -1;
        
        // Ensure pendingSkinOption is valid
        if (this.pendingSkinOption != null) {
             for (int i = 0; i < options.size(); i++) {
                 if (options.get(i).getId().equals(this.pendingSkinOption.getId())) {
                     currentIndex = i;
                     break;
                 }
             }
        }
        
        // Fallback if pending not in list (e.g. wrong team) or null
        if (currentIndex == -1) {
            if (this.minecraft.player != null) {
                PjmPlayerData data = this.minecraft.player.getData(PjmAttachments.PLAYER_DATA);
                String activeId = data.getActiveSkinId();
                for (int i = 0; i < options.size(); i++) {
                    if (options.get(i).getId().equals(activeId)) {
                        currentIndex = i;
                        this.pendingSkinOption = options.get(i);
                        break;
                    }
                }
            }
            // Still -1?
            if (currentIndex == -1 && !options.isEmpty()) {
                currentIndex = 0;
                this.pendingSkinOption = options.get(0);
            }
        }
        
        // Update static preview for Mixin
        previewSkin = this.pendingSkinOption;
        
        // Render 3D Model
        int modelX = x + width / 2;
        int modelY = y + height - 90; // Moved up slightly more
        int scale = height / 3;
        
        renderPlayerModel(graphics, modelX, modelY, scale, mouseX, mouseY);
        
        // Render Controls (Arrows and Name)
        int controlsY = y + height - 70; // Adjusted
        
        // Current skin name
        String skinName = currentIndex >= 0 ? options.get(currentIndex).getDisplayName() : Component.translatable("wrb.customization.default_skin").getString();
        graphics.drawCenteredString(this.font, skinName, x + width / 2, controlsY, COLOR_WHITE_TEXT);
        
        // Arrows
        // Left Arrow
        int arrowSize = 20;
        int arrowY = controlsY - 5;
        int leftArrowX = x + width / 2 - 80;
        int rightArrowX = x + width / 2 + 60;
        
        boolean hoverLeft = mouseX >= leftArrowX && mouseX < leftArrowX + arrowSize && mouseY >= arrowY && mouseY < arrowY + arrowSize;
        boolean hoverRight = mouseX >= rightArrowX && mouseX < rightArrowX + arrowSize && mouseY >= arrowY && mouseY < arrowY + arrowSize;
        
        int colorLeft = hoverLeft ? COLOR_ORANGE_ACCENT : COLOR_WHITE_TEXT;
        int colorRight = hoverRight ? COLOR_ORANGE_ACCENT : COLOR_WHITE_TEXT;
        
        graphics.drawString(this.font, "<", leftArrowX + 6, arrowY + 6, colorLeft, false);
        graphics.drawString(this.font, ">", rightArrowX + 6, arrowY + 6, colorRight, false);
        
        // Interaction hint
        graphics.drawCenteredString(this.font, Component.translatable("wrb.customization.rotate_hint"), x + width / 2, y + 20, 0xFF666666);
        
        // APPLY BUTTON
        int btnWidth = 100;
        int btnHeight = 20;
        int btnX = x + width / 2 - btnWidth / 2;
        int btnY = controlsY + 30;
        
        boolean isApplied = false;
        if (this.minecraft.player != null && this.pendingSkinOption != null) {
            PjmPlayerData data = this.minecraft.player.getData(PjmAttachments.PLAYER_DATA);
            isApplied = data.getActiveSkinId().equals(this.pendingSkinOption.getId());
        }
        
        boolean btnHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;
        
        if (!isApplied) {
            int btnColor = btnHovered ? COLOR_GREEN : 0xFF333333;
            int btnBorder = 0xFF444444;
            int btnText = 0xFFFFFFFF;
            
            graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
            graphics.renderOutline(btnX, btnY, btnWidth, btnHeight, btnBorder);
            graphics.drawCenteredString(this.font, Component.translatable("wrb.customization.apply"), btnX + btnWidth / 2, btnY + 6, btnText);
        } else {
            graphics.drawCenteredString(this.font, Component.translatable("wrb.customization.applied"), btnX + btnWidth / 2, btnY + 6, COLOR_GREEN);
        }
    }

    private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
        if (this.minecraft.player == null) return;
        
        // Manual rendering to support custom rotation
        PoseStack posestack = graphics.pose();
        posestack.pushPose();
        posestack.translate((float)x, (float)y, 50.0F);
        posestack.scale((float)scale, (float)scale, -(float)scale); // Flip Y
        
        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float)Math.PI);
        Quaternionf quaternionf1 = (new Quaternionf()).rotateX(0); // No vertical tilt for now
        quaternionf.mul(quaternionf1);
        posestack.mulPose(quaternionf);
        
        float f2 = this.minecraft.player.yBodyRot;
        float f3 = this.minecraft.player.getYRot();
        float f4 = this.minecraft.player.getXRot();
        float f5 = this.minecraft.player.yHeadRotO;
        float f6 = this.minecraft.player.yHeadRot;
        
        // Apply our custom rotation
        float rot = modelRotation;
        
        this.minecraft.player.yBodyRot = rot;
        this.minecraft.player.setYRot(rot);
        this.minecraft.player.setXRot(0);
        this.minecraft.player.yHeadRot = this.minecraft.player.getYRot();
        this.minecraft.player.yHeadRotO = this.minecraft.player.getYRot();
        
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternionf1.conjugate();
        entityrenderdispatcher.overrideCameraOrientation(quaternionf1);
        entityrenderdispatcher.setRenderShadow(false);
        
        // Render
        net.minecraft.client.renderer.MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        entityrenderdispatcher.render(this.minecraft.player, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, posestack, buffer, 15728880);
        buffer.endBatch();
        
        entityrenderdispatcher.setRenderShadow(true);
        this.minecraft.player.yBodyRot = f2;
        this.minecraft.player.setYRot(f3);
        this.minecraft.player.setXRot(f4);
        this.minecraft.player.yHeadRotO = f5;
        this.minecraft.player.yHeadRot = f6;
        
        posestack.popPose();
        Lighting.setupFor3DItems();
    }


    private void renderCustomizationTypes(GuiGraphics graphics, int x, int y, int width, int height) {
        int padding = 10;
        int currentY = y + padding;
        
        graphics.drawString(this.font, Component.translatable("wrb.customization.category").getString(), x + padding, currentY, COLOR_GREY_TEXT, false);
        currentY += 20;
        
        for (CustomizationType type : CustomizationType.values()) {
            boolean selected = (type == selectedCustomizationType);
            int color = selected ? getTeamAccentColor() : 0xFF333333;
            int textColor = selected ? COLOR_WHITE_TEXT : COLOR_GREY_TEXT;
            
            // Draw simplified button
            graphics.fill(x + padding, currentY, x + width - padding, currentY + 24, color);
            graphics.drawString(this.font, Component.translatable(getCustomizationTypeKey(type)).getString(), x + padding + 6, currentY + 8, textColor, false);
            
            // Click handling is done in mouseClicked, but we visualize here
            currentY += 28;
        }
    }

    private void renderCustomizationOptions(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        CustomizationManager manager = CustomizationManager.getClientInstance();
        List<CustomizationOption> allOptions = manager.getOptionsByType(selectedCustomizationType);
        
        // Filter by team
        String currentTeam = resolveTeamName();
        List<CustomizationOption> options = new ArrayList<>();
        for (CustomizationOption opt : allOptions) {
            if (opt.getRequiredTeam() == null || opt.getRequiredTeam().isEmpty() || 
                normalize(opt.getRequiredTeam()).equals(normalize(currentTeam))) {
                options.add(opt);
            }
        }
        
        int padding = 12;
        graphics.drawString(this.font, Component.translatable("wrb.customization.options", Component.translatable(getCustomizationTypeKey(selectedCustomizationType)).getString()).getString(), x + padding, y + padding, COLOR_WHITE_TEXT, false);
        
        if (options.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("wrb.customization.no_options").getString(), x + padding, y + 40, COLOR_GREY_TEXT, false);
            return;
        }

        int listY = y + 40;
        int listHeight = height - 50;
        
        // Grid layout
        int cardWidth = 140;
        int cardHeight = 40;
        int spacing = 8;
        int columns = Math.max(1, (width - padding * 2) / (cardWidth + spacing));
        int totalRows = (int)Math.ceil((double)options.size() / columns);
        int contentH = totalRows * (cardHeight + spacing);
        
        maxCustomizationScroll = Math.max(0, contentH - listHeight);
        customizationScrollOffset = Mth.clamp(customizationScrollOffset, 0, maxCustomizationScroll);
        
        graphics.enableScissor(x, listY, x + width, listY + listHeight);
        
        int startRow = customizationScrollOffset / (cardHeight + spacing);
        int rowOffsetY = -(customizationScrollOffset % (cardHeight + spacing));
        
        int currentY = listY + rowOffsetY;
        
        for (int row = startRow; row < totalRows; row++) {
            if (currentY > listY + listHeight) break;
            
            for (int col = 0; col < columns; col++) {
                int index = row * columns + col;
                if (index >= options.size()) break;
                
                CustomizationOption option = options.get(index);
                int cardX = x + padding + col * (cardWidth + spacing);
                
                renderCustomizationCard(graphics, option, cardX, currentY, cardWidth, cardHeight, mouseX, mouseY);
            }
            currentY += cardHeight + spacing;
        }
        
        graphics.disableScissor();
        
        renderScrollBar(graphics, x + width - 6, listY, 4, listHeight, customizationScrollOffset, maxCustomizationScroll);
    }
    
    private void renderCustomizationCard(GuiGraphics graphics, CustomizationOption option, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Check if selected
        boolean isSelected = false;
        if (this.minecraft.player != null) {
            PjmPlayerData data = this.minecraft.player.getData(PjmAttachments.PLAYER_DATA);
            if (option.getType() == CustomizationType.SKIN) {
                isSelected = option.getId().equals(data.getActiveSkinId());
            } else {
                isSelected = data.getActiveItemIds().contains(option.getId());
            }
        }

        int bgColor = isSelected ? 0xFF2C3E50 : (hovered ? 0xFF333333 : 0xFF1E1E1E);
        int borderColor = isSelected ? COLOR_BLUE_ACCENT : 0xFF444444;
        
        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.renderOutline(x, y, width, height, borderColor);
        
        graphics.drawString(this.font, shortenForWidth(option.getDisplayName(), width - 10), x + 6, y + 6, COLOR_WHITE_TEXT, false);
        graphics.drawString(this.font, shortenForWidth(option.getValue(), width - 10), x + 6, y + 20, COLOR_GREY_TEXT, false);
    }

    // ============ UTILS & HELPERS ============

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab == Tab.DEPLOYMENT) {
            // Check class cards (Left Panel)
            if (mouseX >= contentX && mouseX <= contentX + classPanelWidth) {
                if (mouseY >= contentY + 24 && mouseY <= contentY + contentHeight) {
                    for (ClassCard card : classCards) {
                        if (card.isMouseOver((int)mouseX, (int)mouseY)) {
                            if (!card.locked) {
                                selectClass(card.playerClass);
                                return true;
                            }
                        }
                    }
                }
            }
            
            // Check kit cards (Center Panel)
            int centerPanelX = contentX + classPanelWidth + PADDING;
            if (mouseX >= centerPanelX && mouseX <= centerPanelX + detailsPanelWidth) {
                List<KitDefinition> kits = ClientKitsCache.getKits(selectedClass.getId(), resolveTeamName());
                int padding = 16;
                // Calculate start Y of kit list (matching renderClassDetailsAndKits)
                int classHeaderHeight = 24 + 20 + 24 + (wrapText(selectedClass.getDescription().getString(), detailsPanelWidth - (padding * 2)).size() * 12) + 12 + 12 + 16;
                int startY = contentY + padding + classHeaderHeight;
                
                int currentY = startY - kitScrollOffset;
                int cardHeight = 40;
                int spacing = 4;
                
                PjmRank playerRank = PjmRank.PRIVATE;
                if (this.minecraft.player != null) {
                    PjmPlayerData data = this.minecraft.player.getData(PjmAttachments.PLAYER_DATA);
                    playerRank = data.getRank();
                }
                
                for (KitDefinition kit : kits) {
                    if (mouseY >= currentY && mouseY <= currentY + cardHeight && mouseY >= startY) {
                        // Используем hasPermissions для проверки на клиенте вместо серверного PjmPermissions
                        boolean isLocked = playerRank.ordinal() < kit.getMinRank().ordinal() && !this.minecraft.player.hasPermissions(2);
                        if (!isLocked) {
                            this.selectedKit = kit;
                            this.loadoutScrollOffset = 0;
                            return true;
                        }
                    }
                    currentY += cardHeight + spacing;
                }
            }
        } else if (currentTab == Tab.CUSTOMIZATION) {
            int x = contentX;
            int typesWidth = 150;
            int padding = 10;
            
            // Check Type Buttons
            if (mouseX >= x + padding && mouseX <= x + typesWidth - padding) {
                int startY = contentY + padding + 20;
                for (CustomizationType type : CustomizationType.values()) {
                    if (mouseY >= startY && mouseY <= startY + 24) {
                        selectedCustomizationType = type;
                        customizationScrollOffset = 0;
                        return true;
                    }
                    startY += 28;
                }
            }
            
            // Handle SKIN selection specific clicks (Arrows & Model Drag start & Apply)
            if (selectedCustomizationType == CustomizationType.SKIN) {
                int contentW = contentWidth - typesWidth - PADDING;
                int centerX = x + typesWidth + PADDING + contentW / 2;
                int controlsY = contentY + contentHeight - 70; // Adjusted to match renderSkinSelection
                int arrowY = controlsY - 5;
                int leftArrowX = centerX - 80;
                int rightArrowX = centerX + 60;
                int arrowSize = 20;
                
                // Left Arrow
                if (mouseX >= leftArrowX && mouseX < leftArrowX + arrowSize && mouseY >= arrowY && mouseY < arrowY + arrowSize) {
                    changeSkinSelection(-1);
                    return true;
                }
                
                // Right Arrow
                if (mouseX >= rightArrowX && mouseX < rightArrowX + arrowSize && mouseY >= arrowY && mouseY < arrowY + arrowSize) {
                    changeSkinSelection(1);
                    return true;
                }
                
                // Apply Button
                int btnWidth = 100;
                int btnHeight = 20;
                int btnX = centerX - btnWidth / 2;
                int btnY = controlsY + 30;
                if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight) {
                    if (this.pendingSkinOption != null) {
                        applySkinSelection();
                    }
                    return true;
                }
                
                // Start Model Drag
                if (mouseX >= x + typesWidth + PADDING && mouseX < x + typesWidth + PADDING + contentW &&
                    mouseY >= contentY && mouseY < contentY + contentHeight - 90) { // Reduced height for controls
                    isDraggingModel = true;
                    lastDragX = (float)mouseX;
                    return true;
                }
            }
            
            // Check Options (Only for ITEM type now, or generic grid)
            if (selectedCustomizationType == CustomizationType.ITEM) {
                int gridX = x + typesWidth + PADDING;
                int gridWidth = contentWidth - typesWidth - PADDING;
                if (mouseX >= gridX && mouseX <= gridX + gridWidth && mouseY >= contentY + 40 && mouseY <= contentY + contentHeight - 10) {
                     CustomizationManager manager = CustomizationManager.getClientInstance();
                     List<CustomizationOption> allOptions = manager.getOptionsByType(selectedCustomizationType);
                     
                     // Filter by team
                     String currentTeam = resolveTeamName();
                     List<CustomizationOption> options = new ArrayList<>();
                     for (CustomizationOption opt : allOptions) {
                         if (opt.getRequiredTeam() == null || opt.getRequiredTeam().isEmpty() || 
                             normalize(opt.getRequiredTeam()).equals(normalize(currentTeam))) {
                             options.add(opt);
                         }
                     }
    
                     int cardWidth = 140;
                     int cardHeight = 40;
                     int spacing = 8;
                     int columns = Math.max(1, (gridWidth - 12 * 2) / (cardWidth + spacing));
                     
                     int listY = contentY + 40;
                     int relativeY = (int)mouseY - listY + customizationScrollOffset;
                     
                     // If clicked inside list area
                     if (relativeY >= 0) {
                         int row = relativeY / (cardHeight + spacing);
                         // Check if click is in spacing
                         if (relativeY % (cardHeight + spacing) <= cardHeight) {
                             int relativeX = (int)mouseX - (gridX + 12);
                             int col = relativeX / (cardWidth + spacing);
                             if (relativeX >= 0 && relativeX % (cardWidth + spacing) <= cardWidth) {
                                 int index = row * columns + col;
                                 if (index >= 0 && index < options.size()) {
                                     CustomizationOption option = options.get(index);
                                     toggleCustomization(option);
                                     return true;
                                 }
                             }
                         }
                     }
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingModel && selectedCustomizationType == CustomizationType.SKIN) {
            float deltaX = (float)mouseX - lastDragX;
            modelRotation += deltaX;
            lastDragX = (float)mouseX;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingModel = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    private void changeSkinSelection(int direction) {
        CustomizationManager manager = CustomizationManager.getClientInstance();
        List<CustomizationOption> allOptions = manager.getOptionsByType(CustomizationType.SKIN);
        String currentTeam = resolveTeamName();
        List<CustomizationOption> options = new ArrayList<>();
        
        for (CustomizationOption opt : allOptions) {
            if (opt.getRequiredTeam() == null || opt.getRequiredTeam().isEmpty() || 
                normalize(opt.getRequiredTeam()).equals(normalize(currentTeam))) {
                options.add(opt);
            }
        }
        
        if (options.isEmpty()) return;
        
        int currentIndex = -1;
        // Check pending first
        if (this.pendingSkinOption != null) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getId().equals(this.pendingSkinOption.getId())) {
                    currentIndex = i;
                    break;
                }
            }
        } else if (this.minecraft.player != null) {
            PjmPlayerData data = this.minecraft.player.getData(PjmAttachments.PLAYER_DATA);
            String activeId = data.getActiveSkinId();
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getId().equals(activeId)) {
                    currentIndex = i;
                    break;
                }
            }
        }
        
        int nextIndex = currentIndex + direction;
        
        // Wrap around
        // Case: No selection (-1) -> Next (0), Prev (last)
        if (currentIndex == -1) {
            previewSkin = this.pendingSkinOption;
            if (direction > 0) nextIndex = 0;
            else nextIndex = options.size() - 1;
        } else {
            if (nextIndex >= options.size()) nextIndex = 0;
            if (nextIndex < 0) nextIndex = options.size() - 1;
        }
        
        if (nextIndex >= 0 && nextIndex < options.size()) {
            this.pendingSkinOption = options.get(nextIndex);
            // Don't send packet here anymore, wait for apply
        }
    }
    
    private void applySkinSelection() {
        if (this.pendingSkinOption != null) {
             PjmNetworking.sendToServer(new SelectCustomizationPacket(this.pendingSkinOption.getId(), true, true));
        }
    }
    
    private void toggleCustomization(CustomizationOption option) {
        boolean isSelected = false;
        if (this.minecraft.player != null) {
            PjmPlayerData data = this.minecraft.player.getData(PjmAttachments.PLAYER_DATA);
            if (option.getType() == CustomizationType.SKIN) {
                isSelected = option.getId().equals(data.getActiveSkinId());
            } else {
                isSelected = data.getActiveItemIds().contains(option.getId());
            }
        }
        
        // If Skin: Select if not selected (cannot deselect, must pick another)
        // If Item: Toggle
        boolean newState = true;
        if (option.getType() == CustomizationType.ITEM) {
            newState = !isSelected;
        } else if (isSelected) {
            return; // Already selected skin
        }
        
        PjmNetworking.sendToServer(new SelectCustomizationPacket(option.getId(), option.getType() == CustomizationType.SKIN, newState));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = scrollY;
        if (currentTab == Tab.DEPLOYMENT) {
             // Check bounds
            if (mouseX < contentX + classPanelWidth) {
                classScrollOffset = (int)Mth.clamp(classScrollOffset - delta * CLASS_SCROLL_STEP, 0, maxClassScroll);
                return true;
            } else if (mouseX > contentX + classPanelWidth + detailsPanelWidth + PADDING * 2) {
                loadoutScrollOffset = (int)Mth.clamp(loadoutScrollOffset - delta * LOADOUT_SCROLL_STEP, 0, maxLoadoutScroll);
                return true;
            }
        } else {
            if (mouseX > contentX + 150) {
                customizationScrollOffset = (int)Mth.clamp(customizationScrollOffset - delta * CLASS_SCROLL_STEP, 0, maxCustomizationScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderSectionFrame(GuiGraphics graphics, int x, int y, int width, int height, int accentColor) {
        int bgBase = tintWithAccent(COLOR_PANEL, accentColor, 0.05f);
        int borderBase = tintWithAccent(COLOR_PANEL_BORDER, accentColor, 0.1f);
        
        graphics.fill(x, y, x + width, y + height, withAlpha(bgBase, (int)(fadeIn * 240)));
        graphics.renderOutline(x, y, width, height, withAlpha(borderBase, (int)(fadeIn * 255)));
        
        // Accent top bar
        graphics.fill(x, y, x + width, y + 2, withAlpha(accentColor, (int)(fadeIn * 200)));
    }
    
    private void renderScrollBar(GuiGraphics graphics, int x, int y, int width, int height, int offset, int max) {
        if (max <= 0) return;
        graphics.fill(x, y, x + width, y + height, 0x40000000);
        
        int thumbHeight = Math.max(16, (int)((float)height * (height / (float)(height + max))));
        int trackHeight = height - thumbHeight;
        int thumbY = y + (int)(trackHeight * (offset / (float)max));
        
        graphics.fill(x, thumbY, x + width, thumbY + thumbHeight, 0xFF666666);
    }
    
    private int interpolateColor(int from, int to, float factor) {
        float clamped = Mth.clamp(factor, 0.0f, 1.0f);
        int a1 = (from >> 24) & 0xFF;
        int r1 = (from >> 16) & 0xFF;
        int g1 = (from >> 8) & 0xFF;
        int b1 = from & 0xFF;

        int a2 = (to >> 24) & 0xFF;
        int r2 = (to >> 16) & 0xFF;
        int g2 = (to >> 8) & 0xFF;
        int b2 = to & 0xFF;

        int a = (int)(a1 + (a2 - a1) * clamped);
        int r = (int)(r1 + (r2 - r1) * clamped);
        int g = (int)(g1 + (g2 - g1) * clamped);
        int b = (int)(b1 + (b2 - b1) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private int tintWithAccent(int baseColor, int accentColor, float factor) {
        return interpolateColor(baseColor, accentColor, factor);
    }

    private int getTeamAccentColor() {
        String normalized = normalize(resolveTeamName());
        if (normalized.equals(normalize(ru.liko.pjmbasemod.client.ClientTeamConfig.getTeam1Name()))) {
            return COLOR_RED;
        }
        if (normalized.equals(normalize(ru.liko.pjmbasemod.client.ClientTeamConfig.getTeam2Name()))) {
            return COLOR_BLUE_ACCENT;
        }
        return COLOR_ORANGE_ACCENT;
    }
    
    private int getSecondaryAccentColor() {
        return interpolateColor(getTeamAccentColor(), COLOR_WHITE_TEXT, 0.35f);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
    
    private String getCustomizationTypeKey(CustomizationType type) {
        return switch (type) {
            case SKIN -> "wrb.customization.type.skin";
            case ITEM -> "wrb.customization.type.item";
        };
    }
    
    private String resolveTeamName() {
        if (playerTeamName != null && !playerTeamName.isBlank()) {
            return playerTeamName;
        }
        String fallback = ru.liko.pjmbasemod.client.ClientTeamConfig.getTeam1Name();
        return fallback != null ? fallback : "team1";
    }
    
    /**
     * Получает отображаемое имя команды (не ID, а настроенное имя)
     * Преобразует ID команды игрока в отображаемое имя из scoreboard
     */
    private String getTeamDisplayName() {
        // resolveTeamName() возвращает ID команды игрока (например, "belogoria")
        String teamId = resolveTeamName();
        
        // ClientTeamConfig хранит маппинг ID -> DisplayName
        // Преобразуем ID в отображаемое имя (например, "Белогория")
        return ru.liko.pjmbasemod.client.ClientTeamConfig.getDisplayName(teamId);
    }

    private int drawBadge(GuiGraphics graphics, int x, int y, Component text, int color) {
        String s = text.getString();
        int w = this.font.width(s) + 8;
        graphics.fill(x, y, x + w, y + 14, color);
        graphics.drawString(this.font, s, x + 4, y + 3, COLOR_WHITE_TEXT, false);
        return w;
    }
    
    /**
     * Улучшенный бейдж с темным фоном и яркой обводкой для лучшей видимости
     */
    private int drawBadgeEnhanced(GuiGraphics graphics, int x, int y, Component text, int color) {
        String s = text.getString();
        int w = this.font.width(s) + 12;
        int h = 16;
        
        // Темный полупрозрачный фон для контраста
        graphics.fill(x, y, x + w, y + h, 0xDD000000);
        
        // Яркая цветная обводка (2px)
        graphics.renderOutline(x, y, w, h, color);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, color); // Верхняя линия
        
        // Текст с тенью для еще большей читаемости
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1);
        graphics.drawString(this.font, s, x + 6, y + 5, COLOR_WHITE_TEXT, true);
        graphics.pose().popPose();
        
        return w;
    }
    
    /**
     * Подсказка в стиле AAA игр (Squad, Battlefield, Arma)
     * Темный фон с яркой обводкой и тенью
     */
    private void renderAAA_Tooltip(GuiGraphics graphics, String text, int x, int y) {
        if (text == null || text.isEmpty()) return;
        
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\n")) {
            lines.add(line);
        }
        
        if (lines.isEmpty()) return;
        
        // Вычисляем размеры
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, this.font.width(line));
        }
        
        int padding = 8;
        int tooltipWidth = maxWidth + padding * 2;
        int lineHeight = 11;
        int tooltipHeight = lines.size() * lineHeight + padding * 2 + 4; // +4 для нижней границы
        
        // Корректируем позицию, чтобы не выходить за границы экрана
        int tooltipX = x + 12;
        int tooltipY = y - 8;
        
        // Максимальная нижняя граница - не заходить на футер с кнопками
        int maxBottomY = this.height - FOOTER_HEIGHT - 4;
        
        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = x - tooltipWidth - 12;
        }
        if (tooltipY + tooltipHeight > maxBottomY) {
            tooltipY = maxBottomY - tooltipHeight;
        }
        if (tooltipY < 0) {
            tooltipY = 4;
        }
        
        // Внешняя яркая рамка (2px толщина) - рисуем ПЕРВОЙ
        int borderColor = 0xFFAAAAAA; // Светло-серая рамка
        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 2, borderColor); // Верх
        graphics.fill(tooltipX, tooltipY + tooltipHeight - 2, tooltipX + tooltipWidth, tooltipY + tooltipHeight, borderColor); // Низ
        graphics.fill(tooltipX, tooltipY, tooltipX + 2, tooltipY + tooltipHeight, borderColor); // Лево
        graphics.fill(tooltipX + tooltipWidth - 2, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, borderColor); // Право
        
        // Основной темный фон (внутри рамки)
        graphics.fill(tooltipX + 2, tooltipY + 2, tooltipX + tooltipWidth - 2, tooltipY + tooltipHeight - 2, 0xF0101010);
        
        // Градиентная верхняя полоса (акцент)
        int accentColor = getTeamAccentColor();
        graphics.fill(tooltipX + 2, tooltipY + 2, tooltipX + tooltipWidth - 2, tooltipY + 4, accentColor);
        
        // Внутренняя акцентная обводка для "свечения"
        int glowColor = withAlpha(accentColor, 80);
        graphics.fill(tooltipX + 3, tooltipY + 4, tooltipX + tooltipWidth - 3, tooltipY + 5, glowColor); // Верх внутренний
        graphics.fill(tooltipX + 3, tooltipY + 4, tooltipX + 4, tooltipY + tooltipHeight - 3, glowColor); // Лево внутренний
        
        // Рендерим текст
        int textY = tooltipY + padding;
        for (String line : lines) {
            graphics.drawString(this.font, line, tooltipX + padding, textY, COLOR_WHITE_TEXT, false);
            textY += lineHeight;
        }
    }
    
    /**
     * Устанавливает подсказку для отображения в следующем кадре
     */
    private void setTooltip(String text, int mouseX, int mouseY) {
        this.hoveredTooltip = text;
        this.tooltipX = mouseX;
        this.tooltipY = mouseY;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }
    
    private String shortenForWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (this.font.width(text) <= maxWidth) return text;
        return this.font.plainSubstrByWidth(text, maxWidth - 6) + "...";
    }

    private void buildClassCards() {
        classCards.clear();
        for (PjmPlayerClass playerClass : PjmPlayerClass.values()) {
            if (playerClass == PjmPlayerClass.NONE) continue;
            
            boolean lockedByPermission = lockedClasses.contains(playerClass.getId());
            boolean locked = !playerClass.isSelectable() || lockedByPermission;
            
            // Пропускаем классы, которые доступны только для Team1, если игрок в другой команде
            if (playerClass.isTeam1Only()) {
                String currentTeam = resolveTeamName();
                String team1Id = ru.liko.pjmbasemod.client.ClientTeamConfig.getTeam1Name();
                if (currentTeam == null || !currentTeam.equalsIgnoreCase(team1Id)) {
                    continue; // Не показываем этот класс вообще
                }
            }
            
            classCards.add(new ClassCard(playerClass, locked, lockedByPermission));
        }
    }
    
    private void selectClass(PjmPlayerClass playerClass) {
        this.selectedClass = playerClass;
        this.loadoutScrollOffset = 0;
        // Инициализируем выбранный кит первым доступным для этого класса
        List<KitDefinition> kits = ClientKitsCache.getKits(playerClass.getId(), resolveTeamName());
        this.selectedKit = kits.isEmpty() ? null : kits.get(0);
        this.kitScrollOffset = 0;
    }
    
    private void confirmSelection() {
        if (selectedClass != null && selectedClass.isSelectable() && selectedKit != null) {
            PjmNetworking.sendToServer(new SelectClassPacket(selectedClass.getId(), selectedKit.getId()));
            this.onClose();
        }
    }

    private void refillAmmunition() {
        PjmNetworking.sendToServer(new RefillAmmunitionPacket());
        this.onClose();
    }
    
    private void changeTeam() {
        if (this.minecraft != null && this.minecraft.player != null) {
            PjmNetworking.sendToServer(OpenTeamSelectionPacket.create(
                Map.of("team1", 0, "team2", 0),
                ru.liko.pjmbasemod.client.ClientTeamConfig.getTeam1Name(),
                ru.liko.pjmbasemod.client.ClientTeamConfig.getTeam2Name(),
                true,
                ru.liko.pjmbasemod.client.ClientTeamConfig.getBalanceThreshold()
            ));
        }
    }
    
    @Override
    public boolean shouldCloseOnEsc() { return true; }
    
    @Override
    public boolean isPauseScreen() { return false; }
    
    // Helper Classes
    
    private class ClassCard {
        final PjmPlayerClass playerClass;
        final boolean locked;
        final boolean lockedByPermission;
        int x, y, width, height; // Bounds for mouse detection
        
        ClassCard(PjmPlayerClass playerClass, boolean locked, boolean lockedByPermission) {
            this.playerClass = playerClass;
            this.locked = locked;
            this.lockedByPermission = lockedByPermission;
        }
        
        void render(GuiGraphics graphics, int mouseX, int mouseY) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            boolean isSelected = (playerClass == selectedClass);
            
            int bgColor = isSelected ? getTeamAccentColor() : (hovered ? COLOR_CARD_HOVER : COLOR_CARD_BG);
            if (isSelected) bgColor = withAlpha(bgColor, 200);
            else if (hovered) bgColor = withAlpha(bgColor, 180);
            else bgColor = withAlpha(bgColor, 120);
            
            // Рамка (1px толщина)
            int borderColor = isSelected ? 0xFFFFFFFF : COLOR_PANEL_BORDER;
            graphics.fill(x, y, x + width, y + 1, borderColor); // Верх
            graphics.fill(x, y + height - 1, x + width, y + height, borderColor); // Низ
            graphics.fill(x, y, x + 1, y + height, borderColor); // Лево
            graphics.fill(x + width - 1, y, x + width, y + height, borderColor); // Право
            
            // Фон внутри рамки
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, bgColor);
            
            // Name
            graphics.drawString(ClassSelectionScreen.this.font, playerClass.getDisplayName(), x + 6, y + 8, COLOR_WHITE_TEXT, false);
            
            // Status
            int limit = classLimits.getOrDefault(playerClass.getId(), 0);
            int current = classPlayerCounts.getOrDefault(playerClass.getId(), 0);
            String status = limit > 0 ? current + "/" + limit : "∞";
            int statusColor = (limit > 0 && current >= limit) ? COLOR_RED : COLOR_GREEN;
            if (locked) {
                status = Component.translatable("wrb.customization.locked").getString();
                statusColor = COLOR_RED;
            }
            graphics.drawString(ClassSelectionScreen.this.font, status, x + width - ClassSelectionScreen.this.font.width(status) - 6, y + 6, statusColor, false);
            
            // Description Preview
            String desc = shortenForWidth(playerClass.getDescription().getString(), width - 12);
            graphics.drawString(ClassSelectionScreen.this.font, desc, x + 6, y + 20, COLOR_GREY_TEXT, false);
            
            // Tooltip on hover - короткая версия в стиле AAA игр
            if (hovered && !locked) {
                String tooltipText = playerClass.getDisplayName().getString();
                if (limit > 0) {
                    tooltipText += "\n" + Component.translatable("wrb.class.limit", current, limit).getString();
                }
                ClassSelectionScreen.this.setTooltip(tooltipText, mouseX, mouseY);
            }
        }
        
        boolean isMouseOver(int mouseX, int mouseY) {
             return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private static class TacticalButton extends Button {
        public TacticalButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            this.isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
            int bgColor = isHovered ? 0xFFE67E22 : 0xFF222222;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
            graphics.renderOutline(getX(), getY(), width, height, 0xFF444444);
            int textColor = isHovered ? 0xFFFFFFFF : 0xFFCCCCCC;
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
        }
    }
    
    private class TabButton extends Button {
        private final Tab tab;
        private final java.util.function.Consumer<Tab> onSelect;
        
        public TabButton(int x, int y, int width, int height, Component message, Tab tab, java.util.function.Consumer<Tab> onSelect) {
            super(x, y, width, height, message, btn -> onSelect.accept(tab), DEFAULT_NARRATION);
            this.tab = tab;
            this.onSelect = onSelect;
        }
        
        @Override
        public void onPress() {
            onSelect.accept(tab);
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            
             this.isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
             boolean isActive = (tab == currentTab);
             
             int bgColor = isActive ? 0xFF333333 : 0xFF111111;
             int textColor = isActive ? 0xFFFFFFFF : 0xFFAAAAAA;
             int outlineColor = (isActive || isHovered) ? 0xFF666666 : 0xFF333333;
             
             graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
             graphics.renderOutline(getX(), getY(), width, height, outlineColor);
             
             if (isActive) {
                 graphics.fill(getX(), getY() + height - 2, getX() + width, getY() + height, 0xFFE67E22);
             }
             
             graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
        }
    }
}
