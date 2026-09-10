package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mekanism.api.chemical.ChemicalStack;
import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedSolarNeutronActivator;
import me.moonscenty.createkinetism.content.recipe.ActivatingRecipe;
import me.moonscenty.createkinetism.foundation.gui.CKGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Solar Neutron Activator recipes: one gas in, one gas out.
 *
 * <p>No basin any more. A basin cannot hold a Mekanism chemical, so both halves are the machine's
 * own tanks and the panel is two chemical slots with the machine between them - there is nothing
 * item-shaped anywhere in this recipe.</p>
 */
@ParametersAreNonnullByDefault
public class ActivatingCategory extends CreateRecipeCategory<ActivatingRecipe> {

	private final AnimatedSolarNeutronActivator activator = new AnimatedSolarNeutronActivator();

	public ActivatingCategory(Info<ActivatingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ActivatingRecipe recipe, IFocusGroup focuses) {
		chemicalSlot(builder, RecipeIngredientRole.INPUT, 27, 51, recipe.input()
			.getRepresentations(),
			recipe.getRequiredAmount());
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
		List<ChemicalStack> stacks, long amount) {
		builder.addSlot(role, x, y)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, stacks);
	}

	@Override
	public void draw(ActivatingRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		// Ours, not Create's: both of Create's arrows are long enough to run under the machine.
		CKGuiTextures.SHORT_RIGHT_ARROW.render(graphics, 106, 55);
		activator.draw(graphics, getBackground().getWidth() / 2 - 13, 58);
	}
}
