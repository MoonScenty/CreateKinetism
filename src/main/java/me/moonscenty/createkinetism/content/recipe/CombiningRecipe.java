package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Combining Chamber: two items back into one, e.g. dust plus cobblestone into ore. */
public class CombiningRecipe extends ChamberRecipe {

	public CombiningRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.COMBINING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 9;
	}
}
