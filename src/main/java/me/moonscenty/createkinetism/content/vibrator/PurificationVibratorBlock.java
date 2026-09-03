package me.moonscenty.createkinetism.content.vibrator;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Mekanism's Purification Chamber as a vibrating table.
 *
 * <p>Unlike the vats this is a single block, and the basin it works out of is installed into it
 * rather than placed under it - see {@link PurificationVibratorBlockEntity} for why. Right-click with
 * a Basin to fit one, right-click with an empty hand to take it back.</p>
 *
 * <p>Driven by a shaft running through it front-to-back, not a cogwheel on top - so this extends
 * {@link HorizontalAxisKineticBlock} the way Create's own Speed Controller does, and picks up its
 * axis property, its shaft-facing rule and its placement-by-neighbour behaviour for free.</p>
 */
public class PurificationVibratorBlock extends HorizontalAxisKineticBlock
	implements IBE<PurificationVibratorBlockEntity> {

	public PurificationVibratorBlock(Properties properties) {
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

	@Override
	public Class<PurificationVibratorBlockEntity> getBlockEntityClass() {
		return PurificationVibratorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PurificationVibratorBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.PURIFICATION_VIBRATOR.get();
	}
}
