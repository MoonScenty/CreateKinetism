package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>The driven end of the pumpjack. The shaft goes into the face it points at, and it needs at
 * least 32 RPM before the walking beam will move at all.</p>
 */
public class PumpjackCrankBlock extends HorizontalKineticBlock implements IBE<PumpjackCrankBlockEntity> {

	public PumpjackCrankBlock(Properties properties) {
		super(properties);
	}


	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(HORIZONTAL_FACING);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public Class<PumpjackCrankBlockEntity> getBlockEntityClass() {
		return PumpjackCrankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PumpjackCrankBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.PUMPJACK_CRANK.get();
	}

	@Override
	public SpeedLevel getMinimumRequiredSpeedLevel() {
		return SpeedLevel.MEDIUM;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CKShapes.PUMPJACK_CRANK.get(state.getValue(HORIZONTAL_FACING));
	}
}
