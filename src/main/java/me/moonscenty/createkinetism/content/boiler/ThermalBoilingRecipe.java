package me.moonscenty.createkinetism.content.boiler;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.content.recipe.VatRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/** Thermal Boiler Tank, in boiler mode: water into steam, sodium into superheated sodium. */
public class ThermalBoilingRecipe extends VatRecipe {

	public ThermalBoilingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.THERMAL_BOILING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return true;
	}
}
