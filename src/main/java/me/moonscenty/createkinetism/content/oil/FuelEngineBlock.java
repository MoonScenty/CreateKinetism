package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.fluid.FluidHelper;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKShapes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>One block class for all three engines. It carries which fuel list the engine reads and how big
 * its tank is; everything else - the RPM dial, load-dependent burn, stress capacity - lives in
 * {@link FuelEngineBlockEntity}.</p>
 *
 * <p>Fuel goes in the bottom, the shaft comes out the front.</p>
 */
public class FuelEngineBlock extends HorizontalKineticBlock implements IBE<FuelEngineBlockEntity> {

	private final CKRecipeTypes fuelRecipeType;
	private final int tankCapacity;

	public FuelEngineBlock(Properties properties, CKRecipeTypes fuelRecipeType, int tankCapacity) {
		super(properties);
		this.fuelRecipeType = fuelRecipeType;
		this.tankCapacity = tankCapacity;
	}

	public CKRecipeTypes getFuelRecipeType() {
		return fuelRecipeType;
	}

	public int getTankCapacity() {
		return tankCapacity;
	}


	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		// Both ends of the axis, so an engine can sit in the middle of a shaft run.
		return state.getValue(HORIZONTAL_FACING)
			.getAxis() == face.getAxis();
	}

	/** Right-click with a bucket of something this engine burns to fill its tank. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hitResult) {
		return onBlockEntityUseItemOn(level, pos, be -> {
			if (stack.isEmpty())
				return ItemInteractionResult.SUCCESS;
			return FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, be) ? ItemInteractionResult.SUCCESS
				: ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		});
	}

	/**
	 * A redstone signal throttles the engine rather than switching it off: full signal stops it, and
	 * anything in between scales the RPM. Pair it with a comparator to hold a network at a set speed.
	 */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
		boolean isMoving) {
		if (level.isClientSide)
			return;
		int signal = level.getBestNeighborSignal(pos);
		withBlockEntityDo(level, pos, be -> {
			be.speedModulator = (15 - signal) / 15.0f;
			be.updateGeneratedRotation();
		});
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<FuelEngineBlockEntity> getBlockEntityClass() {
		return FuelEngineBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FuelEngineBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.FUEL_ENGINE.get();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CKShapes.SMALL_ENGINE.get(state.getValue(HORIZONTAL_FACING));
	}
}
