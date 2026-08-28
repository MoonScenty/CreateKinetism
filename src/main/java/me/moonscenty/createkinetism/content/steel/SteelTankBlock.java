package me.moonscenty.createkinetism.content.steel;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.common.util.DeferredSoundType;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Reuses Create's own tank blockstate properties, so the vanilla tank models and the connected
 * window logic apply unchanged. Wrenching toggles the windows, unless the stack has been claimed by
 * a Distillation Controller - a column has no windows to toggle.</p>
 */
public class SteelTankBlock extends Block implements IWrenchable, IBE<SteelTankBlockEntity> {

	public static final BooleanProperty TOP = FluidTankBlock.TOP;
	public static final BooleanProperty BOTTOM = FluidTankBlock.BOTTOM;
	public static final EnumProperty<Shape> SHAPE = FluidTankBlock.SHAPE;

	/** Tanks are quieter when a whole stack goes down at once. */
	public static final SoundType SILENCED_METAL = new DeferredSoundType(0.1F, 1.5F, () -> SoundEvents.METAL_BREAK,
		() -> SoundEvents.METAL_STEP, () -> SoundEvents.METAL_PLACE, () -> SoundEvents.METAL_HIT,
		() -> SoundEvents.METAL_FALL);

	private static final VoxelShape CAMPFIRE_SMOKE_CLIP = Block.box(0, 4, 0, 16, 16, 16);

	public SteelTankBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(TOP, true)
			.setValue(BOTTOM, true)
			.setValue(SHAPE, Shape.WINDOW));
	}

	public static boolean isTank(BlockState state) {
		return state.getBlock() instanceof SteelTankBlock;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> properties) {
		properties.add(TOP, BOTTOM, SHAPE);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (oldState.getBlock() == state.getBlock() || moved)
			return;
		withBlockEntityDo(level, pos, SteelTankBlockEntity::updateConnectivityExternally);
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
		SteelTankBlockEntity tankAt = ConnectivityHandler.partAt(getBlockEntityType(), level, pos);
		if (tankAt == null)
			return 0;
		SteelTankBlockEntity controller = tankAt.getControllerBE();
		if (controller == null || !controller.hasWindows())
			return 0;
		return tankAt.getLuminosity();
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		withBlockEntityDo(context.getLevel(), context.getClickedPos(), SteelTankBlockEntity::toggleWindows);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
		CollisionContext context) {
		if (context == CollisionContext.empty())
			return CAMPFIRE_SMOKE_CLIP;
		return state.getShape(level, pos);
	}

	@Override
	protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.block();
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
		if (direction == Direction.DOWN && neighborState.getBlock() != this)
			withBlockEntityDo(level, currentPos, SteelTankBlockEntity::updateBoilerTemperature);
		return state;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity())
			return;
		if (state.getBlock() == newState.getBlock() && newState.hasBlockEntity())
			return;

		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof SteelTankBlockEntity tank))
			return;

		SteelTankBlockEntity controller = tank.getControllerBE();
		if (controller != null)
			controller.setDistillationMode(false);
		level.removeBlockEntity(pos);
		ConnectivityHandler.splitMulti(tank);
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
	public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
		SoundType soundType = super.getSoundType(state, level, pos, entity);
		if (entity != null && entity.getPersistentData()
			.contains("SilenceTankSound"))
			return SILENCED_METAL;
		return soundType;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return getBlockEntityOptional(level, pos).map(SteelTankBlockEntity::getControllerBE)
			.map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillState()))
			.orElse(0);
	}

	@Override
	public Class<SteelTankBlockEntity> getBlockEntityClass() {
		return SteelTankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SteelTankBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.STEEL_TANK.get();
	}
}
