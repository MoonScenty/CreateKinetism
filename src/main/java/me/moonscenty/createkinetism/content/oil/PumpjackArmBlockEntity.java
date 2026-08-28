package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>The walking beam. It is the piece that ties the assembly together: two blocks below it and two
 * along its facing sits the crank, two blocks below and two the other way sits the well. Each time
 * the crank swings past the top of its stroke the beam pulls once on the well.</p>
 *
 * <pre>
 *              [ARM]
 *   [WELL]  ..   ..   ..  [CRANK]      (both two below the arm, two out along its facing)
 * </pre>
 */
public class PumpjackArmBlockEntity extends SmartBlockEntity {

	private static final float STROKE_ANGLE = 100f;

	public boolean pumped;
	public PumpjackCrankBlockEntity crank;
	public PumpjackWellBlockEntity well;

	public PumpjackArmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(5);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null)
			return;

		Direction facing = PumpjackArmBlock.getFacing(getBlockState());
		if (facing == null)
			return;

		BlockEntity crankBE = level.getBlockEntity(getBlockPos().below(2)
			.relative(facing, 2));
		BlockEntity wellBE = level.getBlockEntity(getBlockPos().below(2)
			.relative(facing, -2));

		crank = crankBE instanceof PumpjackCrankBlockEntity found ? found : null;
		well = wellBE instanceof PumpjackWellBlockEntity found ? found : null;

		if (crank == null || well == null)
			return;
		if (Mth.abs(crank.getSpeed()) == 0f)
			return;

		float crankAngle = Mth.abs(crank.angle);
		if (crankAngle > STROKE_ANGLE && !pumped) {
			pumped = true;
			if (level.isClientSide)
				return;
			well.updateRecipe();
			well.pump();
		} else if (pumped && crankAngle < STROKE_ANGLE) {
			pumped = false;
		}
	}
}
