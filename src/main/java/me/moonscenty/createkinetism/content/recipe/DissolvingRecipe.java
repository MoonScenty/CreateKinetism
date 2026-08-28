package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Dissolution Vat: item plus sulfuric acid to dirty slurry. The 5x ore step. */
public class DissolvingRecipe extends VatRecipe {

	public DissolvingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.DISSOLVING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
