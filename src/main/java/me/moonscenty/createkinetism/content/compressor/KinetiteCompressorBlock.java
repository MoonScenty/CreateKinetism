package me.moonscenty.createkinetism.content.compressor;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.jetbrains.annotations.Nullable;

/**
 * The front half of the Kinetite Compressor, and the half that does the work.
 *
 * <p>The machine is two blocks deep along its facing. This one holds the spinning head and the
 * recipe; the cradle behind it holds the ram and the cross axle - see
 * {@link KinetiteCompressorCradleBlock}. Only this half is ever placed by hand: the cradle is put
 * down by {@link #tick}, and the placement is refused outright if that space is taken, the way
 * Create's Large Water Wheel refuses to go down into a blocked 3x3.</p>
 *
 * <p>The two halves are driven separately, and each is a load of its own. This one takes a shaft on
 * its front face and spins the head that holds the target; the cradle takes one on either side and
 * drives the ram. Neither answers {@link #hasShaftTowards} across the seam, which is what keeps them
 * on separate networks - a machine that wants both turning, not one shaft doing everything.</p>
 */
public class KinetiteCompressorBlock extends HorizontalKineticBlock
	implements IBE<KinetiteCompressorBlockEntity> {

	public KinetiteCompressorBlock(Properties properties) {
		super(properties);
	}

	/** Where the cradle belongs: directly behind, along the facing. */
	public static BlockPos cradlePos(BlockPos pos, BlockState state) {
		return pos.relative(state.getValue(HORIZONTAL_FACING)
			.getOpposite());
	}

	/** Null refuses the placement, which is how vanilla reports "there is no room for this". */
	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;
		if (!context.getLevel()
			.getBlockState(cradlePos(context.getClickedPos(), state))
			.canBeReplaced())
			return null;
		return state;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.getBlockTicks()
			.hasScheduledTick(pos, this))
			level.scheduleTick(pos, this, 1);
	}

	/**
	 * Put the cradle down, or give up if something got there first.
	 *
	 * <p>Deferred to a tick rather than done in {@code onPlace} for Create's reason: a structure
	 * placed straight from {@code onPlace} can land mid-way through another block's own placement.</p>
	 */
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockPos cradlePos = cradlePos(pos, state);
		BlockState occupied = level.getBlockState(cradlePos);
		BlockState wanted = CKBlocks.KINETITE_COMPRESSOR_CRADLE.getDefaultState()
			.setValue(KinetiteCompressorCradleBlock.FACING, state.getValue(HORIZONTAL_FACING));

		if (occupied == wanted)
			return;
		if (!occupied.canBeReplaced()) {
			level.destroyBlock(pos, false);
			return;
		}
		level.setBlockAndUpdate(cradlePos, wanted);
	}

	/**
	 * The front face only.
	 *
	 * <p>Deliberately not the seam toward the cradle: the two halves are meant to be driven
	 * separately, so they must not join into one network. This face turns the head that holds the
	 * target; the cradle's own sides turn the ram.</p>
	 */
	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(HORIZONTAL_FACING);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public boolean isFlammable(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
		Direction direction) {
		return false;
	}

	@Override
	public Class<KinetiteCompressorBlockEntity> getBlockEntityClass() {
		return KinetiteCompressorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends KinetiteCompressorBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.KINETITE_COMPRESSOR.get();
	}
}
