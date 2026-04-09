package net.vulkanmod.render.chunk.build.frapi.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.chunk.build.frapi.helper.ColorHelper;
import net.vulkanmod.render.chunk.build.frapi.mesh.MutableQuadViewImpl;
import org.jetbrains.annotations.Nullable;

public class SimpleBlockRenderContext extends AbstractRenderContext {
	public static final ThreadLocal<SimpleBlockRenderContext> POOL = ThreadLocal.withInitial(SimpleBlockRenderContext::new);

	private final RandomSource random = RandomSource.create();

	private MultiBufferSource vertexConsumers;
	private RenderType defaultRenderLayer;
	private float red;
	private float green;
	private float blue;
	private int light;

	@Nullable
	private RenderType lastRenderLayer;
	@Nullable
	private VertexConsumer lastVertexConsumer;

	@Override
	protected void bufferQuad(MutableQuadViewImpl quad) {
		RenderMaterial mat = quad.material();
		BlendMode blendMode = mat.blendMode();
		RenderType renderLayer = blendMode == BlendMode.DEFAULT ? this.defaultRenderLayer : blendMode.blockRenderLayer;
		VertexConsumer vertexConsumer;

		if (renderLayer == lastRenderLayer) {
			vertexConsumer = lastVertexConsumer;
		} else {
			lastVertexConsumer = vertexConsumer = vertexConsumers.getBuffer(renderLayer);
			lastRenderLayer = renderLayer;
		}

		tintQuad(quad);
		shadeQuad(quad, mat.emissive());
		bufferQuad(quad, vertexConsumer);
	}

	private void tintQuad(MutableQuadViewImpl quad) {
		if (quad.tintIndex() != -1) {
			final float red = this.red;
			final float green = this.green;
			final float blue = this.blue;

			for (int i = 0; i < 4; i++) {
				quad.color(i, ARGB.scaleRGB(quad.color(i), red, green, blue));
			}
		}
	}

	private void shadeQuad(MutableQuadViewImpl quad, boolean emissive) {
		if (emissive) {
			for (int i = 0; i < 4; i++) {
				quad.lightmap(i, LightTexture.FULL_BRIGHT);
			}
		} else {
			final int light = this.light;

			for (int i = 0; i < 4; i++) {
				quad.lightmap(i, ColorHelper.maxLight(quad.lightmap(i), light));
			}
		}
	}

	public void bufferModel(PoseStack.Pose entry, MultiBufferSource vertexConsumers, BlockStateModel model, float red, float green, float blue, int light, int overlay, BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
		matrices = entry;
		this.overlay = overlay;

		this.vertexConsumers = vertexConsumers;
		this.defaultRenderLayer = ItemBlockRenderTypes.getRenderType(state);
		this.red = Mth.clamp(red, 0.0f, 1.0f);
		this.green = Mth.clamp(green, 0.0f, 1.0f);
		this.blue = Mth.clamp(blue, 0.0f, 1.0f);
		this.light = light;

		random.setSeed(42L);

		model.emitQuads(getEmitter(), blockView, pos, state, random, cullFace -> false);

		matrices = null;
		this.vertexConsumers = null;
		lastRenderLayer = null;
		lastVertexConsumer = null;
	}
}