package me.moonscenty.createkinetism.content.vat;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The one vat that is not turned by a cogwheel.
 *
 * <p>Electrolysis is a straight-through job rather than a stirring one, so this one is driven end on
 * end: a shaft goes in one horizontal face and out the other, and there is no cog on the lid. The
 * axis follows the player's line of sight at placement, so the shaft runs away from and back towards
 * whoever put it down.</p>
 *
 * <p>Everything else - the basin underneath, the shape, the block entity - is still
 * {@link VatBlock}'s. Only the drive differs, which is also why this class does not extend
 * {@link CogVatBlock}: it must not carry the cogwheel marker.</p>
 */
public class ElectrolyticSeparatorBlock extends VatBlock {

	public static final EnumProperty<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	public ElectrolyticSeparatorBlock(Properties properties, CKRecipeTypes recipeType) {
		super(properties, recipeType);
		registerDefaultState(defaultBlockState().setValue(HORIZONTAL_AXIS, Axis.Z));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_AXIS);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// The shaft runs along the player's line of sight: face the machine and the ends point away
		// from you and back towards you. Note this is deliberately not what Create's own
		// HorizontalAxisKineticBlock does - that one adds a getClockWise() and lays the axis across
		// your view instead.
		return defaultBlockState().setValue(HORIZONTAL_AXIS, context.getHorizontalDirection()
			.getAxis());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation.rotate(Direction.NORTH)
			.getAxis() != Axis.Z)
			return state.setValue(HORIZONTAL_AXIS,
				state.getValue(HORIZONTAL_AXIS) == Axis.X ? Axis.Z : Axis.X);
		return state;
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_AXIS);
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(HORIZONTAL_AXIS);
	}

	@Override
	public BlockEntityType<? extends VatBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.ELECTROLYTIC_SEPARATOR.get();
	}
}
