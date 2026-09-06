package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Solar Neutron Activator: one gas in, one gas out, and nothing else.
 *
 * <p>Mekanism's machine has a single gas tank on each side and no item slots at all - what it does
 * is stand in the sun and let neutrons do the work. The limits say that outright rather than leaving
 * the vat's 2/4/2/2 in place.</p>
 */
public class ActivatingRecipe extends VatRecipe {

	public ActivatingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.ACTIVATING, params);
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
