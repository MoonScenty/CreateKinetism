package me.moonscenty.createkinetism.content.boiler;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Reuses Create's own tank blockstate properties, exactly like the Steel Tank, so its models work
 * unchanged - see LICENSE-THIRD-PARTY.md. No windows: every block shows the plain casing.
 *
 * <p>Unlike the Steel Tank, this one is left free to become a Create boiler: place it over a heat
 * source with water inside and it drives a Steam Engine the normal Create way. What sets it apart is
 * only what it is allowed to hold - see {@link ThermalBoilerTankBlockEntity}.</p>
 */
public class ThermalBoilerTankBlock extends Block implements IBE<ThermalBoilerTankBlockEntity> {

	public static final BooleanProperty TOP = FluidTankBlock.TOP;
	public static final BooleanProperty BOTTOM = FluidTankBlock.BOTTOM;

	public ThermalBoilerTankBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(TOP, true)
			.setValue(BOTTOM, true));
	}

	public static boolean isTank(BlockState state) {
		return state.getBlock() instanceof ThermalBoilerTankBlock;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(TOP, BOTTOM);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (oldState.getBlock() == state.getBlock() || moved)
			return;
		withBlockEntityDo(level, pos, ThermalBoilerTankBlockEntity::updateConnectivityExternally);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity())
			return;
		if (state.getBlock() == newState.getBlock() && newState.hasBlockEntity())
			return;

		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof ThermalBoilerTankBlockEntity tank))
			return;

		// A boiler is all or nothing: losing any one segment collapses the whole thing back to plain
		// tanks rather than letting Create's usual smart-reform carve out a smaller boiler.
		ThermalBoilerTankBlockEntity controller = tank.getControllerBE();
		if (controller != null && controller.boilerMode)
			controller.collapseBoiler(pos);

		level.removeBlockEntity(pos);
		ConnectivityHandler.splitMulti(tank);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
		if (direction == Direction.DOWN && neighborState.getBlock() != this)
			withBlockEntityDo(level, currentPos, ThermalBoilerTankBlockEntity::updateBoilerTemperature);
		return state;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return getBlockEntityOptional(level, pos).map(ThermalBoilerTankBlockEntity::getControllerBE)
			.map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillState()))
			.orElse(0);
	}

	@Override
	public Class<ThermalBoilerTankBlockEntity> getBlockEntityClass() {
		return ThermalBoilerTankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ThermalBoilerTankBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.THERMAL_BOILER_TANK.get();
	}
}
