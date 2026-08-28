package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.content.steel.SteelTankBlock;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Bolt this to a Steel Tank stack to turn it into a fractionating column. It mounts like Create's
 * Stressometer - any of six faces, with a second axis flag deciding which way up the gauge reads -
 * and it snaps to face whichever neighbouring tank it finds when you place it.</p>
 *
 * <p>It can only exist with a steel tank directly above it, which is what stops a column being
 * started in mid-air.</p>
 */
public class DistillationControllerBlock extends Block implements IBE<DistillationControllerBlockEntity> {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE =
		DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;

	public DistillationControllerBlock(Properties properties) {
		super(properties);
	}

	public static Direction getFacing(BlockState state) {
		return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.above())
			.is(CKBlocks.STEEL_TANK.get());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS_ALONG_FIRST_COORDINATE, FACING);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getClickedFace();
		boolean alongFirst = false;
		Axis faceAxis = facing.getAxis();

		if (faceAxis.isVertical())
			alongFirst = context.getHorizontalDirection()
				.getAxis() == Axis.Z;
		if (faceAxis == Axis.Z)
			alongFirst = true;

		// Prefer facing away from whichever tank we were placed against.
		for (Direction d : Iterate.directions) {
			BlockState neighbour = context.getLevel()
				.getBlockState(context.getClickedPos()
					.relative(d));
			if (neighbour.getBlock() instanceof SteelTankBlock) {
				facing = d.getOpposite();
				break;
			}
		}

		return defaultBlockState().setValue(FACING, facing)
			.setValue(AXIS_ALONG_FIRST_COORDINATE, alongFirst);
	}

	/**
	 * Which faces the mode dial shows on in the world. Same rules as the static version, plus a
	 * visibility check so a dial buried against a neighbouring block is not drawn - except inside a
	 * wrapped level, where Ponder needs to see it regardless.
	 */
	public static boolean shouldRenderHeadOnFace(Level level, BlockPos pos, BlockState state, Direction face) {
		if (!shouldRenderHeadOnFaceStatic(state, face))
			return false;
		return Block.shouldRenderFace(state, level, pos, face, pos.relative(face)) || level instanceof WrappedLevel;
	}

	/** Which faces the mode dial is allowed to show on, ignoring what is next to the block. */
	public static boolean shouldRenderHeadOnFaceStatic(BlockState state, Direction face) {
		if (face.getAxis()
			.isVertical())
			return false;
		if (face == state.getValue(FACING)
			.getOpposite())
			return false;
		if (face.getAxis() == getAxis(state))
			return false;
		if (getAxis(state) == Axis.Y && face != state.getValue(FACING))
			return false;
		return true;
	}

	/** The axis the dial reads along, derived from the mounting face plus the axis flag. */
	public static Axis getAxis(BlockState state) {
		Axis facingAxis = state.getValue(FACING)
			.getAxis();
		boolean alongFirst = state.getValue(AXIS_ALONG_FIRST_COORDINATE);
		return switch (facingAxis) {
			case X -> alongFirst ? Axis.Y : Axis.Z;
			case Y -> alongFirst ? Axis.X : Axis.Z;
			case Z -> alongFirst ? Axis.X : Axis.Y;
		};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return GaugeBlock.GAUGE.get(state.getValue(FACING), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public Class<DistillationControllerBlockEntity> getBlockEntityClass() {
		return DistillationControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DistillationControllerBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.DISTILLATION_CONTROLLER.get();
	}
}
