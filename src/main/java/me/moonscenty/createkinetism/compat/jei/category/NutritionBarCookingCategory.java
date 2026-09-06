package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedNutritionBarMixer;

import me.moonscenty.createkinetism.content.recipe.NutritionBarCookingRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Nutrition Bar Mixer recipes, one per food in the game.
 *
 * <p>The machine has no recipe files - it reads nutrition off the item - so the list JEI shows is
 * built at load from the item registry. See {@code CreateKinetismJEI.nutritionBarCookingRecipes}.</p>
 *
 * <p>Our own mixer picture, not Create's - the block has its own textures.</p>
 */
@ParametersAreNonnullByDefault
public class NutritionBarCookingCategory extends BasinRecipeCategory<NutritionBarCookingRecipe> {

	private final AnimatedNutritionBarMixer mixer = new AnimatedNutritionBarMixer();

	public NutritionBarCookingCategory(Info<NutritionBarCookingRecipe> info) {
		super(info);
	}

	@Override
	protected int machineAnchor() {
		return 34;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		mixer.draw(graphics, centerX, anchorY);
	}
}
