package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Oxidation Chamber: a solid into a gas, and nothing else.
 *
 * <p>Mekanism's Chemical Oxidizer has one item slot and one gas tank, and the gas tank is an output.
 * The limits here say so outright rather than leaving the vat's generous 2/4/2/2 in place, because
 * anything wider is a recipe for a different machine - a gas going <em>in</em> means the Chemical
 * Infuser, not this.</p>
 */
public class OxidizingRecipe extends VatRecipe {

	public OxidizingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.OXIDIZING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	@Override
	protected int getMaxOutputCount() {
		return 0;
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
