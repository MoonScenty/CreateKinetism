package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedDissolutionChamber;
import me.moonscenty.createkinetism.content.recipe.DissolvingRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Dissolution Chamber recipes. Like the Purification Vibrator it carries its own basin, so it uses
 * the same mirrored anchor; unlike it, the machine drawn here rocks rather than shakes.
 */
@ParametersAreNonnullByDefault
public class DissolvingCategory extends BasinRecipeCategory<DissolvingRecipe> {

	private final AnimatedDissolutionChamber chamber = new AnimatedDissolutionChamber();

	public DissolvingCategory(Info<DissolvingRecipe> info) {
		super(info);
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		chamber.draw(graphics, centerX, anchorY);
	}
}
