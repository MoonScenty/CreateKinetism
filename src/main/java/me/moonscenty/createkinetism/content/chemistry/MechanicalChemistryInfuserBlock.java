package me.moonscenty.createkinetism.content.chemistry;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * Mekanism's Chemical Infuser: two gases in the sides, a third out of the middle.
 *
 * <p>A single block rather than a vat over a basin. Nothing item-shaped is involved at any point -
 * two chemicals go in and one comes out - so there was never anything for a basin to hold.</p>
 *
 * <p>Driven from below on the Y axis, which is the only face left: the two side tanks take the east
 * and west faces, the main tank fills the back, and the front is the window you read it through.</p>
 *
 * <p>{@link #FACING} is the direction the front looks in. The front is the main tank's face - the
 * broad window across the back of the model, which is the side worth looking at - so the unrotated
 * model is {@code facing=north} and the two feed tanks sit behind it. The side tanks follow the
 * machine round when it is placed; see {@code MechanicalChemistryInfuserBlockEntity.leftFace}.</p>
 */
public class MechanicalChemistryInfuserBlock extends KineticBlock
	implements IWrenchable, IBE<MechanicalChemistryInfuserBlockEntity> {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/**
	 * The block's real volume, not a full cube - see {@link CKShapes#MECHANICAL_CHEMISTRY_INFUSER}.
	 *
	 * <p>This matters for more than the hitbox. A block whose collision shape fills the cell is
	 * treated by three separate lighting paths as if every one of its faces were flush with the cell
	 * boundary, and the inside of a window then renders black - which this block, being three glass
	 * tanks, is almost entirely made of. The Pressurized Reaction Chamber cost days to that before
	 * anyone thought to look at {@code getShape}.</p>
	 *
	 * <p>Looked up by facing. The blockstate's {@code y} rotation turns the model only - a
	 * {@code VoxelShape} does not follow it - so a single fixed shape was right facing north and
	 * on the wrong side of the model in every other direction.</p>
	 */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
		CollisionContext context) {
		return CKShapes.MECHANICAL_CHEMISTRY_INFUSER.get(state.getValue(FACING));
	}

	public MechanicalChemistryInfuserBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	/** Faces the player, the way every Create machine with a front does. */
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
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return Direction.Axis.Y;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.DOWN;
	}

	@Override
	public Class<MechanicalChemistryInfuserBlockEntity> getBlockEntityClass() {
		return MechanicalChemistryInfuserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalChemistryInfuserBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.MECHANICAL_CHEMISTRY_INFUSER.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
