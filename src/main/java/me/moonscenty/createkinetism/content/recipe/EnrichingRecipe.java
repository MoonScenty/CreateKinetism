package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Enrichment Chamber: one item in, one stack out. The 2x ore step. */
public class EnrichingRecipe extends ChamberRecipe {

	public EnrichingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.ENRICHING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}
}
