package me.moonscenty.createkinetism.content.reaction;

import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mekanism: Pressurized Reaction Chamber. It sits <em>directly</em> on the basin.
 *
 * <p>Every other basin machine in this mod hangs a block above one, the way Create's Mixer and Press
 * do, because they reach down into it. This one sits on it: the chamber is a vessel with a basin for
 * a floor, and the shaft runs through its middle rather than into its top. That is the one thing it
 * does not inherit from {@code VatBlock}, which forbids standing on a basin outright.</p>
 */
public class PressurizedReactionChamberBlock extends HorizontalAxisKineticBlock
	implements IBE<PressurizedReactionChamberBlockEntity> {

	private static final VoxelShape HOUSING = Shapes.box(0, 0, 0, 1, 15 / 16d, 1);

	public PressurizedReactionChamberBlock(Properties properties) {
		super(properties);
	}

	/** The shaft runs straight through, so both ends of the axis take one. */
	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(HORIZONTAL_AXIS);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	/**
	 * One pixel short of the top, which is where the housing actually ends - only the two thin rails
	 * reach 16.
	 *
	 * <p>It matters far more to the renderer than to anything that walks on it. A block whose
	 * collision shape fills the cube is treated as opaque by the model lighting: {@code
	 * ModelBlockRenderer.calculateShape} then marks <em>every</em> quad as flush with the block face,
	 * so the faces inside the housing are lit from whatever block sits on the other side of the wall -
	 * pitch black as soon as someone builds against the machine - and {@code getShadeBrightness} drops
	 * to 0.2 on top of that. Not being a full cube is the one answer both of those questions read.</p>
	 */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
		CollisionContext context) {
		return HOUSING;
	}
	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<PressurizedReactionChamberBlockEntity> getBlockEntityClass() {
		return PressurizedReactionChamberBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PressurizedReactionChamberBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.PRESSURIZED_REACTION_CHAMBER.get();
	}
}
