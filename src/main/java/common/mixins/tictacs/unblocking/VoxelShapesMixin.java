package common.mixins.tictacs.unblocking;

import net.gegy1000.tictacs.NonBlockingWorldAccess;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IWorldReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = VoxelShapes.class, priority = 1001)
public class VoxelShapesMixin {
    @Redirect(
            method = "getAllowedOffset(Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/world/IWorldReader;DLnet/minecraft/util/math/shapes/ISelectionContext;Lnet/minecraft/util/AxisRotation;Ljava/util/stream/Stream;)D",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/IWorldReader;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
            )
    )

    private static BlockState getBlockState(IWorldReader world, BlockPos pos) {
        if (world instanceof NonBlockingWorldAccess) {
            return ((NonBlockingWorldAccess) world).getBlockStateIfLoaded(pos);
        } else {
            return world.getBlockState(pos);
        }
    }
}
