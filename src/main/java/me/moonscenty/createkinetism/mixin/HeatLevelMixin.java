package me.moonscenty.createkinetism.mixin;

import java.util.Arrays;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import me.moonscenty.createkinetism.content.heat.CKHeatLevels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds {@code SODIUM_HEATED} and {@code CHILLED} to Create's {@link HeatLevel} - see
 * {@link CKHeatLevels}.
 *
 * <p>The constants go in where javac builds the {@code $VALUES} array, which is before the enum's
 * {@code CODEC} and before {@code BlazeBurnerBlock.HEAT_LEVEL} reads {@code values()} - so the block
 * state property, the codec and {@code valueOf} all know them from the start.</p>
 *
 * <p>Appended constants sort after Seething by ordinal, so the two methods that treat ordinal as heat
 * order are answered by {@link CKHeatLevels#rank} instead.</p>
 */
@Mixin(value = HeatLevel.class, remap = false)
public abstract class HeatLevelMixin {

	@Inject(method = "$values", at = @At("RETURN"), cancellable = true)
	private static void createkinetism$appendLevels(CallbackInfoReturnable<HeatLevel[]> cir) {
		HeatLevel[] levels = cir.getReturnValue();
		int next = levels.length;
		levels = Arrays.copyOf(levels, next + 2);
		levels[next] = HeatLevelAccessor.createkinetism$create("SODIUM_HEATED", next);
		levels[next + 1] = HeatLevelAccessor.createkinetism$create("CHILLED", next + 1);
		cir.setReturnValue(levels);
	}

	@Inject(method = "isAtLeast", at = @At("HEAD"), cancellable = true)
	private void createkinetism$compareByRank(HeatLevel other, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(CKHeatLevels.rank((HeatLevel) (Object) this) >= CKHeatLevels.rank(other));
	}

	/**
	 * A Creative Blaze Cake cycles Smouldering, Fading, Kindled, Seething and back. Create works that
	 * out from {@code values().length}, which would now walk into the two new levels as well; this keeps
	 * the cycle to Create's own four, and sends either new level back to its start.
	 */
	@Inject(method = "nextActiveLevel", at = @At("HEAD"), cancellable = true)
	private void createkinetism$cycleCreateLevelsOnly(CallbackInfoReturnable<HeatLevel> cir) {
		int ordinal = ((HeatLevel) (Object) this).ordinal();
		int seething = HeatLevel.SEETHING.ordinal();
		cir.setReturnValue(ordinal <= seething ? HeatLevel.byIndex(ordinal % seething + 1) : HeatLevel.SMOULDERING);
	}
}
