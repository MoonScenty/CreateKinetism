package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Nutrition Bar Mixer: one item in, one item out.
 *
 * <p>This mod ships no {@code nutrition_bar_cooking} recipe at all, and that is deliberate. The machine's
 * actual job is to read a food's nutrition off the item and hand back that many bars, which is not
 * something a recipe file can express - see
 * {@link me.moonscenty.createkinetism.content.nutrition.NutritionBarMixerBlockEntity}. The type
 * exists so a pack can still name an exception: anything written here is matched first, and only
 * when nothing does does the food rule run.</p>
 */
public class NutritionBarCookingRecipe extends VatRecipe {

	public NutritionBarCookingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.NUTRITION_BAR_COOKING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
