package me.moonscenty.createkinetism.content.chemistry;

import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;

import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * One of the two outer cells of the Mechanical Chemistry Infuser - see
 * {@link MechanicalChemistryInfuserBlock}.
 *
 * <p>It draws nothing; the middle block's model reaches over it. What it is for is the cell: a
 * hitbox for the side tank and its pipe, and the outer face that side's gas comes in through. That
 * face - and only that face - hands out the middle block's matching feed tank.</p>
 *
 * <p>{@link #FACING} copies the middle block's; {@link #LEFT} says which side this is.</p>
 */
public class MechanicalChemistryInfuserSideBlock extends Block implements IProxyHoveringInformation {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LEFT = BooleanProperty.create("left");

	public MechanicalChemistryInfuserSideBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
			.setValue(LEFT, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEFT);
	}

	public static BlockState stateFor(Direction facing, boolean left) {
		return CKBlocks.MECHANICAL_CHEMISTRY_INFUSER_SIDE.getDefaultState()
			.setValue(FACING, facing)
			.setValue(LEFT, left);
	}

	/** Which way the middle block is from here. */
	public static Direction towardMiddle(BlockState state) {
		Direction facing = state.getValue(FACING);
		return state.getValue(LEFT) ? facing.getCounterClockWise() : facing.getClockWise();
	}

	/** The face this side's gas comes in through: the one pointing away from the middle. */
	public static Direction outerFace(BlockState state) {
		return towardMiddle(state).getOpposite();
	}

	public static BlockPos middlePos(BlockPos pos, BlockState state) {
		return pos.relative(towardMiddle(state));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(LEFT) ? CKShapes.MECHANICAL_CHEMISTRY_INFUSER_LEFT
			: CKShapes.MECHANICAL_CHEMISTRY_INFUSER_RIGHT).get(state.getValue(FACING));
	}

	/** Goggles on a side tank read the machine, which is where the tanks live. */
	@Override
	public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
		return middlePos(pos, state);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
		Player player) {
		return CKBlocks.MECHANICAL_CHEMISTRY_INFUSER.asStack();
	}

	/** Breaking a side breaks the machine, and the machine is what drops. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			BlockPos middle = middlePos(pos, state);
			if (CKBlocks.MECHANICAL_CHEMISTRY_INFUSER.has(level.getBlockState(middle)))
				level.destroyBlock(middle, true);
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	/** Lose the middle by any means - a command, an explosion - and the side goes with it. */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (direction == towardMiddle(state) && !CKBlocks.MECHANICAL_CHEMISTRY_INFUSER.has(neighbour))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/** The outer face hands out the middle block's feed tank for this side; every other face nothing. */
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlock(Capabilities.CHEMICAL.block(), (level, pos, state, be, side) -> {
			if (side == null || side != outerFace(state))
				return null;
			if (!(level.getBlockEntity(middlePos(pos, state)) instanceof MechanicalChemistryInfuserBlockEntity machine))
				return null;
			return new SidedChemicalAccess(machine, side);
		}, CKBlocks.MECHANICAL_CHEMISTRY_INFUSER_SIDE.get());
	}
}
