package me.moonscenty.createkinetism.content.crystallization;

import me.moonscenty.createkinetism.content.vat.VatBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A plain vat wearing the Injection Chamber's housing.
 *
 * <p>Matching and applying recipes is {@link VatBlockEntity}'s work, unchanged - the basin holds the
 * slurry going in and the crystal coming out, and the block stores nothing. The only override is the
 * head's travel, so the borrowed model animates the way it was drawn to.</p>
 */
public class CrystallizationChamberBlockEntity extends VatBlockEntity {

	/** How far past the vat's own 7px rest position the head plunges at the peak of a cycle. */
	private static final float PLUNGE_AMPLITUDE = 10 / 16f;

	public CrystallizationChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * The vat's own curve, rescaled the way the Injection Chamber rescales it: the mixer head swings
	 * almost a full block to press into the basin, but this housing only needs to read as "the plunger
	 * moved" - ten pixels rather than sixteen.
	 */
	@Override
	public float getRenderedHeadOffset(float partialTicks) {
		float hump = super.getRenderedHeadOffset(partialTicks) - 7 / 16f;
		return 7 / 16f + hump * PLUNGE_AMPLITUDE;
	}
}
