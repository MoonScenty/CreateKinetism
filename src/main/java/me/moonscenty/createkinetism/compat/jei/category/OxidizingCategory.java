package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedOxidationChamber;
import me.moonscenty.createkinetism.content.recipe.OxidizingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Oxidation Chamber recipes.
 *
 * <p>The same panel {@link VatCategory} draws - it is a basin recipe like every other vat's, hung
 * underneath the machine - with the machine picture swapped. The block stopped being Create's mixer
 * when it took the Injection Chamber's housing, so drawing a mixer here would name the wrong
 * machine.</p>
 */
@ParametersAreNonnullByDefault
public class OxidizingCategory extends BasinRecipeCategory<OxidizingRecipe> {

	private final AnimatedOxidationChamber chamber = new AnimatedOxidationChamber();

	public OxidizingCategory(Info<OxidizingRecipe> info) {
		super(info);
	}

	@Override
	protected int extraOutputSlots(OxidizingRecipe recipe) {
		return 1;
	}

	/**
	 * The gas, with its own renderer rather than the one Mekanism registers globally: that one is
	 * built for the ingredient list and leaves the amount out of the tooltip, which in a recipe panel
	 * reads as the recipe not telling you what it makes.
	 */
	@Override
	protected void addExtraOutputSlot(IRecipeLayoutBuilder builder, OxidizingRecipe recipe, int index,
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
		return 34;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		chamber.draw(graphics, centerX, anchorY);
	}
}
