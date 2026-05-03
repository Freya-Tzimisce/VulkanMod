package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.vulkanmod.render.engine.VkFbo;
import net.vulkanmod.render.engine.VkTexture;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.OptionalInt;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

    @Shadow public int viewWidth;
    @Shadow public int viewHeight;
    @Shadow public int width;
    @Shadow public int height;
    @Shadow @Final public boolean useDepth;

    @Shadow @Nullable protected GpuTexture colorTexture;
    @Shadow @Nullable protected GpuTexture depthTexture;
    boolean needClear = false;
    boolean bound = false;

    /**
     * @author
     */
    @Overwrite
    public void blitAndBlendToTexture(GpuTexture gpuTexture) {
        RenderSystem.assertOnRenderThread();

        VkFbo fbo = ((VkTexture)this.colorTexture).getFbo(this.depthTexture);
        if (!fbo.needsClear()) {
            AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer gpuBuffer = autoStorageIndexBuffer.getBuffer(6);
            GpuBuffer gpuBuffer2 = RenderSystem.getQuadVertexBuffer();

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(gpuTexture, OptionalInt.empty())) {
                renderPass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
                renderPass.setVertexBuffer(0, gpuBuffer2);
                renderPass.setIndexBuffer(gpuBuffer, autoStorageIndexBuffer.type());
                renderPass.bindSampler("InSampler", this.colorTexture);
                renderPass.drawIndexed(0, 6);
            }
        }
    }
}