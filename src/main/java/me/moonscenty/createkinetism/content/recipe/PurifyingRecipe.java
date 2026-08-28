package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Purification Vat: item plus oxygen to clumps. The 3x ore step. */
public class PurifyingRecipe extends VatRecipe {

	public PurifyingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.PURIFYING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
