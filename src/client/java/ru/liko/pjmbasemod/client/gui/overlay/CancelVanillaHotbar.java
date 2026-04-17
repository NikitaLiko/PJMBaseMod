package ru.liko.pjmbasemod.client.gui.overlay;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * GAME bus: скрываем ванильный HOTBAR и полосу опыта
 * Сдвигаем за пределы экрана вместо отмены события, чтобы не ломать другие моды (Voice Chat)
 * NeoForge 1.21.1: Updated to use RenderGuiLayerEvent
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CancelVanillaHotbar {

    private CancelVanillaHotbar() {}

    @SubscribeEvent
    public static void onPre(RenderGuiLayerEvent.Pre e) {
        // Сдвигаем hotbar за пределы экрана (вниз на 1000 пикселей)
        if (e.getName().equals(VanillaGuiLayers.HOTBAR)) {
            e.getGuiGraphics().pose().pushPose();
            e.getGuiGraphics().pose().translate(0, 1000, 0);
        }
        
        // Сдвигаем experience bar за пределы экрана
        if (e.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR)) {
            e.getGuiGraphics().pose().pushPose();
            e.getGuiGraphics().pose().translate(0, 1000, 0);
        }
    }
    
    @SubscribeEvent
    public static void onPost(RenderGuiLayerEvent.Post e) {
        // Восстанавливаем позицию после рендеринга
        if (e.getName().equals(VanillaGuiLayers.HOTBAR) ||
            e.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR)) {
            e.getGuiGraphics().pose().popPose();
        }
    }
}

