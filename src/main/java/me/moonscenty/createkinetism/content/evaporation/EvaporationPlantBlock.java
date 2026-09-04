package me.moonscenty.createkinetism.content.evaporation;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Ported from the Steel Tank template, which itself reuses Create's own Fluid Tank properties - see
 * LICENSE-THIRD-PARTY.md.
 *
 * <p>Stacks 1-3 tall exactly like Create's tank. What is different is what happens inside: the
 * block entity slowly boils whatever it holds into the next stage of the evaporation chain, faster
 * with a heat source underneath.</p>
 */
public class EvaporationPlantBlock extends Block implements IWrenchable, IBE<EvaporationPlantBlockEntity> {

	public static final BooleanProperty TOP = FluidTankBlock.TOP;
	public static final BooleanProperty BOTTOM = FluidTankBlock.BOTTOM;
	public static final EnumProperty<Shape> SHAPE = FluidTankBlock.SHAPE;

	public EvaporationPlantBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(TOP, true)
			.setValue(BOTTOM, true)
			.setValue(SHAPE, Shape.WINDOW));
	}

	public static boolean isTank(BlockState state) {
		return state.getBlock() instanceof EvaporationPlantBlock;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(TOP, BOTTOM, SHAPE);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (oldState.getBlock() == state.getBlock() || moved)
			return;
		withBlockEntityDo(level, pos, EvaporationPlantBlockEntity::updateConnectivityExternally);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity())
			return;
		if (state.getBlock() == newState.getBlock() && newState.hasBlockEntity())
			return;

		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof EvaporationPlantBlockEntity tank))
			return;

		level.removeBlockEntity(pos);
		ConnectivityHandler.splitMulti(tank);
	}

	/**
	 * The block's outline stays a full cube for collision, but it is open on top, not sealed - so
	 * sunlight is let straight through rather than stopping dead at the first segment of the stack.
	 */
	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
		EvaporationPlantBlockEntity tankAt = ConnectivityHandler.partAt(getBlockEntityType(), level, pos);
		if (tankAt == null)
			return 0;
		EvaporationPlantBlockEntity controller = tankAt.getControllerBE();
		if (controller == null || !controller.hasWindows())
			return 0;
		return tankAt.getLuminosity();
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		withBlockEntityDo(context.getLevel(), context.getClickedPos(), EvaporationPlantBlockEntity::toggleWindows);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		if (mirror == Mirror.NONE)
			return state;
		boolean x = mirror == Mirror.FRONT_BACK;
		return switch (state.getValue(SHAPE)) {
			case WINDOW_NE -> state.setValue(SHAPE, x ? Shape.WINDOW_NW : Shape.WINDOW_SE);
			case WINDOW_NW -> state.setValue(SHAPE, x ? Shape.WINDOW_NE : Shape.WINDOW_SW);
			case WINDOW_SE -> state.setValue(SHAPE, x ? Shape.WINDOW_SW : Shape.WINDOW_NE);
			case WINDOW_SW -> state.setValue(SHAPE, x ? Shape.WINDOW_SE : Shape.WINDOW_NW);
			default -> state;
		};
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		for (int i = 0; i < rotation.ordinal(); i++)
			state = rotateOnce(state);
		return state;
	}

	private BlockState rotateOnce(BlockState state) {
		return switch (state.getValue(SHAPE)) {
			case WINDOW_NE -> state.setValue(SHAPE, Shape.WINDOW_SE);
			case WINDOW_NW -> state.setValue(SHAPE, Shape.WINDOW_NE);
			case WINDOW_SE -> state.setValue(SHAPE, Shape.WINDOW_SW);
			case WINDOW_SW -> state.setValue(SHAPE, Shape.WINDOW_NW);
			default -> state;
		};
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return getBlockEntityOptional(level, pos).map(EvaporationPlantBlockEntity::getControllerBE)
			.map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillState()))
			.orElse(0);
	}

	@Override
	public Class<EvaporationPlantBlockEntity> getBlockEntityClass() {
		return EvaporationPlantBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends EvaporationPlantBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.EVAPORATION_PLANT.get();
	}
}
