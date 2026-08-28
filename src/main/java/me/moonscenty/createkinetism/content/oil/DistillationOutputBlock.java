package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import me.moonscenty.createkinetism.content.steel.SteelTankBlock;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKShapes;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * <p>A tap on the side of a distillation column. Placing it does the fiddly part for you: it looks
 * around for a steel tank and records that side as {@link #TANK_FACE}, then looks for a fluid pipe
 * and points its output at that instead. Which fraction it receives comes from the tank it is
 * touching - stage 1 at the bottom of the column, counting up every two layers.</p>
 *
 * <p>Give it a redstone signal and it dumps its fraction instead of storing it, which is how you get
 * rid of a cut you have no use for without backing the whole column up.</p>
 */
public class DistillationOutputBlock extends Block implements IBE<DistillationOutputBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final DirectionProperty TANK_FACE = DirectionProperty.create("tank_face");
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public DistillationOutputBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
			.setValue(TANK_FACE, Direction.DOWN)
			.setValue(POWERED, false));
	}

	/** The face fluid leaves through. */
	public static Direction getFacing(BlockState state) {
		return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
	}

	/** The face pressed against the column. */
	public static Direction getTankFace(BlockState state) {
		return state.hasProperty(TANK_FACE) ? state.getValue(TANK_FACE) : Direction.DOWN;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TANK_FACE, POWERED);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction tankDirection = Direction.DOWN;
		Direction facing = context.getNearestLookingDirection()
			.getOpposite();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		for (Direction d : Iterate.directions)
			if (level.getBlockState(pos.relative(d))
				.getBlock() instanceof SteelTankBlock) {
				tankDirection = d;
				break;
			}

		// If there is a pipe next to us, aim the output at it rather than at the player.
		for (Direction d : Iterate.directions) {
			BlockEntity neighbour = level.getBlockEntity(pos.relative(d));
			if (!(neighbour instanceof SmartBlockEntity smart))
				continue;
			if (smart.getBehaviour(FluidTransportBehaviour.TYPE) == null)
				continue;
			BlockState neighbourState = level.getBlockState(pos.relative(d));
			// A straight pipe only accepts along its own axis.
			if (smart instanceof StraightPipeBlockEntity
				&& neighbourState.getValue(AxisPipeBlock.AXIS) != d.getAxis())
				break;
			facing = d;
			break;
		}

		if (facing == tankDirection)
			facing = tankDirection.getOpposite();

		return defaultBlockState().setValue(FACING, facing)
			.setValue(TANK_FACE, tankDirection)
			.setValue(POWERED, level.hasNeighborSignal(pos));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
		boolean isMoving) {
		if (level.isClientSide)
			return;
		if (state.getValue(POWERED) != level.hasNeighborSignal(pos))
			level.setBlock(pos, state.cycle(POWERED), Block.UPDATE_CLIENTS);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.relative(state.getValue(TANK_FACE)))
			.is(CKBlocks.STEEL_TANK.get());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
			.setValue(TANK_FACE, rotation.rotate(state.getValue(TANK_FACE)));
	}

	@Override
	public Class<DistillationOutputBlockEntity> getBlockEntityClass() {
		return DistillationOutputBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DistillationOutputBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.DISTILLATION_OUTPUT.get();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CKShapes.DISTILLATION_OUTPUT.get(state.getValue(FACING));
	}
}
