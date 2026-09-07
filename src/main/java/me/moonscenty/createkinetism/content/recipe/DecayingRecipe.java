package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Radioactive Waste Drum: what decays, how fast, and into what if anything.
 *
 * <p>The rate is read the way the boiler reads its own - the ingredient's amount over the recipe's
 * duration - so {@code 2 mB} over {@code 20} ticks is two millibuckets a second. Writing it as a
 * recipe rather than a constant also decides a second question for free: <b>the drum accepts exactly
 * the fluids named here</b>, so adding a waste is one file rather than a code change.</p>
 *
 * <p>A result is allowed but not required. Mekanism's barrel simply deletes what it holds, and the
 * two recipes shipped here do the same; the slot exists so a pack can make waste decay into
 * something instead of into nothing.</p>
 */
public class DecayingRecipe extends VatRecipe {

	public DecayingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.DECAYING, params);
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
