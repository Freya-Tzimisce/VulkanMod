package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.PostChainConfig.Uniform;
import net.minecraft.client.renderer.PostPass.Input;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PostPass.class)
public abstract class PostPassM {
    @Shadow
    @Final
    private String name;
    @Shadow
    @Final
    private List<Input> inputs;
    @Shadow
    @Final
    private ResourceLocation outputTargetId;
    @Shadow
    @Final
    private List<Uniform> uniforms;
    @Shadow
    @Final
    private RenderPipeline pipeline;
}
