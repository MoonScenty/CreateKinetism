package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Isotopic Centrifuge: one gas in, one gas out.
 *
 * <p>Mekanism's machine has a single gas tank on each side and no item slots - it separates isotopes
 * out of what is already a gas. The limits say so rather than leaving the vat's 2/4/2/2 in place.</p>
 */
public class CentrifugingRecipe extends VatRecipe {

	public CentrifugingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.CENTRIFUGING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 0;
	}

	@Override
	protected int getMaxOutputCount() {
		return 0;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 1;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
