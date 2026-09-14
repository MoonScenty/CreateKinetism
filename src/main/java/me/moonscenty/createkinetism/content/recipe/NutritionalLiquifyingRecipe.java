package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Nutritional Liquid Mixer: one food in, Nutritional Paste out, and the eaten container back.
 *
 * <p>This mod ships no {@code nutritional_liquifying} recipe at all, and that is deliberate. Like
 * Mekanism's Nutritional Liquifier, the machine reads a food's nutrition off the item and hands back
 * that much paste, which is not something a recipe file can express - see
 * {@link me.moonscenty.createkinetism.content.nutrition.NutritionalLiquidMixerBlockEntity}. The type
 * exists so a pack can still name an exception: anything written here is matched first, and only when
 * nothing does does the food rule run.</p>
 */
public class NutritionalLiquifyingRecipe extends VatRecipe {

	public NutritionalLiquifyingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.NUTRITIONAL_LIQUIFYING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	/** The container a food leaves behind once eaten - a bowl, a bottle - if it leaves one. */
	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	/** The paste. */
	@Override
	protected int getMaxFluidOutputCount() {
		return 1;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
