package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

/**
 * Base class for the machines that operate on a Create Basin.
 *
 * <p>Extending {@link BasinRecipe} - exactly what Create's own {@code MixingRecipe} does - hands us
 * fluid ingredients, fluid results, item remainders, heat requirements and the "can the basin take
 * the outputs" check for free.</p>
 */
public abstract class VatRecipe extends BasinRecipe {

	protected VatRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeParams params) {
		super(typeInfo, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 2;
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 2;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 2;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
