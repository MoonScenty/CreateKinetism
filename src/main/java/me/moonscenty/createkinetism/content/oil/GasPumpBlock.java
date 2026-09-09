package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Create's Mechanical Pump for Mekanism chemicals.
 *
 * <p>Fluid pipes cannot carry a gas and Mekanism's tubes will not pull one out of a machine on
 * their own, so a column full of air or of LPG sits there until something reaches in. This is that
 * something: it takes out of the block behind it and pushes into the block in front, and what it
 * pushes into is usually a Flare Stack.</p>
 *
 * <p>It is a cogwheel rather than a shaft block, exactly like the pump it is built from, so it turns
 * about the axis it faces and takes its drive from a cog on the side. How much it moves per tick
 * scales with speed - see {@link GasPumpBlockEntity}.</p>
 */
public class GasPumpBlock extends DirectionalKineticBlock implements ICogWheel, IBE<GasPumpBlockEntity> {

	public GasPumpBlock(Properties properties) {
		super(properties);
	}

	/** Wrenching flips it end for end, the way Create's pump does. */
	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		return originalState.setValue(FACING, originalState.getValue(FACING)
			.getOpposite());
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING)
			.getAxis();
	}

	/** Create's own pump shape, so the two read as the same machine in a build. */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AllShapes.PUMP.get(state.getValue(FACING));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<GasPumpBlockEntity> getBlockEntityClass() {
		return GasPumpBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends GasPumpBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.GAS_PUMP.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
