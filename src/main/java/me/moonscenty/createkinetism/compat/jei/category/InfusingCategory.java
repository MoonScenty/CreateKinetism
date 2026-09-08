package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mekanism.api.chemical.ChemicalStack;
import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedInfuser;
import me.moonscenty.createkinetism.content.recipe.InfusingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Mechanical Metallurgic Infuser recipes, laid out like Create's Spout panel.
 *
 * <p>The machine is a spout, so the panel is the spout's: the item that rides in underneath on the
 * left, the infusion above it because that is where the tank is, and the result on the right.</p>
 *
 * <p>The infusion slot holds a Mekanism {@code ChemicalStack} rather than a fluid, so it is
 * registered under Mekanism's own JEI ingredient type. That type only exists because Mekanism ships
 * a JEI plugin of its own; this class is only ever loaded when JEI is present, so reaching for it is
 * safe.</p>
 */
@ParametersAreNonnullByDefault
public class InfusingCategory extends CreateRecipeCategory<InfusingRecipe> {

	private final AnimatedInfuser infuser = new AnimatedInfuser();

	public InfusingCategory(Info<InfusingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, InfusingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 27, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.itemInput());

		// Beside the machine rather than above the item: the infusion goes into the tank, not onto the
		// belt, and putting it level with the tank says which of the two it is.
		//
		// With its own renderer rather than the one Mekanism registers globally. That one is built for
		// the ingredient list, where a stack has no meaningful amount, so its tooltip deliberately
		// omits one - which in a recipe panel reads as the recipe not telling you the cost. Handing it
		// the recipe's own amount as the capacity fills the slot and puts "80 mB" back in the tooltip.
		long amount = recipe.chemicalInput()
			.amount();
		builder.addSlot(RecipeIngredientRole.INPUT, 47, 18)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, recipe.chemicalInput()
				.getRepresentations());

		builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStack(recipe.getResultItem());
	}

	@Override
	public void draw(InfusingRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
		// A tag ingredient with nothing in it has no representation to draw; the machine still should.
		List<ChemicalStack> shown = recipe.chemicalInput()
			.getRepresentations();
		infuser.withChemical(shown.isEmpty() ? ChemicalStack.EMPTY : shown.getFirst())
			.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
	}
}
