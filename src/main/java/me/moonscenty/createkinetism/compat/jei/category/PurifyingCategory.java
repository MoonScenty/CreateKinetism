package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedPurificationVibrator;
import me.moonscenty.createkinetism.content.recipe.PurifyingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Purification Vibrator recipes. The layout is the shared basin one; this adds the machine and the
 * oxygen.
 *
 * <p>The oxygen is a Mekanism chemical held in the machine rather than a fluid in the basin, so it
 * is not one of the fluid ingredients the basin layout knows how to place - hence the extra-input
 * hook. It still sits in the input row, because that is what it is.</p>
 */
@ParametersAreNonnullByDefault
public class PurifyingCategory extends BasinRecipeCategory<PurifyingRecipe> {

	private final AnimatedPurificationVibrator vibrator = new AnimatedPurificationVibrator();

	public PurifyingCategory(Info<PurifyingRecipe> info) {
		super(info);
	}

	@Override
	protected int extraInputSlots(PurifyingRecipe recipe) {
		return 1;
	}

	/**
	 * With its own renderer, not the one Mekanism registers globally: that one is built for the
	 * ingredient list and leaves the amount out of the tooltip, which in a recipe panel reads as the
	 * recipe not telling you the cost.
	 */
	@Override
	protected void addExtraInputSlot(IRecipeLayoutBuilder builder, PurifyingRecipe recipe, int index,
		int x, int y) {
		long amount = recipe.getRequiredChemical()
			.amount();
		builder.addSlot(RecipeIngredientRole.INPUT, x, y)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, recipe.getRequiredChemical()
				.getRepresentations());
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		vibrator.draw(graphics, centerX, anchorY);
	}
}
