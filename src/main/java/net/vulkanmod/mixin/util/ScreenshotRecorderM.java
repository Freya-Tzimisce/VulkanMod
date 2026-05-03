package net.vulkanmod.mixin.util;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Screenshot;
import net.vulkanmod.render.engine.VkTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Consumer;

@Mixin(Screenshot.class)
public class ScreenshotRecorderM {

    /**
     * @author
     */
    @Overwrite
    public static void takeScreenshot(RenderTarget renderTarget, Consumer<NativeImage> consumer) {
        int width = renderTarget.width;
        int height = renderTarget.height;
        GpuTexture gpuTexture = renderTarget.getColorTexture();
        if (gpuTexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        } else {
            Renderer.getInstance().flushCmds();
            int pixelSize = TextureFormat.RGBA8.pixelSize();
            GpuBuffer gpuBuffer = RenderSystem.getDevice()
                    .createBuffer(() -> "Screenshot buffer", BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, width * height * pixelSize);
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
                try (GpuBuffer.ReadView readView = commandEncoder.readBuffer(gpuBuffer)) {
                    NativeImage nativeImage = new NativeImage(width, height, false);
                    VkTexture colorAttachment = (VkTexture)(Object)Renderer.getInstance().getMainPass().getColorAttachment();
                    boolean isBgraFormat = colorAttachment.getVulkanImage().format == 44;

                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int color = readView.data().getInt((x + y * width) * pixelSize);
                            if (isBgraFormat) {
                                color = ColorUtil.BGRAtoRGBA(color);
                            }

                            nativeImage.setPixelABGR(x, y, color | 0xFF000000);
                        }
                    }

                    consumer.accept(nativeImage);
                }

                gpuBuffer.close();
            }, 0);
        }
    }
}
