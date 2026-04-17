package ru.liko.pjmbasemod.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import ru.liko.pjmbasemod.Pjmbasemod;

/**
 * Регистрация клавиш управления модом
 * NeoForge 1.21.1: Updated event bus annotations
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyBindings {

    public static final String CATEGORY = "key.categories." + Pjmbasemod.MODID;

    public static final KeyMapping OPEN_CLASS_SELECTION = new KeyMapping(
            "key." + Pjmbasemod.MODID + ".open_class_selection",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B, // По умолчанию клавиша B
            CATEGORY);

    public static final KeyMapping TOGGLE_CHAT_MODE = new KeyMapping(
            "key." + Pjmbasemod.MODID + ".toggle_chat_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y, // По умолчанию клавиша Y
            CATEGORY);

    public static final KeyMapping COMMAND_RADIO = new KeyMapping(
            "key." + Pjmbasemod.MODID + ".command_radio",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, // По умолчанию клавиша V
            CATEGORY);

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CLASS_SELECTION);
        event.register(TOGGLE_CHAT_MODE);
        event.register(COMMAND_RADIO);
    }
}
