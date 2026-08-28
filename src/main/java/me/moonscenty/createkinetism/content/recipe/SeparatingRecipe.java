package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Electrolytic Separator: one fluid split into two gases. */
public class SeparatingRecipe extends VatRecipe {

	public SeparatingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.SEPARATING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
