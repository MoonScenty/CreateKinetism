package me.moonscenty.createkinetism.content.compressor;

import com.mojang.serialization.MapCodec;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;

/**
 * The back half of the Kinetite Compressor: the ram's housing and the cross axle.
 *
 * <p>Invisible, because the front half's model already covers both blocks. It exists for two
 * reasons: to occupy the space so nothing else can, and to be the thing a shaft on the left or right
 * connects to. That second one is why it is kinetic rather than a plain structural block like
 * Create's water wheel filler - a shaft has to have a kinetic block to attach to, and this half
 * carries its own stress load because it is genuinely a second thing to turn.</p>
 *
 * <p>{@code FACING} points at the master, so finding it is one step. Breaking either half takes the
 * other with it.</p>
 */
public class KinetiteCompressorCradleBlock extends KineticBlock implements IBE<KinetiteCompressorCradleBlockEntity> {

	public static final MapCodec<KinetiteCompressorCradleBlock> CODEC = simpleCodec(KinetiteCompressorCradleBlock::new);

	/** The direction the master lies in. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public KinetiteCompressorCradleBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends KineticBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		return pos.relative(state.getValue(FACING));
	}

	public static boolean stillValid(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof KinetiteCompressorCradleBlock
			&& level.getBlockState(masterPos(pos, state))
				.getBlock() instanceof KinetiteCompressorBlock;
	}

	/** The front half's model spans both blocks, so this one draws nothing of its own. */
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.BLOCK;
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
		Player player) {
		return new ItemStack(
			me.moonscenty.createkinetism.registry.CKBlocks.KINETITE_COMPRESSOR.get());
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock()) && stillValid(level, pos, state))
			level.destroyBlock(masterPos(pos, state), true);
		super.onRemove(state, level, pos, newState, isMoving);
	}

	/**
	 * Ask to be re-checked whenever anything next to us changes.
	 *
	 * <p>Without this the cradle has no way of hearing that its master is gone: {@link #onRemove}
	 * only fires when the cradle itself is broken, and {@link #tick} only runs if something schedules
	 * it. Breaking the front half left an invisible, unbreakable block behind until this was here.</p>
	 */
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
		LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (level instanceof Level actual && !actual.isClientSide() && !actual.getBlockTicks()
			.hasScheduledTick(pos, this))
			actual.scheduleTick(pos, this, 1);
		return state;
	}

	/** Left orphaned by a master that vanished some other way - clean up rather than linger. */
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!stillValid(level, pos, state))
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
	}

	/** Both sides, and nothing else - joining across the seam would merge the two networks. */
	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(FACING)
			.getClockWise()
			.getAxis();
	}

	/** The cross axle's own axis. Create never asks it to agree with the master's - see there. */
	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING)
			.getClockWise()
			.getAxis();
	}

	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return false;
	}

	@Override
	public Class<KinetiteCompressorCradleBlockEntity> getBlockEntityClass() {
		return KinetiteCompressorCradleBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends KinetiteCompressorCradleBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.KINETITE_COMPRESSOR_CRADLE.get();
	}
}
