package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * Kinetite Compressor: a target item and a lump of Kinetite pressed into one.
 *
 * <p>Mekanism's Osmium Compressor takes an item and osmium; ours takes an item and Kinetite, which
 * is the metal that stands in for osmium here. Two inputs exactly - the machine has one holder for
 * each, so a recipe that wanted three would have nowhere to put the third.</p>
 */
public class KinetiteCompressingRecipe extends ChamberRecipe {

	public KinetiteCompressingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.KINETITE_COMPRESSING, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 2;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}
}
