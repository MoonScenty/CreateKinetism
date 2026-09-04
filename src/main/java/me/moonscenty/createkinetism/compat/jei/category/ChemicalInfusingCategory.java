package me.moonscenty.createkinetism.compat.jei.category;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedChemistryInfuser;
import me.moonscenty.createkinetism.content.recipe.ChemicalInfusingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Chemical Infusing recipes, laid out exactly like {@link InfusingCategory}'s Spout panel: the
 * Metallurgic Infuser's layout carried over as-is, with the depot swapped for a basin and the item
 * that used to ride on the depot swapped for the fluid now sitting in that basin instead - since both
 * of this recipe's ingredients are fluids rather than an item and a fluid.
 */
@ParametersAreNonnullByDefault
public class ChemicalInfusingCategory extends CreateRecipeCategory<ChemicalInfusingRecipe> {

	private final AnimatedChemistryInfuser infuser = new AnimatedChemistryInfuser();

	public ChemicalInfusingCategory(Info<ChemicalInfusingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ChemicalInfusingRecipe recipe, IFocusGroup focuses) {
		List<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();

		// Where the item used to sit on the depot: now the fluid already waiting in the basin.
		addFluidSlot(builder, 27, 51, fluidIngredients.get(1));

		// Beside the machine, level with the tank: the fluid this infuser pours in.
		addFluidSlot(builder, 47, 18, fluidIngredients.get(0));

		int i = 0;
		int size = recipe.getRollableResults()
			.size()
			+ recipe.getFluidResults()
				.size();

		for (ProcessingOutput result : recipe.getRollableResults()) {
			int xPosition = 142 - (size % 2 != 0 && i == size - 1 ? 0 : i % 2 == 0 ? 10 : -9);
			int yPosition = -19 * (i / 2) + 51;
			builder.addSlot(RecipeIngredientRole.OUTPUT, xPosition, yPosition)
				.setBackground(getRenderedSlot(result), -1, -1)
				.addItemStack(result.getStack())
				.addRichTooltipCallback(addStochasticTooltip(result));
			i++;
		}
		for (FluidStack fluidResult : recipe.getFluidResults()) {
			int xPosition = 142 - (size % 2 != 0 && i == size - 1 ? 0 : i % 2 == 0 ? 10 : -9);
			int yPosition = -19 * (i / 2) + 51;
			addFluidSlot(builder, xPosition, yPosition, fluidResult);
			i++;
		}
	}

	@Override
	public void draw(ChemicalInfusingRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
		infuser.withFluids(Arrays.asList(recipe.getFluidIngredients()
			.get(0)
			.getFluids()))
			.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
	}
}
