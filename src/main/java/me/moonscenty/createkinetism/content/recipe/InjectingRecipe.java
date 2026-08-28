package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Injection Vat: item plus hydrogen chloride to shards. The 4x ore step. */
public class InjectingRecipe extends VatRecipe {

	public InjectingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.INJECTING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
