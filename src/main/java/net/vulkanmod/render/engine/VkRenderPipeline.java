package net.vulkanmod.render.engine;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;

public record VkRenderPipeline(RenderPipeline info) implements CompiledRenderPipeline {
	@Override
	public boolean containsUniform(String string) {
		return ExtendedRenderPipeline.of(this.info).getProgram().getUniform(string) != null;
	}

	@Override
	public boolean isValid() {
		return true;
	}
}
