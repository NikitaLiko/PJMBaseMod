package ru.liko.pjmbasemod.client.event;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.client.gui.overlay.RankUpdateOverlay;

/**
 * NeoForge 1.21.1: Updated to use RegisterGuiLayersEvent instead of RegisterGuiOverlaysEvent
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PjmClientModEvents {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        // NeoForge 1.21.1: Use registerAbove with VanillaGuiLayers
        event.registerAbove(VanillaGuiLayers.CHAT, ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "rank_update"), RankUpdateOverlay.OVERLAY);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "xp_gain"), ru.liko.pjmbasemod.client.gui.overlay.XpGainOverlay.OVERLAY);
    }
}
