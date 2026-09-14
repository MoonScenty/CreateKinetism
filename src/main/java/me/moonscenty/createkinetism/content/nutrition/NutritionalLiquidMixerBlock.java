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
 * basin. See {@link NutritionalLiquidMixerBlockEntity}.</p>
 */
public class NutritionalLiquidMixerBlock extends CogVatBlock {

	public NutritionalLiquidMixerBlock(Properties properties) {
		super(properties, CKRecipeTypes.NUTRITIONAL_LIQUIFYING);
	}

	@Override
	public BlockEntityType<? extends NutritionalLiquidMixerBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.NUTRITIONAL_LIQUID_MIXER.get();
	}
}
