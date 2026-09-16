package me.moonscenty.createkinetism.content.turbine;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.content.steel.SteelTankBlock;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import org.jetbrains.annotations.Nullable;

/**
 * A Turbine's wall, floor and ceiling - see {@link TurbineCasingBlockEntity} for how the casings add up
 * to a turbine.
 *
 * <p>Uses Create's own tank blockstate properties, as the Steel Tank does, so the tank models and the
 * connected-texture window logic work unchanged. A Wrench toggles the windows.</p>
 */
public class TurbineCasingBlock extends Block implements IWrenchable, IBE<TurbineCasingBlockEntity> {

	public static final BooleanProperty TOP = FluidTankBlock.TOP;
	public static final BooleanProperty BOTTOM = FluidTankBlock.BOTTOM;
	public static final EnumProperty<Shape> SHAPE = FluidTankBlock.SHAPE;

	public TurbineCasingBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(TOP, true)
			.setValue(BOTTOM, true)
			.setValue(SHAPE, Shape.WINDOW));
	}

	public static boolean isCasing(BlockState state) {
		return state.getBlock() instanceof TurbineCasingBlock;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(TOP, BOTTOM, SHAPE);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (oldState.getBlock() == state.getBlock() || moved)
			return;
		withBlockEntityDo(level, pos, TurbineCasingBlockEntity::updateConnectivityExternally);
	}

	/** Quiet while a whole floor goes down at once - see {@link TurbineCasingItem}. */
	@Override
	public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
		if (entity != null && entity.getPersistentData()
			.contains(TurbineCasingItem.SILENCE_SOUND))
			return SteelTankBlock.SILENCED_METAL;
		return super.getSoundType(state, level, pos, entity);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		withBlockEntityDo(context.getLevel(), context.getClickedPos(), TurbineCasingBlockEntity::toggleWindows);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity())
			return;
		if (state.getBlock() == newState.getBlock() && newState.hasBlockEntity())
			return;
		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof TurbineCasingBlockEntity casing))
			return;
		level.removeBlockEntity(pos);
		ConnectivityHandler.splitMulti(casing);
	}

	@Override
	public Class<TurbineCasingBlockEntity> getBlockEntityClass() {
		return TurbineCasingBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TurbineCasingBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.TURBINE_CASING.get();
	}
}
