package common.mixins.dimthread;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.RedstoneSide;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {

	@Shadow @Final public static IntegerProperty POWER;
	@Shadow @Final public static Map<Direction, EnumProperty<RedstoneSide>> FACING_PROPERTY_MAP;

	@Shadow protected abstract BlockState getUpdatedState(IBlockReader world, BlockState state, BlockPos pos);

	/**
	 * {@code RedstoneWireBlock#wiresGivePower} is not thread-safe since it's a global flag. To ensure
	 * no interference between threads the field is replaced with this thread local one.
	 *
	 * @see RedstoneWireBlock#emitsRedstonePower(BlockState)
	 * */
	private final ThreadLocal<Boolean> wiresGivePowerSafe = ThreadLocal.withInitial(() -> true);

	@Inject(method = "getStrongestSignal", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/World;getRedstonePowerFromNeighbors(Lnet/minecraft/util/math/BlockPos;)I",
			shift = At.Shift.BEFORE))
	private void getReceivedRedstonePowerBefore(World world, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
		this.wiresGivePowerSafe.set(false);
 	}

	@Inject(method = "getStrongestSignal", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/World;getRedstonePowerFromNeighbors(Lnet/minecraft/util/math/BlockPos;)I",
			shift = At.Shift.AFTER))
	private void getReceivedRedstonePowerAfter(World world, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
		this.wiresGivePowerSafe.set(true);
	}

	/**
	 * @author DimensionalThreading (WearBlackAllDay)
	 * @reason Made redstone thread-safe, please inject in the caller.
	 */
	@Overwrite
	public boolean canProvidePower(BlockState state) {
		return this.wiresGivePowerSafe.get();
	}

	/**
	 * @author DimensionalThreading (WearBlackAllDay)
	 * @reason Made redstone thread-safe, please inject in the caller.
	 */
	@Overwrite
	public int getStrongPower(BlockState state, IBlockReader world, BlockPos pos, Direction direction) {
		return !this.wiresGivePowerSafe.get() ? 0 : state.getWeakPower(world, pos, direction);
	}

	/**
	 * @author DimensionalThreading (WearBlackAllDay)
	 * @reason Made redstone thread-safe, please inject in the caller.
	 */
	@Overwrite
	public int getWeakPower(BlockState state, IBlockReader world, BlockPos pos, Direction direction) {
		if(!this.wiresGivePowerSafe.get() || direction == Direction.DOWN) {
			return 0;
		}

		int i = state.get(POWER);
		if(i == 0)return 0;
		return direction != Direction.UP && !this.getUpdatedState(world, state, pos)
				.get(FACING_PROPERTY_MAP.get(direction.getOpposite())).func_235921_b_() ? 0 : i;
	}

}
