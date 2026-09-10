package me.moonscenty.createkinetism.content.solar;

import com.mojang.serialization.MapCodec;

import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
public class SolarNeutronActivatorPanelBlock extends Block implements IProxyHoveringInformation {

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

	/**
	 * Goggles pointed at the canopy read the machine below.
	 *
	 * <p>What a player looks at is the panel - it is the visible half and it is what the model puts
	 * at eye level - but the tanks and the recipe live in the block underneath, and this cell has no
	 * block entity of its own to answer with.</p>
	 */
	@Override
	public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
		return machinePos(pos);
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

	/**
	 * Lose the machine, lose the panel.
	 *
	 * <p>The machine clears this cell itself when it is broken, but only when it gets the chance to:
	 * a {@code /setblock}, a contraption pulling the machine away, an explosion that suppresses the
	 * usual updates - any of those would leave the panel standing with nothing under it. Answering
	 * the neighbour update covers all of them, and clears panels already orphaned in a world as soon
	 * as anything touches them.</p>
	 */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
		LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (direction == Direction.DOWN && !(neighbour.getBlock() instanceof SolarNeutronActivatorBlock))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}
}
