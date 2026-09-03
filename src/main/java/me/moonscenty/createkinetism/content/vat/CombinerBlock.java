package me.moonscenty.createkinetism.content.vat;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Combiner, a vat that also carries an item of its own.
 *
 * <p>Placement, shape and drive are the mixer's, exactly like the other vats. What differs is the
 * block entity: it holds the infusion item, so it needs a type of its own to carry that inventory
 * and its capability.</p>
 */
public class CombinerBlock extends VatBlock {

	public CombinerBlock(Properties properties) {
		super(properties, CKRecipeTypes.COMBINING);
	}

	/**
	 * Right click loads and unloads the infusion slot by hand: an item in hand goes in, an empty hand
	 * takes what is there back out.
	 *
	 * <p>Automation only reaches this slot from above - the sides are closed on purpose, because the
	 * gap between a vat and its basin is where the hoppers for the bulk material run - so without
	 * this the only way to correct a mistake would be to break the machine.</p>
	 */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hitResult) {

		return onBlockEntityUseItemOn(level, pos, be -> {
			if (!(be instanceof CombinerBlockEntity combiner))
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

			if (stack.isEmpty()) {
				ItemStack held = combiner.infusionInv.extractItem(0, Integer.MAX_VALUE, level.isClientSide);
				if (held.isEmpty())
					return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
				if (!level.isClientSide)
					player.getInventory()
						.placeItemBackInInventory(held);
				return ItemInteractionResult.SUCCESS;
			}

			// Simulating on the client keeps the two sides agreeing about whether anything happened,
			// without the client actually moving items around.
			ItemStack remainder = combiner.infusionInv.insertItem(0, stack, level.isClientSide);
			if (remainder.getCount() == stack.getCount())
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide)
				player.setItemInHand(hand, remainder);
			return ItemInteractionResult.SUCCESS;
		});
	}

	@Override
	public BlockEntityType<? extends VatBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.COMBINER.get();
	}
}
