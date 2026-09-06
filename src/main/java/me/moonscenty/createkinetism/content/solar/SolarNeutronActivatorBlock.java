package me.moonscenty.createkinetism.content.solar;

import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mekanism: Solar Neutron Activator. The one machine here that takes no rotation.
 *
 * <p>Every other basin machine in this mod is a {@code VatBlock} driven by a cogwheel or a shaft,
 * because that is how a Mekanism machine's energy draw is expressed in Create's terms. This one has
 * no energy draw to express - in Mekanism it runs on daylight alone - so inventing a shaft for it
 * would be inventing a cost the machine never had. It is a plain block with a block entity, and what
 * gates it is the sky: see {@link SolarNeutronActivatorBlockEntity#hasSunlight()}.</p>
 *
 * <p>It also stands the vats' arrangement on its head: the Basin goes two blocks <em>above</em>
 * it, and the machine reaches up into it.</p>
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

	/** The gap under the basin is the machine's working space - the basin sits two above. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return !BasinBlock.isBasin(level, pos.above());
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
