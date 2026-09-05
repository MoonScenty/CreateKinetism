package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Crystallization Chamber: clean slurry into crystals, and nothing else.
 *
 * <p>Mekanism's Chemical Crystallizer has one gas tank going in and one item slot coming out. The
 * limits say so outright rather than leaving the vat's generous 2/4/2/2 in place, the same way
 * {@link OxidizingRecipe} does for the machine that runs the other direction.</p>
 */
public class CrystallizingRecipe extends VatRecipe {

	public CrystallizingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.CRYSTALLIZING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 0;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
