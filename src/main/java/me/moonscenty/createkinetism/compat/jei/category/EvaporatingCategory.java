package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedEvaporationPlant;
import me.moonscenty.createkinetism.content.recipe.EvaporatingRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Evaporation Plant recipes. The slot layout is the shared Basin one - a recipe's shape does not
 * care that the plant runs it as a slow in-place conversion rather than a Basin cycle.
 */
@ParametersAreNonnullByDefault
public class EvaporatingCategory extends BasinRecipeCategory<EvaporatingRecipe> {

	private final AnimatedEvaporationPlant plant = new AnimatedEvaporationPlant();

	public EvaporatingCategory(Info<EvaporatingRecipe> info) {
		super(info);
	}

	@Override
	protected int machineAnchor() {
		return 82;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		plant.draw(graphics, centerX, anchorY);
	}
}
