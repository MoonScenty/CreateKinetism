package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedMechanicalWasher;
import me.moonscenty.createkinetism.content.recipe.WashingRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Mechanical Washer recipes. The slot layout is the shared basin one - the recipe shape is the same
 * whether or not a basin is what holds it - but the machine drawn under it stands alone.
 */
@ParametersAreNonnullByDefault
public class WashingCategory extends BasinRecipeCategory<WashingRecipe> {

	private final AnimatedMechanicalWasher washer = new AnimatedMechanicalWasher();

	public WashingCategory(Info<WashingRecipe> info) {
		super(info);
	}

	@Override
	protected int machineAnchor() {
		return 50;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		washer.draw(graphics, centerX, anchorY);
	}
}
