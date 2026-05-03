package net.vulkanmod.render.engine;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.opengl.AbstractUniform;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.FogParameters;
import net.vulkanmod.util.LogUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VkProgram {
	private static final Logger LOGGER = LogUtil.getLogger();
	public static Set<String> BUILT_IN_UNIFORMS = Sets.newHashSet(
			"ModelViewMat",
			"ProjMat",
			"TextureMat",
			"ScreenSize",
			"ColorModulator",
			"Light0_Direction",
			"Light1_Direction",
			"GlintAlpha",
			"FogStart",
			"FogEnd",
			"FogColor",
			"FogShape",
			"LineWidth",
			"GameTime",
			"ModelOffset"
	);
	public static VkProgram INVALID_PROGRAM = new VkProgram(-1, "invalid");
	private static final AbstractUniform DUMMY_UNIFORM = new AbstractUniform();
	private final List<String> samplers = new ArrayList<>();
	private final Object2ObjectMap<String, GpuTexture> samplerTextures = new Object2ObjectOpenHashMap<>();
	private final IntList samplerLocations = new IntArrayList();
	private final List<Uniform> uniforms = new ArrayList<>();
	private final Map<String, Uniform> uniformsByName = new HashMap<>();
	private final int programId;
	private final String debugLabel;
	@Nullable
	public Uniform MODEL_VIEW_MATRIX;
	@Nullable
	public Uniform PROJECTION_MATRIX;
	@Nullable
	public Uniform TEXTURE_MATRIX;
	@Nullable
	public Uniform SCREEN_SIZE;
	@Nullable
	public Uniform COLOR_MODULATOR;
	@Nullable
	public Uniform LIGHT0_DIRECTION;
	@Nullable
	public Uniform LIGHT1_DIRECTION;
	@Nullable
	public Uniform GLINT_ALPHA;
	@Nullable
	public Uniform FOG_START;
	@Nullable
	public Uniform FOG_END;
	@Nullable
	public Uniform FOG_COLOR;
	@Nullable
	public Uniform FOG_SHAPE;
	@Nullable
	public Uniform LINE_WIDTH;
	@Nullable
	public Uniform GAME_TIME;
	@Nullable
	public Uniform MODEL_OFFSET;

	public VkProgram(int i, String string) {
		this.programId = i;
		this.debugLabel = string;
	}

//	public static VkProgram link(GlShaderModule glShaderModule, GlShaderModule glShaderModule2, VertexFormat vertexFormat, String string) throws ShaderManager.CompilationException {
//		int i = GlStateManager.glCreateProgram();
//		if (i <= 0) {
//			throw new ShaderManager.CompilationException("Could not create shader program (returned program ID " + i + ")");
//		} else {
//			int j = 0;

//			for (String string2 : vertexFormat.getElementAttributeNames()) {
//				GlStateManager._glBindAttribLocation(i, j, string2);
//				j++;
//			}

//			GlStateManager.glAttachShader(i, glShaderModule.getShaderId());
//			GlStateManager.glAttachShader(i, glShaderModule2.getShaderId());
//			GlStateManager.glLinkProgram(i);
//			int k = GlStateManager.glGetProgrami(i, 35714);
//			if (k == 0) {
//				String string2 = GlStateManager.glGetProgramInfoLog(i, 32768);
//				throw new ShaderManager.CompilationException(
//					"Error encountered when linking program containing VS " + glShaderModule.getId() + " and FS " + glShaderModule2.getId() + ". Log output: " + string2
//				);
//			} else {
//				return new VkProgram(i, string);
//			}
//		}
//	}

	public void setupUniforms(List<RenderPipeline.UniformDescription> list, List<String> samplers) {
		RenderSystem.assertOnRenderThread();

		for (RenderPipeline.UniformDescription uniformDescription : list) {
			String string = uniformDescription.name();
//			int i = Uniform.glGetUniformLocation(this.programId, string);
//			if (i != -1) {
				Uniform uniform = this.createUniform(uniformDescription);
//				uniform.setLocation(i);
				this.uniforms.add(uniform);
				this.uniformsByName.put(string, uniform);
//			}
		}

		for (String sampler : samplers) {
//			int j = Uniform.glGetUniformLocation(this.programId, sampler);
//			if (j == -1) {
//				LOGGER.warn("{} shader program does not use sampler {} defined in the pipeline. This might be a bug.", this.debugLabel, sampler);
//			} else {
				this.samplers.add(sampler);
//				this.samplerLocations.add(j);
//			}
		}

//		int k = GlStateManager.glGetProgrami(this.programId, 35718);

//		try (MemoryStack memoryStack = MemoryStack.stackPush()) {
//			IntBuffer intBuffer = memoryStack.mallocInt(1);
//			IntBuffer intBuffer2 = memoryStack.mallocInt(1);

//			for (int l = 0; l < k; l++) {
//				String string3 = GL20.glGetActiveUniform(this.programId, l, intBuffer, intBuffer2);
//				UniformType uniformType = getTypeFromGl(intBuffer2.get(0));
//				if (!this.uniformsByName.containsKey(string3) && !samplers.contains(string3)) {
//					if (uniformType != null) {
//						LOGGER.info("Found unknown but potentially supported uniform {} in {}", string3, this.debugLabel);
//						Uniform uniform2 = new Uniform(string3, uniformType);
//						uniform2.setLocation(l);
//						this.uniforms.add(uniform2);
//						this.uniformsByName.put(string3, uniform2);
//					} else {
//						LOGGER.warn("Found unknown and unsupported uniform {} in {}", string3, this.debugLabel);
//					}
//				}
//			}
//		}

		this.MODEL_VIEW_MATRIX = this.getUniform("ModelViewMat");
		this.PROJECTION_MATRIX = this.getUniform("ProjMat");
		this.TEXTURE_MATRIX = this.getUniform("TextureMat");
		this.SCREEN_SIZE = this.getUniform("ScreenSize");
		this.COLOR_MODULATOR = this.getUniform("ColorModulator");
		this.LIGHT0_DIRECTION = this.getUniform("Light0_Direction");
		this.LIGHT1_DIRECTION = this.getUniform("Light1_Direction");
		this.GLINT_ALPHA = this.getUniform("GlintAlpha");
		this.FOG_START = this.getUniform("FogStart");
		this.FOG_END = this.getUniform("FogEnd");
		this.FOG_COLOR = this.getUniform("FogColor");
		this.FOG_SHAPE = this.getUniform("FogShape");
		this.LINE_WIDTH = this.getUniform("LineWidth");
		this.GAME_TIME = this.getUniform("GameTime");
		this.MODEL_OFFSET = this.getUniform("ModelOffset");
	}

	private Uniform createUniform(RenderPipeline.UniformDescription uniformDescription) {
		return new Uniform(uniformDescription.name(), uniformDescription.type());
	}

//	public void close() {
//		this.uniforms.forEach(Uniform::close);
//		GlStateManager.glDeleteProgram(this.programId);
//	}

//	public void clear() {
//		RenderSystem.assertOnRenderThread();
//		GlStateManager._glUseProgram(0);
//		int i = GlStateManager._getActiveTexture();

//		for (int j = 0; j < this.samplerLocations.size(); j++) {
//			String string = (String)this.samplers.get(j);
//			if (!this.samplerTextures.containsKey(string)) {
//				GlStateManager._activeTexture(33984 + j);
//				GlStateManager._bindTexture(0);
//			}
//		}

//		GlStateManager._activeTexture(i);
//	}

	@Nullable
	public Uniform getUniform(String string) {
		RenderSystem.assertOnRenderThread();
		return this.uniformsByName.get(string);
	}

	public AbstractUniform safeGetUniform(String string) {
		Uniform uniform = this.getUniform(string);
		return uniform == null ? DUMMY_UNIFORM : uniform;
	}

	public void bindSampler(String string, @Nullable GpuTexture gpuTexture) {
		this.samplerTextures.put(string, gpuTexture);
	}

	public void setDefaultUniforms(VertexFormat.Mode mode, Matrix4f matrix4f, Matrix4f matrix4f2, float f, float g) {
		for (int i = 0; i < 12; i++) {
			GpuTexture gpuTexture = RenderSystem.getShaderTexture(i);
			this.bindSampler("Sampler" + i, gpuTexture);
		}

		if (this.MODEL_VIEW_MATRIX != null) {
			this.MODEL_VIEW_MATRIX.set(matrix4f);
		}

		if (this.PROJECTION_MATRIX != null) {
			this.PROJECTION_MATRIX.set(matrix4f2);
		}

		if (this.COLOR_MODULATOR != null) {
			this.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		}

		if (this.GLINT_ALPHA != null) {
			this.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
		}

		FogParameters fogParameters = RenderSystem.getShaderFog();
		if (this.FOG_START != null) {
			this.FOG_START.set(fogParameters.start());
		}

		if (this.FOG_END != null) {
			this.FOG_END.set(fogParameters.end());
		}

		if (this.FOG_COLOR != null) {
			this.FOG_COLOR.set(fogParameters.red(), fogParameters.green(), fogParameters.blue(), fogParameters.alpha());
		}

		if (this.FOG_SHAPE != null) {
			this.FOG_SHAPE.set(fogParameters.shape().getIndex());
		}

		if (this.TEXTURE_MATRIX != null) {
			this.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
		}

		if (this.GAME_TIME != null) {
			this.GAME_TIME.set(RenderSystem.getShaderGameTime());
		}

		if (this.MODEL_OFFSET != null) {
			this.MODEL_OFFSET.set(RenderSystem.getModelOffset());
		}

		if (this.SCREEN_SIZE != null) {
			this.SCREEN_SIZE.set(f, g);
		}

		if (this.LINE_WIDTH != null && (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP)) {
			this.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
		}

		Vector3f[] vector3fs = RenderSystem.getShaderLights();
		if (this.LIGHT0_DIRECTION != null) {
			this.LIGHT0_DIRECTION.set(vector3fs[0]);
		}

		if (this.LIGHT1_DIRECTION != null) {
			this.LIGHT1_DIRECTION.set(vector3fs[1]);
		}
	}

	@VisibleForTesting
	public int getProgramId() {
		return this.programId;
	}

	public String toString() {
		return this.debugLabel;
	}

	public String getDebugLabel() {
		return this.debugLabel;
	}

	public IntList getSamplerLocations() {
		return this.samplerLocations;
	}

	public List<String> getSamplers() {
		return this.samplers;
	}

	public List<Uniform> getUniforms() {
		return this.uniforms;
	}

	@Nullable
	private static UniformType getTypeFromGl(int i) {
		return switch (i) {
			case GL20.GL_INT -> UniformType.INT;
			case GL20.GL_FLOAT -> UniformType.FLOAT;
			case GL20.GL_FLOAT_VEC2 -> UniformType.VEC2;
			case GL20.GL_FLOAT_VEC3 -> UniformType.VEC3;
			case GL20.GL_FLOAT_VEC4 -> UniformType.VEC4;
			case GL20.GL_INT_VEC3 -> UniformType.IVEC3;
			case GL20.GL_FLOAT_MAT4 -> UniformType.MATRIX4X4;
			default -> null;
		};
	}
}