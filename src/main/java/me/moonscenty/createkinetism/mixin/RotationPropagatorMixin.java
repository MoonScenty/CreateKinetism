package me.moonscenty.createkinetism.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;

import me.moonscenty.createkinetism.content.accumulator.KineticAccumulatorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a large cogwheel sat on the Kinetic Accumulator belong to a different network than its shaft.
 *
 * <p>A block in Create belongs to exactly one kinetic network, so nothing built on the public API
 * can take rotation in on one face and put unrelated rotation out of another. Create has exactly one
 * block that does - the Rotation Speed Controller - and it manages it because this method names it
 * outright rather than testing an interface:</p>
 *
 * <pre>if (!ICogWheel.isLargeCog(from) || !AllBlocks.ROTATION_SPEED_CONTROLLER.has(to))</pre>
 *
 * <p>An identity check, so neither subclassing nor reimplementing gets past it. Widening it for our
 * block is the whole patch. The accumulator itself is an ordinary block of this mod's own and does
 * not extend Create's controller - it is not one: it banks what its shaft feeds it and pays it back
 * out of the cogwheel on demand.</p>
 *
 * <p>See also {@link SpeedControllerBlockEntityMixin}, which is where the speed itself comes from.</p>
 */
@Mixin(RotationPropagator.class)
public class RotationPropagatorMixin {

	@Inject(method = "isLargeCogToSpeedController", at = @At("HEAD"), cancellable = true, remap = false)
	private static void createkinetism$accumulatorTakesLargeCog(BlockState from, BlockState to, BlockPos diff,
		CallbackInfoReturnable<Boolean> cir) {

		if (!(to.getBlock() instanceof KineticAccumulatorBlock))
			return;

		// The same four tests Create applies to its own controller: a large cog, sat directly on top,
		// on edge, and crosswise to the shaft running through the block below it.
		if (!ICogWheel.isLargeCog(from))
			return;
		if (!diff.equals(BlockPos.ZERO.below()))
			return;

		Axis axis = from.getValue(CogWheelBlock.AXIS);
		if (axis.isVertical())
			return;
		if (to.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == axis)
			return;

		cir.setReturnValue(true);
	}
}
