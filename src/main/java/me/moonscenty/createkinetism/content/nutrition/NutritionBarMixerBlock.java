package me.moonscenty.createkinetism.content.nutrition;

import me.moonscenty.createkinetism.content.vat.CogVatBlock;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Mekanism: Nutritional Liquifier, as a Mechanical Mixer.
 *
 * <p>Placement, drive and shape are every other vat's - a cogwheel on top, a basin a block below.
 * What is unusual is that it needs no recipes: it reads the nutrition off whatever food is in the
 * basin. See {@link NutritionBarMixerBlockEntity}.</p>
 */
public class NutritionBarMixerBlock extends CogVatBlock {

	public NutritionBarMixerBlock(Properties properties) {
		super(properties, CKRecipeTypes.NUTRITION_BAR_COOKING);
	}

	@Override
	public BlockEntityType<? extends NutritionBarMixerBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.NUTRITION_BAR_MIXER.get();
	}
}
