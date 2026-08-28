package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Chemical Infusion Vat: two gases into a third. */
public class ChemicalInfusingRecipe extends VatRecipe {

	public ChemicalInfusingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.CHEMICAL_INFUSING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
