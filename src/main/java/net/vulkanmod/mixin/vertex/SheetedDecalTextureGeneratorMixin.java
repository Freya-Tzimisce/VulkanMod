package net.vulkanmod.mixin.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.render.vertex.format.I32_SNorm;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheetedDecalTextureGenerator.class)
public abstract class SheetedDecalTextureGeneratorMixin implements ExtendedVertexBuilder {
	@Shadow @Final private VertexConsumer delegate;
	@Shadow @Final private Matrix3f normalInversePose;
	@Shadow @Final private Matrix4f cameraInversePose;
	@Shadow @Final private float textureScale;

	@Unique private boolean canUseFastVertex = false;

	@Unique private Vector3f normal = new Vector3f();
	@Unique private Vector4f position = new Vector4f();

	@Override
	public boolean canUseFastVertex() {
		return this.canUseFastVertex;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void checkDelegates(VertexConsumer vertexConsumer, PoseStack.Pose pose, float f, CallbackInfo ci) {
		this.canUseFastVertex = (ExtendedVertexBuilder.of(this.delegate) != null);
	}

	@Override
	public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
		float nx = I32_SNorm.unpackX(packedNormal);
		float ny = I32_SNorm.unpackY(packedNormal);
		float nz = I32_SNorm.unpackZ(packedNormal);

		normal.set(nx, ny, nz);
		position.set(x, y, z, 1.0f);

		this.normalInversePose.transform(normal);
		Direction direction = Direction.getApproximateNearest(normal.x(), normal.y(), normal.z());
		this.cameraInversePose.transform(position);
		position.rotateY(3.1415927F);
		position.rotateX(-1.5707964F);
		position.rotate(direction.getRotation());
		float f = -position.x() * this.textureScale;
		float g = -position.y() * this.textureScale;

		final int color = 0xFFFFFFFF;
		this.delegate.addVertex(x, y, z, color, f, g, overlay, light, nx, ny, nz);
	}
}
