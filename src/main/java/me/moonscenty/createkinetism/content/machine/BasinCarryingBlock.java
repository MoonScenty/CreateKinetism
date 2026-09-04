package me.moonscenty.createkinetism.content.machine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A machine that holds its own Create Basin - see {@link BasinCarryingBlockEntity} for why it has to.
 *
 * <p>Right-click with a Basin to fit one, right-click with an empty hand to take it back, and break
 * the block to get the basin and everything in it. That handling is identical whatever the machine
 * does with the basin once it is in, so it lives here.</p>
 *
 * <p>Driven by a shaft running through it rather than a cogwheel on top, the way Create's own Speed
 * Controller is, which is where the axis property and the placement-by-neighbour behaviour come
 * from.</p>
 */
public abstract class BasinCarryingBlock<T extends BasinCarryingBlockEntity> extends HorizontalAxisKineticBlock
	implements IBE<T> {

	protected BasinCarryingBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hitResult) {

		return onBlockEntityUseItemOn(level, pos, be -> {
			if (stack.isEmpty()) {
				if (!be.hasBasin())
					return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
				if (!level.isClientSide) {
					be.spillContents()
						.forEach(spilled -> player.getInventory()
							.placeItemBackInInventory(spilled));
					player.getInventory()
						.placeItemBackInInventory(be.removeBasin());
				}
				return ItemInteractionResult.SUCCESS;
			}

			if (!stack.is(AllBlocks.BASIN.asItem()) || be.hasBasin())
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide) {
				be.installBasin(stack);
				if (!player.isCreative())
					stack.shrink(1);
			}
			return ItemInteractionResult.SUCCESS;
		});
	}

	/** The basin and anything in it come back when the machine is broken. */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
		boolean movedByPiston) {
		if (state.getBlock() == newState.getBlock()) {
			super.onRemove(state, level, pos, newState, movedByPiston);
			return;
		}
		withBlockEntityDo(level, pos, be -> {
			be.spillContents()
				.forEach(spilled -> Block.popResource(level, pos, spilled));
			ItemStack basin = be.removeBasin();
			if (!basin.isEmpty())
				Block.popResource(level, pos, basin);
		});
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
