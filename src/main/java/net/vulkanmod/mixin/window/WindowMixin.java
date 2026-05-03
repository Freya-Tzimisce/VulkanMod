package net.vulkanmod.mixin.window;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.config.ConfigManager;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.option.VOptions;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowMode;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private long window;

    @Shadow private boolean vsync;
    @Shadow private boolean fullscreen;

    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;

    @Shadow private int framebufferWidth;
    @Shadow private int framebufferHeight;

    @Unique private boolean wasOnFullscreen = false;

    @Shadow public abstract int getWidth();

    @Shadow public abstract int getHeight();

    @Shadow protected abstract void updateFullscreen(boolean bl, @Nullable TracyFrameCapture tracyFrameCapture);

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void redirect(int hint, int value) { }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private void vulkanHint(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        //Fix WayLand Client-Side Decorators
        GLFW.glfwWindowHint(GLFW_DECORATED, (Platform.isWayLand() ? GLFW_FALSE : GLFW_TRUE));
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void getHandle(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        VRenderSystem.setWindow(this.window);
    }

    /**
     * @author
     */
    @Overwrite
    public void updateVsync(boolean vsync) {
        this.vsync = vsync;
        Vulkan.setVsync(vsync);
    }

    @Inject(method = "toggleFullScreen()V", at = @At("TAIL"))
    public void toggleFullScreen(CallbackInfo ci) {
        VOptions.fullscreenDirty = true;
    }

    /**
     * @author
     */
    @Overwrite
    public void updateDisplay(@Nullable TracyFrameCapture tracyFrameCapture) {
        RenderSystem.flipFrame(this.window, tracyFrameCapture);

        if (VOptions.fullscreenDirty) {
            VOptions.fullscreenDirty = false;
            this.updateFullscreen(this.vsync, tracyFrameCapture);
        }
    }

    /**
     * @author
     */
    @Overwrite
    private void setMode() {
        var config = ConfigManager.getConfig();

        long monitor = GLFW.glfwGetPrimaryMonitor();
        if (this.fullscreen) {
            var videoMode = config.videoMode;
            VideoModeSet set = VideoModeManager.getFromVideoMode(videoMode);
            boolean supported;

            if (set != null) {
                supported = set.hasRefreshRate(videoMode.getRefreshRate());
            } else {
                supported = false;
            }

            if (!supported) {
                LOGGER.error("Resolution not supported, using first available as fallback");
                videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
            }

            if (!this.wasOnFullscreen) {
                this.windowedX = this.x;
                this.windowedY = this.y;
                this.windowedWidth = this.width;
                this.windowedHeight = this.height;
            }

            this.x = 0;
            this.y = 0;
            this.width = videoMode.getWidth();
            this.height = videoMode.getHeight();
            GLFW.glfwSetWindowMonitor(this.window, monitor, this.x, this.y, this.width, this.height, videoMode.getRefreshRate());

            this.wasOnFullscreen = true;
        } else if (config.windowMode == WindowMode.WINDOWED_FULLSCREEN.mode) {
            var videoModex = VideoModeManager.getOsVideoMode();

            if (!this.wasOnFullscreen) {
                this.windowedX = this.x;
                this.windowedY = this.y;
                this.windowedWidth = this.width;
                this.windowedHeight = this.height;
            }

            int width = videoModex.getWidth();
            int height = videoModex.getHeight();

            GLFW.glfwSetWindowAttrib(this.window, GLFW_DECORATED, GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(this.window, 0L, 0, 0, width, height, -1);

            this.width = width;
            this.height = height;
            this.wasOnFullscreen = true;
        } else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;

            GLFW.glfwSetWindowMonitor(this.window, 0L, this.x, this.y, this.width, this.height, -1);
            GLFW.glfwSetWindowAttrib(this.window, GLFW_DECORATED, GLFW_TRUE);

            this.wasOnFullscreen = false;
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void onFramebufferResize(long window, int width, int height) {
        if (window == this.window) {
            int prevWidth = this.getWidth();
            int prevHeight = this.getHeight();

            if (width > 0 && height > 0) {
                this.framebufferWidth = width;
                this.framebufferHeight = height;
                // if (this.framebufferWidth != prevWidth || this.framebufferHeight != prevHeight) {
                //     this.eventHandler.resizeDisplay();
                // }

                Renderer.scheduleSwapChainUpdate();
            }

        }
    }

    @Inject(method = "onResize(JII)V", at = @At("TAIL"))
    private void onResize(long l, int i, int j, CallbackInfo ci) {
        if (width > 0 && height > 0) {
            Renderer.scheduleSwapChainUpdate();
        }
    }
}