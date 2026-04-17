package ru.liko.pjmbasemod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.client.network.ClientPacketProxyImpl;
import ru.liko.pjmbasemod.common.network.ClientPacketProxy;

/**
 * Клиентские события мода (MOD bus).
 * Регистрирует ClientPacketProxy и устанавливает иконку окна.
 */
@EventBusSubscriber(modid = Pjmbasemod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PjmClientModEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Регистрируем клиентский прокси для обработки S2C пакетов
        ClientPacketProxy.setInstance(new ClientPacketProxyImpl());
        LOGGER.info("[PJM] ClientPacketProxy registered");

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        event.enqueueWork(PjmClientModEvents::setWindowIcon);
    }

    private static void setWindowIcon() {
        try {
            java.io.InputStream icon16 = Pjmbasemod.class.getResourceAsStream("/assets/" + Pjmbasemod.MODID + "/textures/icon/icon_16x16.png");
            java.io.InputStream icon32 = Pjmbasemod.class.getResourceAsStream("/assets/" + Pjmbasemod.MODID + "/textures/icon/icon_32x32.png");

            if (icon16 != null && icon32 != null) {
                byte[] bytes16 = icon16.readAllBytes();
                byte[] bytes32 = icon32.readAllBytes();
                icon16.close();
                icon32.close();

                java.nio.ByteBuffer buffer16 = org.lwjgl.system.MemoryUtil.memAlloc(bytes16.length);
                buffer16.put(bytes16).flip();
                java.nio.ByteBuffer buffer32 = org.lwjgl.system.MemoryUtil.memAlloc(bytes32.length);
                buffer32.put(bytes32).flip();

                org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush();
                java.nio.IntBuffer w = stack.mallocInt(1);
                java.nio.IntBuffer h = stack.mallocInt(1);
                java.nio.IntBuffer c = stack.mallocInt(1);

                java.nio.ByteBuffer pixels16 = org.lwjgl.stb.STBImage.stbi_load_from_memory(buffer16, w, h, c, 4);
                java.nio.ByteBuffer pixels32 = org.lwjgl.stb.STBImage.stbi_load_from_memory(buffer32, w, h, c, 4);

                if (pixels16 != null && pixels32 != null) {
                    org.lwjgl.glfw.GLFWImage.Buffer icons = org.lwjgl.glfw.GLFWImage.malloc(2);
                    icons.position(0).width(16).height(16).pixels(pixels16);
                    icons.position(1).width(32).height(32).pixels(pixels32);
                    icons.position(0);

                    long windowHandle = Minecraft.getInstance().getWindow().getWindow();
                    org.lwjgl.glfw.GLFW.glfwSetWindowIcon(windowHandle, icons);

                    org.lwjgl.stb.STBImage.stbi_image_free(pixels16);
                    org.lwjgl.stb.STBImage.stbi_image_free(pixels32);
                    icons.free();
                    LOGGER.info("Project Minecraft window icon set successfully");
                }

                org.lwjgl.system.MemoryUtil.memFree(buffer16);
                org.lwjgl.system.MemoryUtil.memFree(buffer32);
                stack.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to set Project Minecraft window icon: {}", e.getMessage());
        }
    }
}
