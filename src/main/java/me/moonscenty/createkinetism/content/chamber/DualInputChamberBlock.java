package me.moonscenty.createkinetism.content.chamber;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * A chamber that takes two different items and therefore needs a way to tell them apart.
 *
 * <p>This mod has no GUIs, so it follows Create's convention and settles the question with
 * geometry: the face the block points at is the <em>secondary</em> input, everything else feeds the
 * primary one. Point a funnel at the front to deliver infusion material, drop the base item in from
 * the top, pull the result out of any face.</p>
 */
public class DualInputChamberBlock extends ChamberBlock {

	public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

	public DualInputChamberBlock(Properties properties, CKRecipeTypes recipeType, int inputSlots) {
		super(properties, recipeType, inputSlots);
		registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
	}

	/** The face that feeds the second input slot. */
	public static Direction getSecondaryInputSide(BlockState state) {
		return state.hasProperty(HORIZONTAL_FACING) ? state.getValue(HORIZONTAL_FACING) : null;
	}

	@Override
	public boolean rendersCog() {
		// These use a flat casing model with a marked front face, so there is no recess to show a cog in.
		return false;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection()
			.getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
	}

	@Override
	@SuppressWarnings("deprecation") // BlockState#rotate, same idiom Create uses in its own blocks
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
	}
}
