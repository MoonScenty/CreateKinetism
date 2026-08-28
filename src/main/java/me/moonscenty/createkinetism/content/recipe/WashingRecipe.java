package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Washing Vat: dirty slurry plus water to clean slurry. */
public class WashingRecipe extends VatRecipe {

	public WashingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.WASHING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
