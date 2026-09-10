package me.moonscenty.createkinetism.content.solar;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The upper half of the Solar Neutron Activator, and the half that has to see the sky.
 *
 * <p>It draws nothing: the machine below owns a model two blocks tall and reaches up through this
 * cell. What this block is for is the cell itself - {@code canSeeSky} is asked about the space over
 * it, so the panel needs somewhere to be before there is anything to shade.</p>
 *
 * <p>Never placed by hand. The machine puts it down on its own tick and breaking either half takes
 * the other with it, the way the Kinetite Compressor's cradle does.</p>
 */
public class SolarNeutronActivatorPanelBlock extends Block {

	public static final MapCodec<SolarNeutronActivatorPanelBlock> CODEC =
		simpleCodec(SolarNeutronActivatorPanelBlock::new);

	/**
	 * The canopy, and only the part of it in this cell.
	 *
	 * <p>Kept off a full cube on purpose: a collision shape that fills the cell is what three
	 * separate lighting paths read to decide that a model's inner faces may not be lit from their own
	 * block, and the machine below is drawing into this one.</p>
	 */
	private static final VoxelShape PANEL = Shapes.box(0, 0, 0, 1, 7 / 16d, 1);

	public SolarNeutronActivatorPanelBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	/** Where the machine is: directly below. */
	public static BlockPos machinePos(BlockPos pos) {
		return pos.below();
	}

	public static boolean stillValid(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof SolarNeutronActivatorPanelBlock
			&& level.getBlockState(machinePos(pos))
				.getBlock() instanceof SolarNeutronActivatorBlock;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return PANEL;
	}

	/** Picking the panel hands over the machine, which is the only half that exists as an item. */
	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
		net.minecraft.world.entity.player.Player player) {
		return level.getBlockState(machinePos(pos))
			.getBlock()
			.asItem()
			.getDefaultInstance();
	}

	/** Breaking the panel breaks the machine, and the machine is what drops. */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock()) && stillValid(level, pos, state))
			level.destroyBlock(machinePos(pos), true);
		super.onRemove(state, level, pos, newState, isMoving);
	}

	/** Scheduled by the machine when it goes, so an orphaned panel clears itself. */
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!stillValid(level, pos, state))
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}
}
