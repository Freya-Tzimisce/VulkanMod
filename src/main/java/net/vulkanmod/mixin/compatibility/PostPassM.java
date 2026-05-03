package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.render.engine.VkCommandEncoder;
import net.vulkanmod.render.engine.VkBuffer;
import net.vulkanmod.render.engine.VkDevice;
import net.vulkanmod.render.engine.VkTexture;
import net.vulkanmod.render.engine.VkRenderPass;
import net.vulkanmod.vulkan.Renderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

@Mixin(PostPass.class)
public abstract class PostPassM {
    @Shadow @Final private String name;
    @Shadow @Final private List<PostPass.Input> inputs;
    @Shadow @Final private ResourceLocation outputTargetId;
    @Shadow @Final private List<PostChainConfig.Uniform> uniforms;
    @Shadow @Final private RenderPipeline pipeline;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void addToFrame(FrameGraphBuilder frameGraphBuilder, Map<ResourceLocation, ResourceHandle<RenderTarget>> map, Matrix4f matrix4f, @Nullable Consumer<RenderPass> consumer) {
        FramePass framePass = frameGraphBuilder.addPass(this.name);

        for (PostPass.Input input : this.inputs) {
            input.addToPass(framePass, map);
        }

        ResourceHandle<RenderTarget> resourceHandle = map.computeIfPresent(
                this.outputTargetId, (resourceLocation, resourceHandlex) -> framePass.readsAndWrites((ResourceHandle<RenderTarget>)resourceHandlex)
        );
        if (resourceHandle == null) {
            throw new IllegalStateException("Missing handle for target " + this.outputTargetId);
        } else {
            framePass.executes(
                    () -> {
                        RenderTarget renderTarget = resourceHandle.get();
                        RenderSystem.backupProjectionMatrix();
                        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC);
                        VkBuffer quadVertexBuffer = (VkBuffer)RenderSystem.getQuadVertexBuffer();
                        RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                        VkBuffer indexBuffer = (VkBuffer)autoStorageIndexBuffer.getBuffer(6);
                        Renderer.getInstance().endRenderPass();

                        for (PostPass.Input inputx : this.inputs) {
                            if (inputx instanceof PostPass.TargetInput) {
                                ResourceLocation targetId = ((PostPass.TargetInput)inputx).targetId();
                                RenderTarget inTarget = map.get(targetId).get();
                                if (inTarget instanceof MainTarget) {
                                    VkTexture colorTexture = (VkTexture)inTarget.getColorTexture();
                                    colorTexture.getVulkanImage().readOnlyLayout();
                                    VkTexture depthTexture = (VkTexture)inTarget.getDepthTexture();
                                    if (depthTexture != null) {
                                        depthTexture.getVulkanImage().readOnlyLayout();
                                    }
                                }
                            }
                        }

                        try (RenderPass renderPass = RenderSystem.getDevice()
                                .createCommandEncoder()
                                .createRenderPass(
                                        renderTarget.getColorTexture(),
                                        OptionalInt.empty(),
                                        renderTarget.useDepth ? renderTarget.getDepthTexture() : null,
                                        OptionalDouble.empty()
                                )) {
                            renderPass.setPipeline(this.pipeline);
                            renderPass.setUniform("OutSize", (float)renderTarget.width, (float)renderTarget.height);
                            renderPass.setVertexBuffer(0, quadVertexBuffer);
                            renderPass.setIndexBuffer(indexBuffer, autoStorageIndexBuffer.type());

                            for (PostPass.Input inputxx : this.inputs) {
                                inputxx.bindTo(renderPass, map);
                            }

                            if (consumer != null) {
                                consumer.accept(renderPass);
                            }

                            for (PostChainConfig.Uniform uniform : this.uniforms) {
                                uniform.setOnRenderPass(renderPass);
                            }

                            VkDevice gpuDevice = (VkDevice)RenderSystem.getDevice();
                            VkCommandEncoder commandEncoder = (VkCommandEncoder)gpuDevice.createCommandEncoder();
                            commandEncoder.setupUniforms((VkRenderPass)renderPass);
                            commandEncoder.bindPipeline(((VkRenderPass)renderPass).getPipeline());

                            int indexType = switch (autoStorageIndexBuffer.type()) {
                                case SHORT -> 0;
                                case INT -> 1;
                            };
                            Renderer.getDrawer().drawIndexed(quadVertexBuffer.getBuffer(), indexBuffer.getBuffer(), 6, indexType);
                        }

                        RenderSystem.restoreProjectionMatrix();

                        for (PostPass.Input input2 : this.inputs) {
                            input2.cleanup(map);
                        }
                    }
            );
        }
    }

}