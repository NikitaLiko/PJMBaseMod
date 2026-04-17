package ru.liko.pjmbasemod.client.event;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.lwjgl.glfw.GLFW;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.client.gui.overlay.CustomHotbarOverlay;
import ru.liko.pjmbasemod.client.input.ModKeyBindings;
import ru.liko.pjmbasemod.common.init.PjmSounds;
import ru.liko.pjmbasemod.common.network.PjmNetworking;
import ru.liko.pjmbasemod.common.network.packet.ChangeChatModePacket;
import ru.liko.pjmbasemod.common.network.packet.OpenClassSelectionPacket;
import ru.liko.pjmbasemod.common.network.packet.RadioSwitchPacket;

/**
 * Обработчик нажатий клавиш на клиенте
 * NeoForge 1.21.1: Updated event annotations and imports
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class KeyInputHandler {

    private static boolean isRadioPressed = false;
    public static int activeTeammateRadios = 0;
    private static net.minecraft.client.resources.sounds.SoundInstance teammateRadioNoise;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // NeoForge 1.21.1: Uses ClientTickEvent.Post instead of
        // TickEvent.ClientTickEvent with phase check

        // Проверяем, нажата ли клавиша открытия меню выбора класса
        if (ModKeyBindings.OPEN_CLASS_SELECTION.consumeClick()) {
            PjmNetworking.sendToServer(OpenClassSelectionPacket.INSTANCE);
        }

        // Проверяем, нажата ли клавиша переключения режима чата
        if (ModKeyBindings.TOGGLE_CHAT_MODE.consumeClick()) {
            // Отправляем пакет на сервер для переключения режима чата
            PjmNetworking.sendToServer(ChangeChatModePacket.createToggle());
        }

        // Логика Push-to-Talk для рации
        boolean currentlyPressed = ModKeyBindings.COMMAND_RADIO.isDown();
        if (currentlyPressed != isRadioPressed) {
            isRadioPressed = currentlyPressed;
            PjmNetworking.sendToServer(new RadioSwitchPacket(isRadioPressed));

            // Воспроизведение звуков рации на клиенте
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                if (isRadioPressed) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(PjmSounds.RADIO_START.get(), 1.0F, 1.0F));
                } else {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(PjmSounds.RADIO_END.get(), 1.0F, 1.0F));
                }
            }
        }

        // Логика фонового шума рации от тиммейтов
        Minecraft mc = Minecraft.getInstance();
        if (activeTeammateRadios > 0) {
            // Если звук еще не играет или уже закончился, запускаем его заново
            if (teammateRadioNoise == null || !mc.getSoundManager().isActive(teammateRadioNoise)) {
                // Питч 1.0F, Громкость 0.5F (чтобы шум не перекрывал голос)
                teammateRadioNoise = SimpleSoundInstance.forUI(PjmSounds.RADIO_BACKGROUND.get(), 1.0F, 0.5F);
                mc.getSoundManager().play(teammateRadioNoise);
            }
        } else {
            // Тиммейты замолчали - рубим фон
            if (teammateRadioNoise != null) {
                mc.getSoundManager().stop(teammateRadioNoise);
                teammateRadioNoise = null;
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // Отслеживаем взаимодействия с hotbar (клавиши 1-9 для переключения слотов)
        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
            int keyCode = event.getKey();
            if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
                CustomHotbarOverlay.updateInteractionTime();
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        // Обновляем время взаимодействия при прокрутке колесика мыши
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && event.getScrollDeltaY() != 0) {
            CustomHotbarOverlay.updateInteractionTime();
        }
    }
}
