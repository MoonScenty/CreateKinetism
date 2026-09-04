package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedPurificationVibrator;
import me.moonscenty.createkinetism.content.recipe.PurifyingRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Purification Vibrator recipes. The layout is the shared basin one; all this adds is the machine.
 */
@ParametersAreNonnullByDefault
public class PurifyingCategory extends BasinRecipeCategory<PurifyingRecipe> {

	private final AnimatedPurificationVibrator vibrator = new AnimatedPurificationVibrator();

	public PurifyingCategory(Info<PurifyingRecipe> info) {
		super(info);
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		vibrator.draw(graphics, centerX, anchorY);
	}
}
