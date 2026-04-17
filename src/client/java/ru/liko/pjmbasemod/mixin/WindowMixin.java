package ru.liko.pjmbasemod.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;

/**
 * Mixin to replace Minecraft window title with Project Minecraft.
 * Icon is set separately in Pjmbasemod.ClientModEvents.onClientSetup()
 */
@Mixin(Window.class)
public abstract class WindowMixin {

    // Refmap теперь корректно упакован в JAR, поэтому достаточно deobf-имени.
    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void onSetTitle(String title, CallbackInfo ci) {
        String newTitle = title.replace("Minecraft", "Project Minecraft");
        glfwSetWindowTitle(((Window)(Object)this).getWindow(), newTitle);
        ci.cancel();
    }
}
