package net.vulkanmod.mixin.chunk;

import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Direction.class)
public class DirectionMixin {
    @Shadow @Final private static Direction[] BY_3D_DATA;

    @Redirect(method = "getOpposite()Lnet/minecraft/core/Direction;", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;from3DDataValue(I)Lnet/minecraft/core/Direction;"))
    public Direction onGetOpposite(int i) {
        return BY_3D_DATA[i];
    }
}
