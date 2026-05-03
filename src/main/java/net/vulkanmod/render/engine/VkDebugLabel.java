package net.vulkanmod.render.engine;

import com.mojang.blaze3d.opengl.VertexArrayCache;
import net.vulkanmod.util.LogUtil;
import org.slf4j.Logger;

import java.util.Set;

public class VkDebugLabel {
	private static final Logger LOGGER = LogUtil.getLogger();

	public void applyLabel(VkBuffer glBuffer) {
	}

	public void applyLabel(VkTexture glTexture) {
	}

	public void applyLabel(VkShaderModule vkShaderModule) {
	}

	public void applyLabel(VkProgram eglProgram) {
	}

	public void applyLabel(VertexArrayCache.VertexArray vertexArray) {
	}

	public static VkDebugLabel create(boolean bl, Set<String> set) {
		return new VkDebugLabel();
	}

	public boolean exists() {
		return true;
	}


}