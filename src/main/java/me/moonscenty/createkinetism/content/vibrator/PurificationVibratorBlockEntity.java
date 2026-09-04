package me.moonscenty.createkinetism.content.vibrator;

import me.moonscenty.createkinetism.content.machine.BasinCarryingBlockEntity;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mekanism: Purification Chamber. The 3x ore step.
 *
 * <p>Everything about carrying its own basin lives in {@link BasinCarryingBlockEntity}. What is left
 * here is the shake: a fast, shallow tremor rather than the slow press of a vat, because this machine
 * agitates the ore rather than squashing it.</p>
 */
public class PurificationVibratorBlockEntity extends BasinCarryingBlockEntity {

	/** How far the head and basin travel from rest, in blocks. */
	public static final float SHAKE_AMPLITUDE = 1 / 16f;

	public PurificationVibratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.PURIFYING;
	}

	/** How far the head and the basin have shaken from rest. */
	public float getShakeOffset(float renderTime) {
		if (!running)
			return 0;
		float frequency = 0.6f + Math.min(Math.abs(getSpeed()) / 128f, 1f) * 0.8f;
		return Mth.sin(renderTime * frequency) * SHAKE_AMPLITUDE;
	}
}
