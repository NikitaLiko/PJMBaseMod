package ru.liko.pjmbasemod.client.event;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.client.gui.screen.TacticalMainMenuScreen;

/**
 * Client-side event handlers for WRB BaseMod
 * Note: Player interaction (right-click) is now handled by PlayerInteractionHandler
 * NeoForge 1.21.1: Updated event bus annotations and event classes
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class PjmClientEvents {

    private static final int TARGET_GUI_SCALE = 2;

    private PjmClientEvents() {}

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        minecraft.execute(() -> {
            applyDisplayPreferences(minecraft);
            disableHitboxRendering(minecraft);
        });

    }
    
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // Сбрасываем клиентские данные при выходе из мира
        ru.liko.pjmbasemod.common.customization.CustomizationManager.resetClientInstance();
        ru.liko.pjmbasemod.client.ClientTeamConfig.reset();
        ru.liko.pjmbasemod.client.ClientKitsCache.clear();
        ru.liko.pjmbasemod.client.ClientPlayerDataCache.clear();
    }
    
    /**
     * Отключает отображение хитбоксов (F3+B) при подключении к серверу.
     * Предотвращает использование хитбоксов, если они были включены в одиночной игре.
     */
    private static void disableHitboxRendering(Minecraft minecraft) {
        if (minecraft.getEntityRenderDispatcher() != null) {
            minecraft.getEntityRenderDispatcher().setRenderHitBoxes(false);
        }
    }

    @SubscribeEvent
    public static void onOpenScreen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof TitleScreen && !(event.getScreen() instanceof TacticalMainMenuScreen)) {
            event.setNewScreen(new TacticalMainMenuScreen());
        }
    }

    private static void applyDisplayPreferences(Minecraft minecraft) {
        Options options = minecraft.options;
        if (options == null) {
            return;
        }

        enforceFullscreen(minecraft, options);
        enforceGuiScale(options);
        options.save();
    }

    private static void enforceFullscreen(Minecraft minecraft, Options options) {
        Window window = minecraft.getWindow();
        if (window == null) {
            return;
        }

        OptionInstance<Boolean> fullscreenOption = options.fullscreen();
        if (fullscreenOption != null && !Boolean.TRUE.equals(fullscreenOption.get())) {
            fullscreenOption.set(true);
        }

        if (!window.isFullscreen()) {
            window.toggleFullScreen();
        }
    }

    private static void enforceGuiScale(Options options) {
        OptionInstance<Integer> guiScaleOption = options.guiScale();
        if (guiScaleOption == null) {
            return;
        }

        Integer currentValue = guiScaleOption.get();
        if (currentValue == null || currentValue != TARGET_GUI_SCALE) {
            guiScaleOption.set(TARGET_GUI_SCALE);
        }
    }
}
