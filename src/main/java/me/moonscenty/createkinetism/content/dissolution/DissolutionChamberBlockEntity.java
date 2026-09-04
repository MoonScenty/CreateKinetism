package me.moonscenty.createkinetism.content.dissolution;

import me.moonscenty.createkinetism.content.machine.BasinCarryingBlockEntity;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mekanism: Chemical Dissolution Chamber. Raw ore plus sulfuric acid, the first 5x step.
 *
 * <p>Carries its own basin like the Purification Vibrator - see {@link BasinCarryingBlockEntity} -
 * but where that one shakes the table straight up and down, this one rocks it. Dissolving is a slow
 * swirl, not a tremor: the table tips one way, holds, and tips back, the way a lab rocker keeps acid
 * moving over ore without splashing it.</p>
 */
public class DissolutionChamberBlockEntity extends BasinCarryingBlockEntity {

	/** How far the table tips from level, in degrees. */
	public static final float ROCK_ANGLE = 7f;

	public DissolutionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.DISSOLVING;
	}

	/**
	 * The table's tilt, in degrees. Slower than the vibrator's tremor and eased at the ends of the
	 * stroke rather than sinusoidal throughout, so it reads as a deliberate rock rather than a wobble.
	 */
	public float getRockAngle(float renderTime) {
		if (!running)
			return 0;
		float frequency = 0.12f + Math.min(Math.abs(getSpeed()) / 128f, 1f) * 0.18f;
		float phase = Mth.sin(renderTime * frequency);
		// Cubing keeps the ends of the stroke slow and the middle quick - a rocker pauses at the tip.
		return phase * phase * phase * ROCK_ANGLE;
	}
}
