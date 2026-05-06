package net.vulkanmod.mixin.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class VertexMultiConsumersM {

    @Mixin(targets = "com/mojang/blaze3d/vertex/VertexMultiConsumer.Double")
    public static class DoubleMixin implements ExtendedVertexBuilder {
        @Shadow @Final private VertexConsumer first;
        @Shadow @Final private VertexConsumer second;

        @Unique private ExtendedVertexBuilder firstExt;
        @Unique private ExtendedVertexBuilder secondExt;

        @Unique private boolean canUseFastVertex = false;

        @Override
        public boolean canUseFastVertex() {
            return this.canUseFastVertex;
        }

        @Inject(method = "<init>", at = @At("RETURN"))
        private void checkDelegates(VertexConsumer vertexConsumer, VertexConsumer vertexConsumer2, CallbackInfo ci) {
            this.canUseFastVertex = (ExtendedVertexBuilder.of(this.first) != null)
                    && (ExtendedVertexBuilder.of(this.second) != null);

            if (this.canUseFastVertex) {
                this.firstExt = ExtendedVertexBuilder.of(this.first);
                this.secondExt = ExtendedVertexBuilder.of(this.second);
            }
        }

        @Override
        public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
            this.firstExt.vertex(x, y, z, packedColor, u, v, overlay, light, packedNormal);
            this.secondExt.vertex(x, y, z, packedColor, u, v, overlay, light, packedNormal);
        }
    }

    @Mixin(targets = "com/mojang/blaze3d/vertex/VertexMultiConsumer$Multiple")
    public static class MultipleMixin implements ExtendedVertexBuilder {
        @Shadow @Final private VertexConsumer[] delegates;

        @Unique
        private boolean canUseFastVertex = false;

        @Override
        public boolean canUseFastVertex() {
            return this.canUseFastVertex;
        }

        @Inject(method = "<init>", at = @At("RETURN"))
        private void checkDelegates(VertexConsumer[] vertexConsumers, CallbackInfo ci) {
            for (VertexConsumer delegate : this.delegates) {
                if (ExtendedVertexBuilder.of(delegate) == null) {
                    this.canUseFastVertex = false;
                    return;
                }
            }

            this.canUseFastVertex = true;
        }

        @Override
        public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
            for (VertexConsumer vertexConsumer : this.delegates) {
                ExtendedVertexBuilder extendedVertexBuilder = (ExtendedVertexBuilder) vertexConsumer;

                extendedVertexBuilder.vertex(x, y, z, packedColor, u, v, overlay, light, packedNormal);
            }
        }
    }

}