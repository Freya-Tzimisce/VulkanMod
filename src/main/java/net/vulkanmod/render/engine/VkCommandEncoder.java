package net.vulkanmod.render.engine;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.util.LogUtil;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.memory.buffer.IndexBuffer;
import net.vulkanmod.vulkan.memory.buffer.index.AutoIndexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class VkCommandEncoder implements CommandEncoder {
	private static final Logger LOGGER = LogUtil.getLogger();
	private final VkDevice device;
//	private final int readFbo;
//	private final int drawFbo;
	@Nullable
	private RenderPipeline lastPipeline;
	private boolean inRenderPass;
	@Nullable
	private VkProgram lastProgram;

	protected VkCommandEncoder(VkDevice vkDevice) {
		this.device = vkDevice;
//		this.readFbo = vkDevice.directStateAccess().createFrameBufferObject();
//		this.drawFbo = vkDevice.directStateAccess().createFrameBufferObject();
	}

	@Override
	public RenderPass createRenderPass(GpuTexture gpuTexture, OptionalInt optionalInt) {
		return this.createRenderPass(gpuTexture, optionalInt, null, OptionalDouble.empty());
	}

	@Override
	public RenderPass createRenderPass(GpuTexture gpuTexture, OptionalInt optionalInt, @Nullable GpuTexture gpuTexture2, OptionalDouble optionalDouble) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		} else {
			if (optionalDouble.isPresent() && gpuTexture2 == null) {
				LOGGER.warn("Depth clear value was provided but no depth texture is being used");
			}

			if (Minecraft.getInstance().getMainRenderTarget().getColorTexture() == gpuTexture) {
				Renderer.getInstance().getMainPass().rebindMainTarget();

				int j = 0;
				if (optionalInt.isPresent()) {
					int k = optionalInt.getAsInt();
					GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
					j |= 16384;
				}

				if (gpuTexture2 != null && optionalDouble.isPresent()) {
					GL11.glClearDepth(optionalDouble.getAsDouble());
					j |= 256;
				}

				if (j != 0) {
					GlStateManager._disableScissorTest();
					GlStateManager._depthMask(true);
					GlStateManager._colorMask(true, true, true, true);
					GlStateManager._clear(j);
				}

				return new VkRenderPass(this, gpuTexture2 != null);
			}

			if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Color texture is closed");
			} else if (gpuTexture2 != null && gpuTexture2.isClosed()) {
				throw new IllegalStateException("Depth texture is closed");
			} else {
				this.inRenderPass = true;
				VkFbo fbo = ((VkTexture)gpuTexture).getFbo(gpuTexture2);
				fbo.bind();

				int jx = 0;
				if (optionalInt.isPresent()) {
					int k = optionalInt.getAsInt();
					GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
					jx |= 16384;
				}

				if (gpuTexture2 != null && optionalDouble.isPresent()) {
					GL11.glClearDepth(optionalDouble.getAsDouble());
					jx |= 256;
				}

				if (jx != 0) {
					GlStateManager._disableScissorTest();
					GlStateManager._depthMask(true);
					GlStateManager._colorMask(true, true, true, true);
					GlStateManager._clear(jx);
				}

				GlStateManager._viewport(0, 0, gpuTexture.getWidth(0), gpuTexture.getHeight(0));
				this.lastPipeline = null;
				return new VkRenderPass(this, gpuTexture2 != null);
			}
		}

	}

	@Override
	public void clearColorTexture(GpuTexture colorAttachment, int color) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}
		else {
			VRenderSystem.setClearColor(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
			Renderer.clearAttachments(16384);
		}
	}

	@Override
	public void clearColorAndDepthTextures(GpuTexture colorAttachment, int clearColor, GpuTexture depthAttachment, double clearDepth) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}
		else {
			if (Minecraft.getInstance().getMainRenderTarget().getColorTexture() == colorAttachment) {
				Renderer.getInstance().getMainPass().rebindMainTarget();

				VRenderSystem.clearDepth(clearDepth);
				VRenderSystem.setClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
				Renderer.clearAttachments(0x4100);
			}
			else {
				VkFbo fbo = ((VkTexture)colorAttachment).getFbo(depthAttachment);

				fbo.clear = 0x4100;
				fbo.clearColor = clearColor;
				fbo.clearDepth = (float) clearDepth;

				Framebuffer boundFramebuffer = Renderer.getInstance().getBoundFramebuffer();
				if (boundFramebuffer.getColorAttachment() == ((VkTexture) colorAttachment).getVulkanImage()
						&& boundFramebuffer.getDepthAttachment() == ((VkTexture) depthAttachment).getVulkanImage())
				{
					fbo.clearAttachments();
				}
			}
		}
	}

	@Override
	public void clearDepthTexture(GpuTexture depthAttachment, double clearDepth) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		} else {
			VRenderSystem.clearDepth(clearDepth);
			Renderer.clearAttachments(256);
		}
	}

	@Override
	public void writeToBuffer(GpuBuffer gpuBuffer, ByteBuffer byteBuffer, int offset) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		} else {
			VkBuffer vkBuffer = (VkBuffer)gpuBuffer;
			if (vkBuffer.closed) {
				throw new IllegalStateException("Buffer already closed");
			}
			else if (!vkBuffer.usage().isWritable()) {
				throw new IllegalStateException("Buffer is not writable");
			}
			else {
				int remaining = byteBuffer.remaining();
				if (remaining + offset > vkBuffer.size) {
					throw new IllegalArgumentException(
							"Cannot write more data than this buffer can hold (attempting to write " + remaining + " bytes at offset " + offset + " to " + vkBuffer.size + " size buffer)"
					);
				} else {
					if (!vkBuffer.initialized) {
						vkBuffer.buffer.createBuffer(vkBuffer.size());
						vkBuffer.initialized = true;
					}

					vkBuffer.buffer.copyBuffer(byteBuffer, byteBuffer.remaining(), offset);
				}
			}
		}
	}

	@Override
	public GpuBuffer.ReadView readBuffer(GpuBuffer gpuBuffer) {
		return this.readBuffer(gpuBuffer, 0, gpuBuffer.size());
	}

	@Override
	public GpuBuffer.ReadView readBuffer(GpuBuffer gpuBuffer, int offset, int size) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		} else {
			VkBuffer vkGpuBuffer = (VkBuffer)gpuBuffer;
			if (vkGpuBuffer.closed) {
				throw new IllegalStateException("Buffer already closed");
			} else if (!vkGpuBuffer.usage().isReadable()) {
				throw new IllegalStateException("Buffer is not readable");
			} else if (offset + size > vkGpuBuffer.size) {
				throw new IllegalArgumentException(
						"Cannot read more data than this buffer can hold (attempting to read "
								+ size
								+ " bytes at offset "
								+ offset
								+ " from "
								+ vkGpuBuffer.size
								+ " size buffer)"
				);
			} else if (vkGpuBuffer.getBuffer().getDataPtr() == 0L) {
				throw new IllegalArgumentException("Buffer not mappable");
			} else {
				ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(vkGpuBuffer.getBuffer().getDataPtr() + offset, size);
				return new VkBuffer.ReadView(0, byteBuffer);
			}
		}
	}

	@Override
	public void writeToTexture(GpuTexture gpuTexture, NativeImage nativeImage) {
		int i = gpuTexture.getWidth(0);
		int j = gpuTexture.getHeight(0);
		if (nativeImage.getWidth() != i || nativeImage.getHeight() != j) {
			throw new IllegalArgumentException(
					"Cannot replace texture of size " + i + "x" + j + " with image of size " + nativeImage.getWidth() + "x" + nativeImage.getHeight()
			);
		} else if (gpuTexture.isClosed()) {
			throw new IllegalStateException("Destination texture is closed");
		} else {
			this.writeToTexture(gpuTexture, nativeImage, 0, 0, 0, i, j, 0, 0);
		}
	}

	@Override
	public void writeToTexture(GpuTexture gpuTexture, NativeImage nativeImage, int level, int xOffset, int yOffset, int width, int height, int unpackSkipPixels, int unpackSkipRows) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		} else if (level >= 0 && level < gpuTexture.getMipLevels()) {
			if (unpackSkipPixels + width > nativeImage.getWidth() || unpackSkipRows + height > nativeImage.getHeight()) {
				throw new IllegalArgumentException(
						"Copy source ("
								+ nativeImage.getWidth()
								+ "x"
								+ nativeImage.getHeight()
								+ ") is not large enough to read a rectangle of "
								+ width
								+ "x"
								+ height
								+ " from "
								+ unpackSkipPixels
								+ "x"
								+ unpackSkipRows
				);
			} else if (xOffset + width > gpuTexture.getWidth(level) || yOffset + height > gpuTexture.getHeight(level)) {
				throw new IllegalArgumentException(
						"Dest texture (" + width + "x" + height + ") is not large enough to write a rectangle of " + width + "x" + height + " at " + xOffset + "x" + yOffset + " (at mip level " + level + ")"
				);
			} else if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Destination texture is closed");
			} else {
				VTextureSelector.setActiveTexture(0);
				VTextureSelector.bindTexture(((VkTexture) gpuTexture).getVulkanImage());
				VTextureSelector.uploadSubTexture(level, width, height, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, nativeImage.getWidth(), nativeImage.getPointer());
			}
		} else {
			throw new IllegalArgumentException("Invalid mipLevel " + level + ", must be >= 0 and < " + gpuTexture.getMipLevels());
		}
	}

	@Override
	public void writeToTexture(GpuTexture gpuTexture, IntBuffer intBuffer, NativeImage.Format format, int i, int j, int k, int l, int m) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		} else if (i >= 0 && i < gpuTexture.getMipLevels()) {
			if (l * m > intBuffer.remaining()) {
				throw new IllegalArgumentException(
						"Copy would overrun the source buffer (remaining length of " + intBuffer.remaining() + ", but copy is " + l + "x" + m + ")"
				);
			} else if (j + l > gpuTexture.getWidth(i) || k + m > gpuTexture.getHeight(i)) {
				throw new IllegalArgumentException(
						"Dest texture ("
								+ gpuTexture.getWidth(i)
								+ "x"
								+ gpuTexture.getHeight(i)
								+ ") is not large enough to write a rectangle of "
								+ l
								+ "x"
								+ m
								+ " at "
								+ j
								+ "x"
								+ k
				);
			} else if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Destination texture is closed");
			} else {
				GlStateManager._bindTexture(((VkTexture)gpuTexture).id);
				GlStateManager._pixelStore(3314, l);
				GlStateManager._pixelStore(3316, 0);
				GlStateManager._pixelStore(3315, 0);
				GlStateManager._pixelStore(3317, format.components());
				GlStateManager._texSubImage2D(3553, i, j, k, l, m, GlConst.toGl(format), 5121, intBuffer);
			}
		} else {
			throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + gpuTexture.getMipLevels());
		}
	}

	@Override
	public void copyTextureToBuffer(GpuTexture gpuTexture, GpuBuffer gpuBuffer, int i, Runnable runnable, int j) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		} else {
			this.copyTextureToBuffer(gpuTexture, gpuBuffer, i, runnable, j, 0, 0, gpuTexture.getWidth(j), gpuTexture.getHeight(j));
		}
	}

	@Override
	public void copyTextureToBuffer(GpuTexture gpuTexture, GpuBuffer gpuBuffer, int dstOffset, Runnable runnable, int mipLevel, int xOffset, int yOffset, int width, int height) {
		VkBuffer vkGpuBuffer = (VkBuffer) gpuBuffer;
		VkTexture vkGpuTexture = (VkTexture) gpuTexture;

		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		} else if (mipLevel >= 0 && mipLevel < gpuTexture.getMipLevels()) {
			if (gpuTexture.getWidth(mipLevel) * gpuTexture.getHeight(mipLevel) * vkGpuTexture.getVulkanImage().formatSize + dstOffset > gpuBuffer.size()) {
				throw new IllegalArgumentException(
						"Buffer of size "
								+ gpuBuffer.size()
								+ " is not large enough to hold "
								+ width
								+ "x"
								+ height
								+ " pixels ("
								+ vkGpuTexture.getVulkanImage().formatSize
								+ " bytes each) starting from offset "
								+ dstOffset
				);
			}
			else if (gpuBuffer.type() != BufferType.PIXEL_PACK) {
				throw new IllegalArgumentException("Buffer of type " + gpuBuffer.type() + " cannot be used to retrieve a texture");
			}
			else if (xOffset + width > gpuTexture.getWidth(mipLevel) || yOffset + height > gpuTexture.getHeight(mipLevel)) {
				throw new IllegalArgumentException(
						"Copy source texture ("
								+ gpuTexture.getWidth(mipLevel)
								+ "x"
								+ gpuTexture.getHeight(mipLevel)
								+ ") is not large enough to read a rectangle of "
								+ width
								+ "x"
								+ height
								+ " from "
								+ xOffset
								+ ","
								+ yOffset
				);
			} else if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Source texture is closed");
			} else if (gpuBuffer.isClosed()) {
				throw new IllegalStateException("Destination buffer is closed");
			} else {
				ImageUtil.copyImageToBuffer(vkGpuTexture.getVulkanImage(), vkGpuBuffer.getBuffer(), mipLevel, width, height, xOffset, yOffset, dstOffset, width, height);

				runnable.run();
			}
		} else {
			throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + gpuTexture.getMipLevels());
		}
	}

	@Override
	public void copyTextureToTexture(GpuTexture gpuTexture, GpuTexture gpuTexture2, int mipLevel, int j, int k, int l, int m, int n, int o) {
		if (this.inRenderPass) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		} else if (mipLevel >= 0 && mipLevel < gpuTexture.getMipLevels() && mipLevel < gpuTexture2.getMipLevels()) {
			if (j + n > gpuTexture2.getWidth(mipLevel) || k + o > gpuTexture2.getHeight(mipLevel)) {
				throw new IllegalArgumentException(
						"Dest texture ("
								+ gpuTexture2.getWidth(mipLevel)
								+ "x"
								+ gpuTexture2.getHeight(mipLevel)
								+ ") is not large enough to write a rectangle of "
								+ n
								+ "x"
								+ o
								+ " at "
								+ j
								+ "x"
								+ k
				);
			} else if (l + n > gpuTexture.getWidth(mipLevel) || m + o > gpuTexture.getHeight(mipLevel)) {
				throw new IllegalArgumentException(
						"Source texture ("
								+ gpuTexture.getWidth(mipLevel)
								+ "x"
								+ gpuTexture.getHeight(mipLevel)
								+ ") is not large enough to read a rectangle of "
								+ n
								+ "x"
								+ o
								+ " at "
								+ l
								+ "x"
								+ m
				);
			} else if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Source texture is closed");
			} else if (gpuTexture2.isClosed()) {
				throw new IllegalStateException("Destination texture is closed");
			} else {
				// TODO implement
			}
		} else {
			throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + gpuTexture.getMipLevels() + " and < " + gpuTexture2.getMipLevels());
		}
	}

	@Override
	public void presentTexture(GpuTexture gpuTexture) {
		throw new UnsupportedOperationException();
	}

	protected void executeDrawMultiple(
			VkRenderPass renderPass,
			Collection<RenderPass.Draw> collection,
			@Nullable GpuBuffer gpuBuffer,
			@Nullable VertexFormat.IndexType indexType
	) {
		if (this.trySetup(renderPass)) {
			if (indexType == null) {
				indexType = VertexFormat.IndexType.SHORT;
			}

			Pipeline pipeline = ExtendedRenderPipeline.of(renderPass.getPipeline()).getPipeline();

			for (RenderPass.Draw draw : collection) {
				VertexFormat.IndexType indexType2 = draw.indexType() == null ? indexType : draw.indexType();
				renderPass.setIndexBuffer(draw.indexBuffer() == null ? gpuBuffer : draw.indexBuffer(), indexType2);
				renderPass.setVertexBuffer(draw.slot(), draw.vertexBuffer());

				if (VkRenderPass.VALIDATION) {
					if (renderPass.indexBuffer == null) {
						throw new IllegalStateException("Missing index buffer");
					}

					if (renderPass.indexBuffer.isClosed()) {
						throw new IllegalStateException("Index buffer has been closed!");
					}

					if (renderPass.vertexBuffers[0] == null) {
						throw new IllegalStateException("Missing vertex buffer at slot 0");
					}

					if (renderPass.vertexBuffers[0].isClosed()) {
						throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
					}
				}

				Consumer<RenderPass.UniformUploader> consumer = draw.uniformUploaderConsumer();
				if (consumer != null) {
					consumer.accept((string, fs) -> {
						VkProgram vkProgram = ExtendedRenderPipeline.of(renderPass.pipeline).getProgram();
						Uniform uniform = vkProgram.getUniform(string);
						if (uniform != null) {
							uniform.set(fs);
						}

						if (string.equals("ModelOffset")) {
							VRenderSystem.setModelOffset(fs[0], fs[1], fs[2]);
						}
					});
					Renderer.getInstance().uploadAndBindUBOs(pipeline);
				}

				this.drawFromBuffers(renderPass, draw.firstIndex(), draw.indexCount(), indexType2, renderPass.pipeline);
			}
		}
	}

	protected void executeDraw(VkRenderPass vkRenderPass, int i, int j, @Nullable VertexFormat.IndexType indexType) {
		if (this.trySetup(vkRenderPass)) {
			if (VkRenderPass.VALIDATION) {
				if (indexType != null) {
					if (vkRenderPass.indexBuffer == null) {
						throw new IllegalStateException("Missing index buffer");
					}

					if (vkRenderPass.indexBuffer.isClosed()) {
						throw new IllegalStateException("Index buffer has been closed!");
					}
				}

				if (vkRenderPass.vertexBuffers[0] == null) {
					throw new IllegalStateException("Missing vertex buffer at slot 0");
				}

				if (vkRenderPass.vertexBuffers[0].isClosed()) {
					throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
				}
			}

			this.drawFromBuffers(vkRenderPass, i, j, indexType, vkRenderPass.pipeline);
		}
	}

	public void drawFromBuffers(VkRenderPass renderPass, int firstIndex, int vertexCount,
	                            @Nullable VertexFormat.IndexType indexType, RenderPipeline renderPipeline)
	{
		VkCommandBuffer vkCommandBuffer = Renderer.getCommandBuffer();
		VkBuffer vertexBuffer = (VkBuffer)renderPass.vertexBuffers[0];

		try (MemoryStack stack = MemoryStack.stackPush()) {
			VK11.vkCmdBindVertexBuffers(vkCommandBuffer, 0, stack.longs(vertexBuffer.buffer.getId()), stack.longs(0L));
			if (indexType != null) {
				VkBuffer indexBuffer = (VkBuffer)renderPass.indexBuffer;

				VK11.vkCmdBindIndexBuffer(vkCommandBuffer, indexBuffer.buffer.getId(), 0L, IndexBuffer.IndexType.UINT16.value);
				VK11.vkCmdDrawIndexed(vkCommandBuffer, vertexCount, 1, firstIndex, 0, 0);
			} else {
				AutoIndexBuffer autoIndexBuffer = Renderer.getDrawer().getAutoIndexBuffer(renderPipeline.getVertexFormatMode(), vertexCount);
				if (autoIndexBuffer != null) {
					int indexCount = autoIndexBuffer.getIndexCount(vertexCount);
					VK11.vkCmdBindIndexBuffer(vkCommandBuffer, autoIndexBuffer.getIndexBuffer().getId(), 0L, IndexBuffer.IndexType.UINT16.value);
					VK11.vkCmdDrawIndexed(vkCommandBuffer, indexCount, 1, firstIndex, 0, 0);
				}
				else {
					VK11.vkCmdDraw(vkCommandBuffer, vertexCount, 1, firstIndex, 0);
				}
			}
		}
	}

	public boolean trySetup(VkRenderPass renderPass) {
		if (VkRenderPass.VALIDATION) {
			if (renderPass.pipeline == null) {
				throw new IllegalStateException("Can't draw without a render pipeline");
			}

			for (RenderPipeline.UniformDescription uniformDescription : renderPass.pipeline.getUniforms()) {
				Object object = renderPass.uniforms.get(uniformDescription.name());
				if (object == null && !VkProgram.BUILT_IN_UNIFORMS.contains(uniformDescription.name())) {
					throw new IllegalStateException("Missing uniform " + uniformDescription.name() + " (should be " + uniformDescription.type() + ")");
				}
			}

		}

		setupUniforms(renderPass);

		if (renderPass.scissorState.isEnabled()) {
			GlStateManager._enableScissorTest();
			GlStateManager._scissorBox(
					renderPass.scissorState.getX(), renderPass.scissorState.getY(), renderPass.scissorState.getWidth(), renderPass.scissorState.getHeight()
			);
		} else {
			GlStateManager._disableScissorTest();
		}

		return bindPipeline(renderPass.pipeline);
	}

	// TODO
	public void setupUniforms(VkRenderPass renderPass) {
		RenderPipeline renderPipeline = renderPass.pipeline;
		VkProgram vkProgram = ExtendedRenderPipeline.of(renderPass.pipeline).getProgram();

		for (Uniform uniform : vkProgram.getUniforms()) {
			if (renderPass.dirtyUniforms.contains(uniform.getName())) {
				Object object2 = renderPass.uniforms.get(uniform.getName());
				if (object2 instanceof int[]) {
					vkProgram.safeGetUniform(uniform.getName()).set((int[])object2);
				} else if (object2 instanceof float[]) {
					vkProgram.safeGetUniform(uniform.getName()).set((float[])object2);
				} else if (object2 != null) {
					throw new IllegalStateException("Unknown uniform type - expected " + uniform.getType() + ", found " + object2);
				}
			}
		}

		renderPass.dirtyUniforms.clear();
		this.applyPipelineState(renderPipeline);
		boolean bl = this.lastProgram != vkProgram;
		if (bl) {
			this.lastProgram = vkProgram;
		}

		List<String> samplers = vkProgram.getSamplers();

		for (int i = 0; i < samplers.size(); i++) {
			String sampler = samplers.get(i);
			VkTexture glTexture = (VkTexture) renderPass.samplers.get(sampler);
			if (glTexture != null) {
				if (bl || renderPass.dirtySamplers.contains(sampler)) {
					GlStateManager._activeTexture(33984 + i);
				}

				RenderSystem.setShaderTexture(i, glTexture);
				GlStateManager._bindTexture(glTexture.glId());
				glTexture.flushModeChanges();
			}
		}

		Window window = Minecraft.getInstance().getWindow();
		vkProgram.setDefaultUniforms(
				renderPipeline.getVertexFormatMode(),
				RenderSystem.getModelViewMatrix(),
				RenderSystem.getProjectionMatrix(),
				window == null ? 0.0f : window.getWidth(),
				window == null ? 0.0f : window.getHeight()
		);
		VRenderSystem.applyModelViewMatrix(RenderSystem.getModelViewMatrix());
		VRenderSystem.applyProjectionMatrix(RenderSystem.getProjectionMatrix());
		VRenderSystem.calculateMVP();
	}

	public boolean bindPipeline(RenderPipeline renderPipeline) {
		Pipeline pipeline = ExtendedRenderPipeline.of(renderPipeline).getPipeline();

		if (pipeline == null) {
			return false;
		}

		Renderer renderer = Renderer.getInstance();
		renderer.bindGraphicsPipeline((GraphicsPipeline) pipeline);
		VTextureSelector.bindShaderTextures(pipeline);

		renderer.uploadAndBindUBOs(pipeline);

		return true;
	}

	public void applyPipelineState(RenderPipeline renderPipeline) {
		if (this.lastPipeline != renderPipeline) {
			this.lastPipeline = renderPipeline;
			if (renderPipeline.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
				GlStateManager._enableDepthTest();
				GlStateManager._depthFunc(GlConst.toGl(renderPipeline.getDepthTestFunction()));
			} else {
				GlStateManager._disableDepthTest();
			}

			if (renderPipeline.isCull()) {
				GlStateManager._enableCull();
			} else {
				GlStateManager._disableCull();
			}

			if (renderPipeline.getBlendFunction().isPresent()) {
				GlStateManager._enableBlend();
				BlendFunction blendFunction = renderPipeline.getBlendFunction().get();
				GlStateManager._blendFuncSeparate(
						GlConst.toGl(blendFunction.sourceColor()),
						GlConst.toGl(blendFunction.destColor()),
						GlConst.toGl(blendFunction.sourceAlpha()),
						GlConst.toGl(blendFunction.destAlpha())
				);
			} else {
				GlStateManager._disableBlend();
			}

			GlStateManager._polygonMode(1032, GlConst.toGl(renderPipeline.getPolygonMode()));
			GlStateManager._depthMask(renderPipeline.isWriteDepth());
			GlStateManager._colorMask(renderPipeline.isWriteColor(), renderPipeline.isWriteColor(), renderPipeline.isWriteColor(), renderPipeline.isWriteAlpha());
			if (renderPipeline.getDepthBiasConstant() == 0.0F && renderPipeline.getDepthBiasScaleFactor() == 0.0F) {
				GlStateManager._disablePolygonOffset();
			} else {
				GlStateManager._polygonOffset(renderPipeline.getDepthBiasScaleFactor(), renderPipeline.getDepthBiasConstant());
				GlStateManager._enablePolygonOffset();
			}

			switch (renderPipeline.getColorLogic()) {
				case NONE:
					GlStateManager._disableColorLogicOp();
					break;
				case OR_REVERSE:
					GlStateManager._enableColorLogicOp();
					GlStateManager._logicOp(5387);
			}

			VRenderSystem.setPrimitiveTopologyGL(GlConst.toGl(renderPipeline.getVertexFormatMode()));
		}
	}

	public void finishRenderPass() {
		this.inRenderPass = false;
	}

	protected VkDevice getDevice() {
		return this.device;
	}
}