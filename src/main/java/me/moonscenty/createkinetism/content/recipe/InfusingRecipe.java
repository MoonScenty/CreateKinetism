package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Metallurgic Infuser: a base item plus an infusion item. */
public class InfusingRecipe extends ChamberRecipe {

	public InfusingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.INFUSING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 9;
	}
}
