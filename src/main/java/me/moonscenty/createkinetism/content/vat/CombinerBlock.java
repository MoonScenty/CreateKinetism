package me.moonscenty.createkinetism.content.vat;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

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

	@Override
	public BlockEntityType<? extends VatBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.COMBINER.get();
	}
}
