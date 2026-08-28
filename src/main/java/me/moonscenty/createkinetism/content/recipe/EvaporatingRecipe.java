package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Evaporation Vat: boils a fluid down. Needs a Blaze Burner underneath the basin. */
public class EvaporatingRecipe extends VatRecipe {

	public EvaporatingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.EVAPORATING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return true;
	}
}
