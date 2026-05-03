package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.render.engine.VkProgram;
import net.vulkanmod.render.engine.VkCommandEncoder;
import net.vulkanmod.render.engine.VkTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(RenderType.CompositeRenderType.class)
public abstract class CompositeRenderTypeM {

   @Shadow @Final private RenderType.CompositeState state;
   @Shadow @Final private RenderPipeline renderPipeline;

   @Shadow
   public abstract RenderPipeline getRenderPipeline();

   // TODO
   /**
    * @author
    * @reason
    */
   @Overwrite
   public void draw(MeshData meshData) {
      RenderPipeline renderPipeline = this.getRenderPipeline();
      ((RenderType.CompositeRenderType)(Object)(this)).setupRenderState();

      try {
         RenderTarget renderTarget = ((CompositeStateAccessor)(Object)(this.state)).getOutputState().getRenderTarget();
         VkCommandEncoder commandEncoder = (VkCommandEncoder)RenderSystem.getDevice().createCommandEncoder();

         try (RenderPass renderPass = commandEncoder.createRenderPass(
                 renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.useDepth ? renderTarget.getDepthTexture() : null, OptionalDouble.empty()
         )) {
            renderPass.setPipeline(renderPipeline);
            if (RenderSystem.SCISSOR_STATE.isEnabled()) {
               renderPass.enableScissor(RenderSystem.SCISSOR_STATE);
            }

            for (int i = 0; i < 12; i++) {
               GpuTexture gpuTexture = RenderSystem.getShaderTexture(i);
               if (gpuTexture != null) {
                  if (((VkTexture)gpuTexture).getVulkanImage() == null) {
                     throw new NullPointerException();
                  }

                  renderPass.bindSampler("Sampler" + i, gpuTexture);
                  VTextureSelector.bindTexture(i, ((VkTexture)gpuTexture).getVulkanImage());
               }
            }

            VRenderSystem.applyModelViewMatrix(RenderSystem.getModelViewMatrix());
            VRenderSystem.applyProjectionMatrix(RenderSystem.getProjectionMatrix());
            VRenderSystem.calculateMVP();
            VkProgram glProgram = ExtendedRenderPipeline.of(renderPipeline).getProgram();
            Window window = Minecraft.getInstance().getWindow();
            glProgram.setDefaultUniforms(
                    renderPipeline.getVertexFormatMode(),
                    RenderSystem.getModelViewMatrix(),
                    RenderSystem.getProjectionMatrix(),
                    window.getWidth(),
                    window.getHeight()
            );
            commandEncoder.applyPipelineState(renderPipeline);
            Pipeline pipeline = ExtendedRenderPipeline.of(renderPipeline).getPipeline();
            VRenderSystem.setPrimitiveTopologyGL(GlConst.toGl(meshData.drawState().mode()));
            Renderer renderer = Renderer.getInstance();
            renderer.bindGraphicsPipeline((GraphicsPipeline)pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer()
                    .draw(
                            meshData.vertexBuffer(),
                            meshData.indexBuffer(),
                            meshData.drawState().mode(),
                            meshData.drawState().format(),
                            meshData.drawState().vertexCount()
                    );
         }
      } catch (Throwable var13) {
         if (meshData != null) {
            try {
               meshData.close();
            } catch (Throwable var10) {
               var13.addSuppressed(var10);
            }
         }

         throw var13;
      }

      if (meshData != null) {
         meshData.close();
      }

      ((RenderType.CompositeRenderType)(Object)(this)).clearRenderState();
   }

}