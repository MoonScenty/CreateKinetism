package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Create's Fluid Valve, for gases: a straight segment that a shaft opens and closes.
 *
 * <p>Not a switch. The handwheel on the side has to be <em>turned</em>, and which way it turns is
 * what opens or shuts it - so a valve is opened by running its shaft forwards and shut again by
 * reversing the same shaft. Between the two it is part way round and still closed; see
 * {@link GasValveBlockEntity}, which is where {@link #ENABLED} actually gets set.</p>
 *
 * <p>Two axes to keep straight. The gas runs along {@link #getPipeAxis}, and the shaft turns on the
 * other horizontal one - both of them fall out of {@code FACING} and
 * {@code AXIS_ALONG_FIRST_COORDINATE}, exactly as they do for Create's valve.</p>
 */
public class GasValveBlock extends DirectionalAxisKineticBlock implements IBE<GasValveBlockEntity> {

	/** Whether gas is getting through. Set by the block entity, never by the player directly. */
	public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

	public GasValveBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ENABLED, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ENABLED));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AllShapes.FLUID_VALVE.get(getPipeAxis(state));
	}

	/**
	 * The axis gas travels along: the one that is neither the shaft's nor {@code FACING}'s.
	 *
	 * <p>Create's own derivation, kept verbatim so a gas valve reads the same way in hand and in the
	 * world as the fluid valve it is modelled on.</p>
	 */
	public static Axis getPipeAxis(BlockState state) {
		if (!(state.getBlock() instanceof GasValveBlock))
			throw new IllegalStateException("Provided BlockState is for a different block.");
		Direction facing = state.getValue(FACING);
		boolean alongFirst = !state.getValue(AXIS_ALONG_FIRST_COORDINATE);
		for (Axis axis : Iterate.axes) {
			if (axis == facing.getAxis())
				continue;
			if (!alongFirst) {
				alongFirst = true;
				continue;
			}
			return axis;
		}
		throw new IllegalStateException("Impossible axis.");
	}

	/** Line up with gas pipes on the pipe axis; leave the shaft axis to Create's own preference. */
	@Override
	protected boolean prefersConnectionTo(LevelReader reader, BlockPos pos, Direction facing, boolean shaftAxis) {
		if (shaftAxis)
			return super.prefersConnectionTo(reader, pos, facing, true);
		BlockPos other = pos.relative(facing);
		if (reader.getBlockState(other)
			.getBlock() instanceof GasPipeBlock)
			return true;
		return reader instanceof Level level && level
			.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), other,
				facing.getOpposite()) != null;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<GasValveBlockEntity> getBlockEntityClass() {
		return GasValveBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends GasValveBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.GAS_VALVE.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
