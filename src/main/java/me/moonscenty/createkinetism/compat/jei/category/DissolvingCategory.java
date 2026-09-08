package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedDissolutionChamber;
import me.moonscenty.createkinetism.content.recipe.DissolvingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Dissolution Chamber recipes. Like the Purification Vibrator it carries its own basin, so it uses
 * the same mirrored anchor; unlike it, the machine drawn here rocks rather than shakes.
 *
 * <p>The only panel in this mod with a chemical on both sides. Neither is a fluid, so both go in
 * through the basin layout's extra-slot hooks - the acid in the input row, the slurry in the output
 * row, each where the fluid would have been.</p>
 */
@ParametersAreNonnullByDefault
public class DissolvingCategory extends BasinRecipeCategory<DissolvingRecipe> {

	private final AnimatedDissolutionChamber chamber = new AnimatedDissolutionChamber();

	public DissolvingCategory(Info<DissolvingRecipe> info) {
		super(info);
	}

	@Override
	protected int extraInputSlots(DissolvingRecipe recipe) {
		return 1;
	}

	@Override
	protected int extraOutputSlots(DissolvingRecipe recipe) {
		return 1;
	}

	/**
	 * Both slots use their own renderer rather than the one Mekanism registers globally: that one is
	 * built for the ingredient list and leaves the amount out of the tooltip, which in a recipe panel
	 * reads as the recipe not telling you the cost.
	 */
	@Override
	protected void addExtraInputSlot(IRecipeLayoutBuilder builder, DissolvingRecipe recipe, int index,
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
	protected void addExtraOutputSlot(IRecipeLayoutBuilder builder, DissolvingRecipe recipe, int index,
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
		chamber.draw(graphics, centerX, anchorY);
	}
}
