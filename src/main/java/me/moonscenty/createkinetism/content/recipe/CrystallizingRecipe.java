package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Crystallizing Vat: clean slurry to crystals. */
public class CrystallizingRecipe extends VatRecipe {

	public CrystallizingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.CRYSTALLIZING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
