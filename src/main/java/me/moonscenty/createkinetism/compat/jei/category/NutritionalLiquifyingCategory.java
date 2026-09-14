package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedNutritionalLiquidMixer;

import me.moonscenty.createkinetism.content.recipe.NutritionalLiquifyingRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Nutritional Liquid Mixer recipes, one per food in the game.
 *
 * <p>The machine has no recipe files - it reads nutrition off the item - so the list JEI shows is
 * built at load from the item registry. See {@code CreateKinetismJEI.nutritionalLiquifyingRecipes}.</p>
 *
 * <p>Our own mixer picture, not Create's - the block has its own textures.</p>
 */
@ParametersAreNonnullByDefault
public class NutritionalLiquifyingCategory extends BasinRecipeCategory<NutritionalLiquifyingRecipe> {

	private final AnimatedNutritionalLiquidMixer mixer = new AnimatedNutritionalLiquidMixer();

	public NutritionalLiquifyingCategory(Info<NutritionalLiquifyingRecipe> info) {
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
