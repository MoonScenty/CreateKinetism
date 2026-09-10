package me.moonscenty.createkinetism.content.solar;

import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * Mekanism: Solar Neutron Activator. The one machine here that takes no rotation.
 *
 * <p>Every other basin machine in this mod is a {@code VatBlock} driven by a cogwheel or a shaft,
 * because that is how a Mekanism machine's energy draw is expressed in Create's terms. This one has
 * no energy draw to express - in Mekanism it runs on daylight alone - so inventing a shaft for it
 * would be inventing a cost the machine never had. It is a plain block with a block entity, and what
 * gates it is the sky: see {@link SolarNeutronActivatorBlockEntity#hasSunlight()}.</p>
 *
 * <p>Two blocks tall and no basin. A basin cannot hold a Mekanism chemical and this machine's
 * recipe is a gas on both sides, so what used to sit in one is a pair of tanks in the machine - see
 * {@link SolarNeutronActivatorBlockEntity}. The upper cell is a
 * {@link SolarNeutronActivatorPanelBlock}, put down by {@link #tick} and never placed by hand; the
 * placement is refused outright if that space is taken.</p>
 */
public class SolarNeutronActivatorBlock extends Block implements IBE<SolarNeutronActivatorBlockEntity> {

	/**
	 * The casing only.
	 *
	 * <p>The panel is five slabs that fan out a full block past the housing on every side, and none of
	 * that is something to stand on or click - it is a canopy. Keeping the shape to the casing also
	 * keeps it from being a full cube, which is what the model lighting reads to decide whether the
	 * faces inside a model may be lit from their own block rather than the neighbour's.</p>
	 */
	private static final VoxelShape CASING = Shapes.box(0, 0, 0, 1, 6 / 16d, 1);

	public SolarNeutronActivatorBlock(Properties properties) {
		super(properties);
	}

	/** Where the panel belongs: directly above. */
	public static BlockPos panelPos(BlockPos pos) {
		return pos.above();
	}

	/** Null refuses the placement, which is how vanilla reports "there is no room for this". */
	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;
		if (!context.getLevel()
			.getBlockState(panelPos(context.getClickedPos()))
			.canBeReplaced())
			return null;
		return state;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.getBlockTicks()
			.hasScheduledTick(pos, this))
			level.scheduleTick(pos, this, 1);
	}

	/**
	 * Put the panel up, or give up if something got there first.
	 *
	 * <p>Deferred to a tick rather than done in {@code onPlace} for Create's reason: a structure
	 * placed straight from {@code onPlace} can land mid-way through another block's own placement.</p>
	 */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockPos panel = panelPos(pos);
		BlockState occupied = level.getBlockState(panel);
		BlockState wanted = CKBlocks.SOLAR_NEUTRON_ACTIVATOR_PANEL.getDefaultState();

		if (occupied.is(wanted.getBlock()))
			return;
		if (!occupied.canBeReplaced()) {
			level.destroyBlock(pos, false);
			return;
		}
		level.setBlockAndUpdate(panel, wanted);
	}

	/** Breaking the machine clears its panel. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())
			&& SolarNeutronActivatorPanelBlock.stillValid(level, panelPos(pos), level.getBlockState(panelPos(pos))))
			level.setBlockAndUpdate(panelPos(pos), Blocks.AIR.defaultBlockState());
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CASING;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<SolarNeutronActivatorBlockEntity> getBlockEntityClass() {
		return SolarNeutronActivatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SolarNeutronActivatorBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.SOLAR_NEUTRON_ACTIVATOR.get();
	}
}
