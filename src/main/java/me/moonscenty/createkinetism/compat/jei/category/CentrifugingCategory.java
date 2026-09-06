package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedIsotopicCentrifuge;
import me.moonscenty.createkinetism.content.recipe.CentrifugingRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Isotopic Centrifuge recipes. Carries its own basin like the Dissolution Chamber, so it takes the
 * same mirrored anchor; what differs is that the machine drawn here turns rather than tips.
 */
@ParametersAreNonnullByDefault
public class CentrifugingCategory extends BasinRecipeCategory<CentrifugingRecipe> {

	private final AnimatedIsotopicCentrifuge centrifuge = new AnimatedIsotopicCentrifuge();

	public CentrifugingCategory(Info<CentrifugingRecipe> info) {
		super(info);
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		centrifuge.draw(graphics, centerX, anchorY);
	}
}
