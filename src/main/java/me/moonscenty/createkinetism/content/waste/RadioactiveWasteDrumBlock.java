package me.moonscenty.createkinetism.content.waste;

import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mekanism: Radioactive Waste Barrel. Somewhere to put what nothing else wants.
 *
 * <p>Not a machine and not driven: it holds waste and lets it decay on its own. What makes it worth
 * building more than one of is that a drum feeds the drum beneath it, so a column of them behaves as
 * one deep tank that drains from the bottom - see {@link RadioactiveWasteDrumBlockEntity}.</p>
 */
public class RadioactiveWasteDrumBlock extends Block implements IBE<RadioactiveWasteDrumBlockEntity> {

	/**
	 * The redrawn model has one different-looking side (see {@code block.json}'s south face using
	 * {@code side2} where the other three use {@code side1}) - it needs a facing to make that side
	 * point somewhere the player chose rather than always south.
	 */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/**
	 * The barrel's own footprint, two pixels in from every side on X/Z - matches the redrawn body
	 * (2-14 on both axes), full height (the rim strips at the very top still reach 0-16 on Y).
	 *
	 * <p>Not a full cube on purpose. Beyond being what the model actually occupies, a shape that
	 * fills the block makes the renderer light every face of the model from the neighbour rather than
	 * from this block - which is what turned the inside of two other machines here black.</p>
	 */
	private static final VoxelShape BARREL = Shapes.box(2 / 16d, 0, 2 / 16d, 14 / 16d, 1, 14 / 16d);

	public RadioactiveWasteDrumBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection()
			.getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return BARREL;
	}

	@Override
	public Class<RadioactiveWasteDrumBlockEntity> getBlockEntityClass() {
		return RadioactiveWasteDrumBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RadioactiveWasteDrumBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.RADIOACTIVE_WASTE_DRUM.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
