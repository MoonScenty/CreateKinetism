package me.moonscenty.createkinetism.content.chemistry;

import javax.annotation.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mekanism's Chemical Infuser: two gases in, a third out of the middle. Three blocks wide.
 *
 * <p>This block is the middle one and owns everything - the tanks, the recipe and a model that
 * reaches a block out to either side. The two cells beside it are
 * {@link MechanicalChemistryInfuserSideBlock}s, put down by {@link #tick} and never placed by hand,
 * which give the side tanks a hitbox and the faces their gas comes in through. The placement is
 * refused if either cell is taken.</p>
 *
 * <p>{@link #FACING} is the direction the front - a glass wall of the middle tank - looks in. The
 * unrotated model is {@code facing=north}; seen from the front, its "Left" tank is at +X, which is
 * {@code facing.getClockWise()}, and its "Right" tank is the other way.</p>
 *
 * <p>Driven from above on the Y axis, where the model's axle sticks out of the lid.</p>
 */
public class MechanicalChemistryInfuserBlock extends KineticBlock
	implements IWrenchable, IBE<MechanicalChemistryInfuserBlockEntity> {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public MechanicalChemistryInfuserBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	/** The cell the Left tank stands in. */
	public static BlockPos leftPos(BlockPos pos, Direction facing) {
		return pos.relative(facing.getClockWise());
	}

	/** The cell the Right tank stands in. */
	public static BlockPos rightPos(BlockPos pos, Direction facing) {
		return pos.relative(facing.getCounterClockWise());
	}

	/**
	 * The middle's real volume, not a full cube - see {@link CKShapes#MECHANICAL_CHEMISTRY_INFUSER}.
	 *
	 * <p>A block whose collision shape fills the cell has every model face inside it treated as flush
	 * with the cell, and the gas behind the glass would render black.</p>
	 */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CKShapes.MECHANICAL_CHEMISTRY_INFUSER.get(state.getValue(FACING));
	}

	/** Faces the player, and only if both side cells are free - null refuses the placement. */
	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection()
			.getOpposite();
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if (!level.getBlockState(leftPos(pos, facing))
			.canBeReplaced()
			|| !level.getBlockState(rightPos(pos, facing))
				.canBeReplaced())
			return null;
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.getBlockTicks()
			.hasScheduledTick(pos, this))
			level.scheduleTick(pos, this, 1);
	}

	/**
	 * Put both side cells down, or give up if something got into either first.
	 *
	 * <p>A tick later rather than in {@code onPlace}, for the Solar Neutron Activator's reason: a
	 * structure placed straight from {@code onPlace} can land in the middle of another placement.</p>
	 */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Direction facing = state.getValue(FACING);
		BlockPos left = leftPos(pos, facing);
		BlockPos right = rightPos(pos, facing);
		BlockState wantedLeft = MechanicalChemistryInfuserSideBlock.stateFor(facing, true);
		BlockState wantedRight = MechanicalChemistryInfuserSideBlock.stateFor(facing, false);

		BlockState atLeft = level.getBlockState(left);
		BlockState atRight = level.getBlockState(right);
		boolean leftOk = atLeft == wantedLeft || atLeft.canBeReplaced();
		boolean rightOk = atRight == wantedRight || atRight.canBeReplaced();
		if (!leftOk || !rightOk) {
			level.destroyBlock(pos, true);
			return;
		}
		if (atLeft != wantedLeft)
			level.setBlockAndUpdate(left, wantedLeft);
		if (atRight != wantedRight)
			level.setBlockAndUpdate(right, wantedRight);
	}

	/**
	 * Breaking the middle clears both sides. Each is asked only what it is: by now the level already
	 * holds the new state here, so asking whether its middle is still standing would always say no.
	 */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			Direction facing = state.getValue(FACING);
			for (BlockPos side : new BlockPos[] { leftPos(pos, facing), rightPos(pos, facing) })
				if (CKBlocks.MECHANICAL_CHEMISTRY_INFUSER_SIDE.has(level.getBlockState(side)))
					level.setBlockAndUpdate(side, Blocks.AIR.defaultBlockState());
		}
		IBE.onRemove(state, level, pos, newState);
		super.onRemove(state, level, pos, newState, isMoving);
	}

	/** No turning with a Wrench - the two side cells would be left pointing the old way. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.PASS;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return Direction.Axis.Y;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.UP;
	}

	@Override
	public Class<MechanicalChemistryInfuserBlockEntity> getBlockEntityClass() {
		return MechanicalChemistryInfuserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalChemistryInfuserBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.MECHANICAL_CHEMISTRY_INFUSER.get();
	}
}
