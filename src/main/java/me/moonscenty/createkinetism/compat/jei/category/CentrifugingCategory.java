package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedIsotopicCentrifuge;
import me.moonscenty.createkinetism.content.recipe.CentrifugingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Isotopic Centrifuge recipes. Carries its own basin like the Dissolution Chamber, so it takes the
 * same mirrored anchor; what differs is that the machine drawn here turns rather than tips.
 *
 * <p>A gas on each side and nothing else - no items, no fluids - so the whole panel is the two
 * extra-slot hooks the basin layout already offers, each sitting where a fluid would have been.
 * See {@link DissolvingCategory}, which does the same either side of a row of ore.</p>
 */
@ParametersAreNonnullByDefault
public class CentrifugingCategory extends BasinRecipeCategory<CentrifugingRecipe> {

	private final AnimatedIsotopicCentrifuge centrifuge = new AnimatedIsotopicCentrifuge();

	public CentrifugingCategory(Info<CentrifugingRecipe> info) {
		super(info);
	}

	@Override
	protected int extraInputSlots(CentrifugingRecipe recipe) {
		return 1;
	}

	@Override
	protected int extraOutputSlots(CentrifugingRecipe recipe) {
		return 1;
	}

	/**
	 * Both slots use their own renderer rather than the one Mekanism registers globally: that one is
	 * built for the ingredient list and leaves the amount out of the tooltip, which in a recipe panel
	 * reads as the recipe not telling you the cost.
	 */
	@Override
	protected void addExtraInputSlot(IRecipeLayoutBuilder builder, CentrifugingRecipe recipe, int index,
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
	protected void addExtraOutputSlot(IRecipeLayoutBuilder builder, CentrifugingRecipe recipe, int index,
		int x, int y) {
		long amount = recipe.chemicalOutput()
			.getAmount();
		builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, List.of(recipe.chemicalOutput()));
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		centrifuge.draw(graphics, centerX, anchorY);
	}
}
