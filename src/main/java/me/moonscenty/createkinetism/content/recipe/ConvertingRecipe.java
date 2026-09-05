package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Chemical Tank: one solid infusion source dissolved into the fluid form of itself.
 *
 * <p>This is the half of Mekanism's Metallurgic Infuser that this mod could not keep. Mekanism puts
 * an infusion slot on the infuser itself and converts redstone or Enriched Redstone into infusion
 * units inside the machine; our infuser is a spout, so it can only drip a fluid. The Chemical Tank
 * is that slot moved out into a block of its own - it holds the solid, turns it into the infusion
 * fluid, and feeds whatever sits underneath it.</p>
 *
 * <p>The yield is what carries Mekanism's enrichment ratio: a plain item is worth an eighth of its
 * Enriched form, so enriching is an efficiency upgrade rather than a gate.</p>
 */
public class ConvertingRecipe extends ChamberRecipe {

	public ConvertingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.CONVERTING, params);
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 1;
	}
}
