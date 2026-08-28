package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Oxidation Vat: a solid into a gas. */
public class OxidizingRecipe extends VatRecipe {

	public OxidizingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.OXIDIZING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
