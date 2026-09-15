package me.moonscenty.createkinetism.mixin;

import java.util.Arrays;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;

import me.moonscenty.createkinetism.content.heat.CKHeatLevels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds {@code SODIUM_HEATED} and {@code CHILLED} to Create's recipe {@link HeatCondition}, next to
 * the matching heat levels from {@link HeatLevelMixin}. Appended before {@code CODEC} is built, so
 * {@code "heat_requirement": "sodium_heated"} / {@code "chilled"} parse in any processing recipe.
 *
 * <p>What a condition accepts:</p>
 * <ul>
 *   <li>None - anything, as before, Chilled included.</li>
 *   <li>Heated - Fading or hotter; Sodium Heated counts, Chilled does not.</li>
 *   <li>Super-Heated - Seething or hotter, so Sodium Heated counts too.</li>
 *   <li>Sodium Heated - only Sodium Heated.</li>
 *   <li>Chilled - only Chilled.</li>
 * </ul>
 */
@Mixin(value = HeatCondition.class, remap = false)
public abstract class HeatConditionMixin {

	@Inject(method = "$values", at = @At("RETURN"), cancellable = true)
	private static void createkinetism$appendConditions(CallbackInfoReturnable<HeatCondition[]> cir) {
		HeatCondition[] conditions = cir.getReturnValue();
		int next = conditions.length;
		conditions = Arrays.copyOf(conditions, next + 2);
		conditions[next] =
			HeatConditionAccessor.createkinetism$create("SODIUM_HEATED", next, CKHeatLevels.SODIUM_HEATED_COLOR);
		conditions[next + 1] =
			HeatConditionAccessor.createkinetism$create("CHILLED", next + 1, CKHeatLevels.CHILLED_COLOR);
		cir.setReturnValue(conditions);
	}

	@Inject(method = "testBlazeBurner", at = @At("HEAD"), cancellable = true)
	private void createkinetism$testByRank(HeatLevel level, CallbackInfoReturnable<Boolean> cir) {
		HeatCondition self = (HeatCondition) (Object) this;
		if (self == CKHeatLevels.SODIUM_HEATED_CONDITION)
			cir.setReturnValue(level == CKHeatLevels.SODIUM_HEATED);
		else if (self == CKHeatLevels.CHILLED_CONDITION)
			cir.setReturnValue(level == CKHeatLevels.CHILLED);
		else if (self == HeatCondition.SUPERHEATED)
			cir.setReturnValue(level.isAtLeast(HeatLevel.SEETHING));
		else if (self == HeatCondition.HEATED)
			cir.setReturnValue(level.isAtLeast(HeatLevel.FADING));
	}

	@Inject(method = "visualizeAsBlazeBurner", at = @At("HEAD"), cancellable = true)
	private void createkinetism$visualizeNewLevels(CallbackInfoReturnable<HeatLevel> cir) {
		HeatCondition self = (HeatCondition) (Object) this;
		if (self == CKHeatLevels.SODIUM_HEATED_CONDITION)
			cir.setReturnValue(CKHeatLevels.SODIUM_HEATED);
		else if (self == CKHeatLevels.CHILLED_CONDITION)
			cir.setReturnValue(CKHeatLevels.CHILLED);
	}
}
