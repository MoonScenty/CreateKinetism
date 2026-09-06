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

	/**
	 * One pixel short of the top, where the housing actually ends - only the two thin rails reach 16.
	 *
	 * <p>It matters far more to the renderer than to anything standing on it, and it is the whole of
	 * how Create's basin avoids the same trouble. A block whose shape fills the cube is opaque as far
	 * as light is concerned: {@code ModelBlockRenderer.calculateShape} marks <em>every</em> quad as
	 * flush with the block face, so the faces inside the housing are lit from whatever sits on the
	 * other side of the wall - black the moment someone builds against the machine - {@code
	 * getShadeBrightness} drops to 0.2, and {@code propagatesSkylightDown} shuts the daylight out of
	 * the cell entirely. All three read this one answer. The basin's own shape is hollowed the same
	 * way, which is why a model with an enclosed interior and a canal poking six pixels into the next
	 * block renders correctly there with nothing else done to it.</p>
	 */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
