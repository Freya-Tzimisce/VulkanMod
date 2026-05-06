package net.vulkanmod.mixin.wayland;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import net.vulkanmod.config.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InputConstants.class)
public class InputConstantsM {
    @WrapOperation(method = "grabOrReleaseMouse(JIDD)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"))
    private static void onGrabOrReleaseMouse(long window, double xpos, double ypos, Operation<Void> original) {
        if (!Platform.isWayLand()) {
            original.call(window, xpos, ypos);
        }
    }
}