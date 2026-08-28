package me.moonscenty.createkinetism.content.oil;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The gas turbine. Petrochem has no counterpart for it - its turbine is an FE generator - so this
 * one is built on Create's Encased Fan instead, which already has exactly the shape we want: an
 * intake on one end and a shaft on the other.
 *
 * <p>The layout follows the fan's, so the two read the same way in a build:</p>
 *
 * <ul>
 * <li><b>Front</b> ({@code HORIZONTAL_FACING}) - the fan. Nothing connects here.</li>
 * <li><b>Back</b> - the only face that gives a shaft, so rotation leaves opposite the intake.</li>
 * <li><b>The other four</b> - fuel goes in, via the capability registered in
 * {@code CKBlockEntityTypes}.</li>
 * </ul>
 *
 * <p>The block model parents Create's {@code encased_fan/block} and swaps the textures, so the
 * casing geometry stays in step with the fan's if Create ever reshapes it.</p>
 */
public class GasTurbineBlock extends FuelEngineBlock {

	public GasTurbineBlock(Properties properties, CKRecipeTypes fuelRecipeType, int tankCapacity) {
		super(properties, fuelRecipeType, tankCapacity);
	}

	/**
	 * Unlike the gasoline engine, which sits in the middle of a shaft run, the turbine only drives
	 * from the back - the front is the intake and has a fan turning in it.
	 */
	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(HORIZONTAL_FACING)
			.getOpposite();
	}

	/** A full cube, like the encased fan whose model this borrows. */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	/** The turbine's casing is a full cube, so the dial has to sit at 16 rather than inside it. */
	@Override
	public float getValueBoxDepth() {
		return 16f;
	}

	@Override
	public BlockEntityType<? extends FuelEngineBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.GAS_TURBINE.get();
	}
}
