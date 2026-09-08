package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedMechanicalWasher;
import me.moonscenty.createkinetism.content.recipe.WashingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

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
	protected int extraInputSlots(WashingRecipe recipe) {
		return 1;
	}

	@Override
	protected int extraOutputSlots(WashingRecipe recipe) {
		return 1;
	}

	/**
	 * Both slurries use their own renderer rather than the one Mekanism registers globally: that one
	 * is built for the ingredient list and leaves the amount out of the tooltip, which in a recipe
	 * panel reads as the recipe not telling you the cost.
	 */
	@Override
	protected void addExtraInputSlot(IRecipeLayoutBuilder builder, WashingRecipe recipe, int index,
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
	protected void addExtraOutputSlot(IRecipeLayoutBuilder builder, WashingRecipe recipe, int index,
		int x, int y) {
		long amount = recipe.chemicalOutput()
			.getAmount();
		builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, List.of(recipe.chemicalOutput()));
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
