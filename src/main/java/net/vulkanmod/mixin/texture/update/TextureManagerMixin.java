package net.vulkanmod.mixin.texture.update;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.texture.TextureManager;
import net.vulkanmod.render.texture.SpriteUpdateUtil;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {
    @WrapMethod(method = "tick()V")
    public void tickWrap(Operation<Void> original) {
        if (!Renderer.skipRendering) {
            original.call();
            SpriteUpdateUtil.transitionLayouts();
        }
    }
}
