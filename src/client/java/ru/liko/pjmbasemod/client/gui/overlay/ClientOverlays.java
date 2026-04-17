package ru.liko.pjmbasemod.client.gui.overlay;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.client.gui.overlay.gamemode.GameModeHudOverlay;

/**
 * MOD bus: регистрируем свой оверлей
 * NeoForge 1.21.1: Updated to use RegisterGuiLayersEvent
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientOverlays {

    private ClientOverlays() {}

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        // NeoForge 1.21.1: Use registerBelow with VanillaGuiLayers and ResourceLocation
        e.registerBelow(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "custom_hotbar_right_center"), CustomHotbarOverlay.INSTANCE);
        e.registerBelow(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "compass_overlay"), HudOverlay.COMPASS_OVERLAY);
        e.registerBelow(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "capture_overlay"), GameModeHudOverlay.OVERLAY);
    }
}

