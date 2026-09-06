package me.moonscenty.createkinetism.content.centrifuge;

import me.moonscenty.createkinetism.content.machine.BasinCarryingBlockEntity;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mekanism: Isotopic Centrifuge. Nuclear waste into plutonium.
 *
 * <p>Carries its own basin like the Dissolution Chamber, and looks the same. Where that one tips the
 * table a few degrees to keep acid moving over ore, this one swings it flat about the vertical: a
 * quarter turn one way, a quarter turn back, over and over.</p>
 */
public class IsotopicCentrifugeBlockEntity extends BasinCarryingBlockEntity {

	/** How far the table turns from centre, in degrees. A quarter turn each way. */
	public static final float SWING_ANGLE = 90f;

	public IsotopicCentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.CENTRIFUGING;
	}

	/**
	 * The table's heading, in degrees, between -90 and +90.
	 *
	 * <p>Not a sine. A sine is always turning, and this has to look like something that starts from a
	 * standstill, winds up, and is caught at the far end - so the phase is a triangle wave run through
	 * smoothstep, which is zero velocity at both ends and fastest through the middle. Faster
	 * rotation only shortens the cycle; the shape of the swing stays the same.</p>
	 */
	public float getSwingAngle(float renderTime) {
		if (!running)
			return 0;
		float frequency = 0.010f + Math.min(Math.abs(getSpeed()) / 128f, 1f) * 0.020f;
		return swingAngle(renderTime * frequency);
	}

	/**
	 * The curve itself, given a position in cycles. Public and static so the JEI panel swings on
	 * exactly the same shape as the block does in world - the panel is where a player learns to tell
	 * this machine from the Dissolution Chamber, so the two must not drift apart.
	 */
	public static float swingAngle(float cycles) {
		float cycle = cycles % 2f;
		float triangle = cycle < 1 ? cycle : 2 - cycle;
		float eased = triangle * triangle * (3 - 2 * triangle);
		return (eased * 2 - 1) * SWING_ANGLE;
	}
}
