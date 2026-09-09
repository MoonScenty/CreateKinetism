package me.moonscenty.createkinetism.content.oil;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Create's Smart Fluid Pipe, for gases: one straight segment that only passes what its filter names.
 *
 * <p>Shape, model and placement are Create's, down to the way it turns to line up with whatever it
 * is placed against - see {@link #getStateForPlacement}. What it filters on is a Mekanism chemical
 * rather than a fluid; how the filter is set is {@link SmartGasPipeBlockEntity}'s business.</p>
 *
 * <p>Unlike a plain {@link GasPipeBlock} this is a <em>straight</em> segment: it joins only the two
 * ends of one axis, so it is a valve in a run rather than a junction.</p>
 */
public class SmartGasPipeBlock extends FaceAttachedHorizontalDirectionalBlock
	implements IBE<SmartGasPipeBlockEntity>, IWrenchable {

	public static final MapCodec<SmartGasPipeBlock> CODEC = simpleCodec(SmartGasPipeBlock::new);

	public SmartGasPipeBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING);
	}

	/** The axis the segment lets gas through, and the only one it connects on. */
	public static Axis getPipeAxis(BlockState state) {
		return state.getValue(FACE) == AttachFace.WALL ? Axis.Y
			: state.getValue(FACING)
				.getAxis();
	}

	public boolean isOpenAt(BlockState state, Direction side) {
		return side.getAxis() == getPipeAxis(state);
	}

	/**
	 * Line up with whatever it is being placed against.
	 *
	 * <p>Create's own logic: if every neighbour worth joining sits on one axis, take that axis, even
	 * when it means standing the segment on its side. Placing one into a gap in a run therefore just
	 * works instead of needing to be wrenched round afterwards.</p>
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;

		Axis preferred = null;
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		for (Direction side : Iterate.directions) {
			if (!prefersConnectionTo(level, pos, side))
				continue;
			if (preferred != null && preferred != side.getAxis()) {
				preferred = null;
				break;
			}
			preferred = side.getAxis();
		}

		if (preferred == Axis.Y)
			return state.setValue(FACE, AttachFace.WALL)
				.setValue(FACING, state.getValue(FACING)
					.getOpposite());

		if (preferred == null)
			return state;

		if (state.getValue(FACE) == AttachFace.WALL)
			state = state.setValue(FACE, AttachFace.FLOOR);
		for (Direction looking : context.getNearestLookingDirections()) {
			if (looking.getAxis() != preferred)
				continue;
			state = state.setValue(FACING, looking.getOpposite());
		}
		return state;
	}

	private static boolean prefersConnectionTo(Level level, BlockPos pos, Direction side) {
		BlockPos other = pos.relative(side);
		if (level.getBlockState(other)
			.getBlock() instanceof GasPipeBlock)
			return true;
		return level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), other,
			side.getOpposite()) != null;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		AttachFace face = state.getValue(FACE);
		VoxelShaper shape = face == AttachFace.FLOOR ? AllShapes.SMART_FLUID_PIPE_FLOOR
			: face == AttachFace.CEILING ? AllShapes.SMART_FLUID_PIPE_CEILING : AllShapes.SMART_FLUID_PIPE_WALL;
		return shape.get(state.getValue(FACING));
	}

	/** Never pops off. Create's does not either - a pipe holding itself up is not a puzzle. */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return true;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<SmartGasPipeBlockEntity> getBlockEntityClass() {
		return SmartGasPipeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SmartGasPipeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.SMART_GAS_PIPE.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
