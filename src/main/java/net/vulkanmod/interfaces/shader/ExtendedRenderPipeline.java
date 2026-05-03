package net.vulkanmod.interfaces.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.vulkanmod.render.engine.VkProgram;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.function.Supplier;

public interface ExtendedRenderPipeline {

   static ExtendedRenderPipeline of(RenderPipeline renderPipeline) {
      return (ExtendedRenderPipeline) renderPipeline;
   }

   void setupUniformSuppliers(UBO ubo);

   Supplier<MappedBuffer> getUniformSupplier(String name);

   void setPipeline(GraphicsPipeline pipeline);

   void setProgram(VkProgram program);

   Pipeline getPipeline();

   VkProgram getProgram();
}