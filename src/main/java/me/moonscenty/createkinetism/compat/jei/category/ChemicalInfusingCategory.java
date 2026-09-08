package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedChemistryInfuser;
import me.moonscenty.createkinetism.content.recipe.ChemicalInfusingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Chemical Infusing recipes: two gases in, one out.
 *
 * <p>The two inputs sit side by side where the machine's own two tanks are, and the result sits
 * where its middle tank is. Nothing item-shaped appears anywhere in this recipe, so the panel is
 * three chemical slots and nothing else.</p>
 */
@ParametersAreNonnullByDefault
public class ChemicalInfusingCategory extends CreateRecipeCategory<ChemicalInfusingRecipe> {

	private final AnimatedChemistryInfuser infuser = new AnimatedChemistryInfuser();

	public ChemicalInfusingCategory(Info<ChemicalInfusingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ChemicalInfusingRecipe recipe, IFocusGroup focuses) {
		chemicalSlot(builder, RecipeIngredientRole.INPUT, 27, 51, recipe.leftInput()
			.getRepresentations(),
			recipe.leftInput()
				.amount());
		chemicalSlot(builder, RecipeIngredientRole.INPUT, 47, 18, recipe.rightInput()
			.getRepresentations(),
			recipe.rightInput()
				.amount());
		chemicalSlot(builder, RecipeIngredientRole.OUTPUT, 132, 51, List.of(recipe.output()),
			recipe.output()
				.getAmount());
	}

	/**
	 * A slot holding a Mekanism chemical, with its own renderer rather than the one Mekanism
	 * registers globally: that one is built for the ingredient list and leaves the amount out of the
	 * tooltip, which in a recipe panel reads as the recipe not telling you the cost.
	 */
	private void chemicalSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y,
		List<mekanism.api.chemical.ChemicalStack> stacks, long amount) {
		builder.addSlot(role, x, y)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, stacks);
	}

	@Override
	public void draw(ChemicalInfusingRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
		// Twenty pixels lower than the panels that hang a machine over a basin: this one stands on its
		// own, so it wants to sit on the shadow rather than float above where a basin would be.
		infuser.draw(graphics, getBackground().getWidth() / 2 - 13, 42);
	}
}
