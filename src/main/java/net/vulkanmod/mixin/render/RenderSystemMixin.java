package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.render.engine.VkDevice;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BiFunction;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Shadow private static Matrix4f projectionMatrix;
    @Shadow private static Matrix4f savedProjectionMatrix;
    @Shadow @Final private static Matrix4fStack modelViewStack;

    @Shadow private static Matrix4f textureMatrix;

    @Shadow @Final private static float[] shaderColor;
    @Shadow @Final private static Vector3f[] shaderLightDirections;

    @Shadow private static @Nullable Thread renderThread;

    @Shadow
    public static void assertOnRenderThread() {
    }

    @Shadow private static ProjectionType projectionType;

    @Shadow private static ProjectionType savedProjectionType;

    @Shadow private static FogParameters shaderFog;
    @Shadow private static @Nullable GpuDevice DEVICE;
    @Shadow private static String apiDescription;
    @Shadow private static @Nullable GpuBuffer QUAD_VERTEX_BUFFER;

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void initRenderer(long l, int i, boolean bl, BiFunction<ResourceLocation, ShaderType, String> biFunction, boolean bl2) {
        renderThread.setPriority(7);
        DEVICE = new VkDevice(l, i, bl, biFunction, bl2);
        VRenderSystem.initRenderer();

        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(DefaultVertexFormat.POSITION.getVertexSize() * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bufferBuilder.addVertex(1.0f, 1.0f, 0.0f);
            bufferBuilder.addVertex(0.0f, 1.0f, 0.0f);
            bufferBuilder.addVertex(0.0f, 0.0f, 0.0f);
            bufferBuilder.addVertex(1.0f, 0.0f, 0.0f);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                QUAD_VERTEX_BUFFER = RenderSystem.getDevice().createBuffer(() -> "Quad", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
            }
        }
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableScissor(int x, int y, int width, int height) {
        Renderer.setScissor(x, y, width, height);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableScissor() {
        Renderer.resetScissor();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setShaderLights(Vector3f dir0, Vector3f dir1) {
        shaderLightDirections[0] = dir0;
        shaderLightDirections[1] = dir1;

        VRenderSystem.lightDirection0.buffer.putFloat(0, dir0.x());
        VRenderSystem.lightDirection0.buffer.putFloat(4, dir0.y());
        VRenderSystem.lightDirection0.buffer.putFloat(8, dir0.z());

        VRenderSystem.lightDirection1.buffer.putFloat(0, dir1.x());
        VRenderSystem.lightDirection1.buffer.putFloat(4, dir1.y());
        VRenderSystem.lightDirection1.buffer.putFloat(8, dir1.z());
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setShaderColor(float r, float g, float b, float a) {
        shaderColor[0] = r;
        shaderColor[1] = g;
        shaderColor[2] = b;
        shaderColor[3] = a;

        VRenderSystem.setShaderColor(r, g, b, a);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setShaderFog(FogParameters fogParameters) {
        assertOnRenderThread();
        shaderFog = fogParameters;

        VRenderSystem.setShaderFogColor(fogParameters.red(), fogParameters.green(), fogParameters.blue(), fogParameters.alpha());
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setProjectionMatrix(Matrix4f projectionMatrix, ProjectionType projectionType) {
        Matrix4f matrix4f = new Matrix4f(projectionMatrix);
        RenderSystemMixin.projectionMatrix = matrix4f;
        RenderSystemMixin.projectionType = projectionType;

        VRenderSystem.applyProjectionMatrix(matrix4f);
        VRenderSystem.calculateMVP();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setTextureMatrix(Matrix4f matrix4f) {
        assertOnRenderThread();
        textureMatrix.set(matrix4f);
        VRenderSystem.setTextureMatrix(matrix4f);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void resetTextureMatrix() {
        assertOnRenderThread();
        textureMatrix.identity();
        VRenderSystem.setTextureMatrix(textureMatrix);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void restoreProjectionMatrix() {
        projectionMatrix = savedProjectionMatrix;
        projectionType = savedProjectionType;

        VRenderSystem.applyProjectionMatrix(projectionMatrix);
        VRenderSystem.calculateMVP();
    }

}
