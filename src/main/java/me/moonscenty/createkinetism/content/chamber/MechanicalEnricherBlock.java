package me.moonscenty.createkinetism.content.chamber;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mekanism's Enrichment Chamber, built on Create's Mechanical Press.
 *
 * <p>This is a copy of {@code MechanicalPressBlock}, not a chamber with press art bolted on. The two
 * disagree about more than their model: a chamber is a millstone - a solid cube, driven from below,
 * holding its own inventory - while a press is a hollow frame driven from the side that works on
 * whatever sits underneath it. Keeping the art without the behaviour is what left this block telling
 * players to use a depot that it could not actually read.</p>
 *
 * <p>What makes it an enricher rather than a press lives entirely in
 * {@link MechanicalEnricherBlockEntity}: the recipe type it looks up.</p>
 */
public class MechanicalEnricherBlock extends HorizontalKineticBlock
	implements IBE<MechanicalEnricherBlockEntity> {

	public MechanicalEnricherBlock(Properties properties) {
		super(properties);
	}

	/** The press's own shape, including its trick of giving players a plain box to walk past. */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext
			&& entityContext.getEntity() instanceof Player)
			return AllShapes.CASING_14PX.get(Direction.DOWN);
		return AllShapes.MECHANICAL_PROCESSOR_SHAPE;
	}

	/** A basin belongs two blocks down with air between, never directly underneath. */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return !BasinBlock.isBasin(level, pos.below());
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction preferred = getPreferredHorizontalFacing(context);
		if (preferred != null)
			return defaultBlockState().setValue(HORIZONTAL_FACING, preferred);
		return super.getStateForPlacement(context);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public Class<MechanicalEnricherBlockEntity> getBlockEntityClass() {
		return MechanicalEnricherBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalEnricherBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.MECHANICAL_ENRICHER.get();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}
}
