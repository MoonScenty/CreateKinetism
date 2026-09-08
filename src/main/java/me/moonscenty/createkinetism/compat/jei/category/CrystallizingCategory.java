package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedCrystallizationChamber;
import me.moonscenty.createkinetism.content.recipe.CrystallizingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Crystallization Chamber recipes.
 *
 * <p>{@link VatCategory}'s panel with the machine picture swapped, but the basin under this one is
 * machine - with the machine picture swapped, exactly as {@link OxidizingCategory} does it. The block
 * stopped being Create's mixer when it took the Injection Chamber's housing.</p>
 */
@ParametersAreNonnullByDefault
public class CrystallizingCategory extends BasinRecipeCategory<CrystallizingRecipe> {

	private final AnimatedCrystallizationChamber chamber = new AnimatedCrystallizationChamber();

	public CrystallizingCategory(Info<CrystallizingRecipe> info) {
		super(info);
	}

	@Override
	protected int extraInputSlots(CrystallizingRecipe recipe) {
		return 1;
	}

	/**
	 * The slurry, with its own renderer rather than the one Mekanism registers globally: that one is
	 * built for the ingredient list and leaves the amount out of the tooltip, which in a recipe panel
	 * reads as the recipe not telling you the cost.
	 */
	@Override
	protected void addExtraInputSlot(IRecipeLayoutBuilder builder, CrystallizingRecipe recipe,
		int index, int x, int y) {
		long amount = recipe.getRequiredChemical()
			.amount();
		builder.addSlot(RecipeIngredientRole.INPUT, x, y)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, recipe.getRequiredChemical()
				.getRepresentations());
	}

	@Override
	protected int machineAnchor() {
		return 34;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		chamber.draw(graphics, centerX, anchorY);
	}
}
