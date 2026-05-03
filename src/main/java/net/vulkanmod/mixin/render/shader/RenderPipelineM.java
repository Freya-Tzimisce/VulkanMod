package net.vulkanmod.mixin.render.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.render.engine.VkProgram;
import net.vulkanmod.util.LogUtil;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Supplier;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineM implements ExtendedRenderPipeline {
	@Shadow @Final private ResourceLocation location;
	@Shadow @Final private VertexFormat vertexFormat;
	@Shadow @Final private List<RenderPipeline.UniformDescription> uniforms;
	@Shadow @Final private List<String> samplers;
	@Unique GraphicsPipeline pipeline;
	@Unique VkProgram vkProgram;
	@Unique Logger LOGGER = LogUtil.getLogger();

	@Shadow public abstract ResourceLocation getVertexShader();

	@Shadow public abstract ResourceLocation getFragmentShader();

	@Unique
	@Override
	public void setupUniformSuppliers(UBO ubo) {
		for (Uniform vUniform : ubo.getUniforms()) {
			Supplier<MappedBuffer> supplier = this.getUniformSupplier(vUniform.getName());
			vUniform.setSupplier(supplier);
		}
	}

	@Override
	public Supplier<MappedBuffer> getUniformSupplier(String name) {
		com.mojang.blaze3d.opengl.Uniform uniform = this.vkProgram.getUniform(name);
		if (uniform == null) {
			LOGGER.error("Error: field {} not present in uniform map", name);
			return null;
		} else {
			ByteBuffer byteBuffer = switch (uniform.getType()) {
				case INT, IVEC3 -> MemoryUtil.memByteBuffer(((UniformAccessor)uniform).getIntValues());
				case FLOAT, VEC2, VEC3, VEC4, MATRIX4X4 -> MemoryUtil.memByteBuffer(((UniformAccessor)uniform).getFloatValues());
			};
			MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
			return () -> mappedBuffer;
		}
	}

	@Override
	public void setPipeline(GraphicsPipeline pipeline) {
		this.pipeline = pipeline;
	}

	@Override
	public void setProgram(VkProgram program) {
		this.vkProgram = program;
	}

	@Override
	public VkProgram getProgram() {
		return this.vkProgram;
	}

	@Override
	public Pipeline getPipeline() {
		return this.pipeline;
	}
}