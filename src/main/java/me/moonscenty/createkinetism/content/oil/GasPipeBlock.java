package me.moonscenty.createkinetism.content.oil;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.pathfinder.PathComputationType;

import org.jetbrains.annotations.NotNull;

/**
 * Create's Fluid Pipe, carrying Mekanism chemicals instead.
 *
 * <p>Same model and the same six-way connection logic - it joins onto another gas pipe, or onto any
 * block that offers a chemical handler to the face it is touching. What it will not do is reach into
 * one: like Create's pipes, a run of these moves nothing on its own. A {@link GasPumpBlock} pushes
 * gas in at one end and the pipes carry it from there.</p>
 *
 * <p>The connection state is recomputed on placement and whenever a neighbour changes, and only
 * written back when it actually differs - which is what stops two adjacent pipes from notifying each
 * other forever.</p>
 */
public class GasPipeBlock extends PipeBlock implements IBE<GasPipeBlockEntity> {

	public static final MapCodec<GasPipeBlock> CODEC = simpleCodec(GasPipeBlock::new);

	/** Half the core's width, in blocks. Create's pipe core is 8 wide, so 4/16. */
	private static final float APOTHEM = 4 / 16f;

	public GasPipeBlock(Properties properties) {
		super(APOTHEM, properties);
		BlockState state = stateDefinition.any();
		for (Direction side : Iterate.directions)
			state = state.setValue(PROPERTY_BY_DIRECTION.get(side), false);
		registerDefaultState(state);
	}

	@Override
	protected @NotNull MapCodec<? extends PipeBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		for (Direction side : Iterate.directions)
			builder.add(PROPERTY_BY_DIRECTION.get(side));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return connections(context.getLevel(), context.getClickedPos(), defaultBlockState());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		refresh(level, pos);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
		boolean isMoving) {
		refresh(level, pos);
	}

	private void refresh(Level level, BlockPos pos) {
		if (level.isClientSide)
			return;
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof GasPipeBlock))
			return;
		BlockState updated = connections(level, pos, state);
		// Only when it changed. A pipe that rewrote its own state unconditionally would notify its
		// neighbours, which would notify it back, and so on.
		if (updated != state)
			level.setBlock(pos, updated, Block.UPDATE_ALL);
	}

	private static BlockState connections(BlockGetter reader, BlockPos pos, BlockState state) {
		for (Direction side : Iterate.directions)
			state = state.setValue(PROPERTY_BY_DIRECTION.get(side), canConnect(reader, pos, side));
		return state;
	}

	/** Another pipe, or anything that would take a gas through that face. */
	private static boolean canConnect(BlockGetter reader, BlockPos pos, Direction side) {
		BlockPos other = pos.relative(side);
		if (reader.getBlockState(other)
			.getBlock() instanceof GasPipeBlock)
			return true;
		return reader instanceof Level level
			&& level.getCapability(Capabilities.CHEMICAL.block(), other, side.getOpposite()) != null;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<GasPipeBlockEntity> getBlockEntityClass() {
		return GasPipeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends GasPipeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.GAS_PIPE.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
