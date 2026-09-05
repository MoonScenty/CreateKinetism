package me.moonscenty.createkinetism.content.vat;

import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

/**
 * A vat driven the way Create's Mechanical Mixer is: by a cogwheel on top, turning about Y.
 *
 * <p>This is what every vat used to be, and still is bar one. It exists as its own class only
 * because {@link ICogWheel} is what lets an adjacent cog drive the machine, and the Electrolytic
 * Separator takes a shaft instead - see
 * {@link me.moonscenty.createkinetism.content.vat.ElectrolyticSeparatorBlock}. Leaving the marker on
 * the shared base would have let a cogwheel drive that one too.</p>
 */
public class CogVatBlock extends VatBlock implements ICogWheel {

	public CogVatBlock(Properties properties, CKRecipeTypes recipeType) {
		super(properties, recipeType);
	}
}
