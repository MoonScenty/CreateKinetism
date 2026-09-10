package me.moonscenty.createkinetism.content.boiler;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Burns Mekanism's superheated sodium back down into sodium, the way a Blaze Burner burns fuel: a
 * Thermal Boiler Tank stacked on top reads it as a heat source through Create's own
 * {@link com.simibubi.create.api.boiler.BoilerHeater} registry, no basin or GUI involved.
 *
 * <p>Driven end on end like {@link me.moonscenty.createkinetism.content.vat.MechanicalElectrolyzerBlock}
 * - a shaft goes in one horizontal face and out the other, axis following the player's line of sight
 * at placement. The shaft is optional, though: the burner works unpowered, and spinning it only
 * changes how hot a boiler above thinks it is - see {@link SodiumBurnerBlockEntity#heatLevel()}.</p>
 *
 * <p>{@link #LIT} swaps the whole texture to the burn.png variant - see
 * {@link SodiumBurnerBlockEntity#isSuperheated()} for exactly when.</p>
 */
public class SodiumBurnerBlock extends KineticBlock implements IBE<SodiumBurnerBlockEntity> {

	public static final EnumProperty<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 14 / 16d, 1);

	public SodiumBurnerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(HORIZONTAL_AXIS, Axis.Z)
			.setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_AXIS, LIT);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HORIZONTAL_AXIS, context.getHorizontalDirection()
			.getAxis());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation.rotate(Direction.NORTH)
			.getAxis() != Axis.Z)
			return state.setValue(HORIZONTAL_AXIS, state.getValue(HORIZONTAL_AXIS) == Axis.X ? Axis.Z : Axis.X);
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
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<SodiumBurnerBlockEntity> getBlockEntityClass() {
		return SodiumBurnerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SodiumBurnerBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.SODIUM_BURNER.get();
	}
}
