package me.moonscenty.createkinetism.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;

import me.moonscenty.createkinetism.content.accumulator.KineticAccumulatorBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * What the cogwheel on top of a Kinetic Accumulator is handed.
 *
 * <p>{@link RotationPropagatorMixin} gets the connection recognised; this decides its speed. Create
 * routes that through a static method guarded by {@code instanceof SpeedControllerBlockEntity}, so
 * the accumulator has to be let in here as well - it is not one of those and does not pretend to
 * be.</p>
 *
 * <p>Only one of the two directions is ours to decide. Asked what the <em>cog</em> should turn at,
 * we answer with the dial. Asked what the <em>accumulator</em> should turn at, we have to answer
 * with the speed it is already doing off its own shaft - anything else, zero included, reads to
 * Create as two sources disagreeing, and a rotation conflict pops the block off the wall.</p>
 */
@Mixin(SpeedControllerBlockEntity.class)
public class SpeedControllerBlockEntityMixin {

	@Inject(method = "getConveyedSpeed", at = @At("HEAD"), cancellable = true, remap = false)
	private static void createkinetism$accumulatorSpeed(KineticBlockEntity cogWheel,
		KineticBlockEntity speedControllerIn, boolean targetingController, CallbackInfoReturnable<Float> cir) {

		if (!(speedControllerIn instanceof KineticAccumulatorBlockEntity accumulator))
			return;

		// A discharging accumulator is a generator, so it has no source and often no network of
		// its own yet. That is allowed for a generator and only for a generator - which is why the
		// block is one while it discharges. Silence is only for having nothing to give.
		float output = accumulator.getOutputSpeed();
		if (output == 0) {
			cir.setReturnValue(0f);
			return;
		}

		cir.setReturnValue(targetingController ? accumulator.getTheoreticalSpeed() : output);
	}
}
