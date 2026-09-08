package me.moonscenty.createkinetism.compat.jei.category;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mekanism.api.chemical.ChemicalStack;
import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedInjectionChamber;
import me.moonscenty.createkinetism.content.recipe.InjectingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Injection Chamber recipes, laid out exactly like the Combiner's panel.
 *
 * <p>Both machines are vats that also hold something of their own - the Combiner an infusion item,
 * this one a gas tank - so the panel poses the same question the same way: what goes in the basin
 * below on the left, what the machine itself holds above, what comes out on the right.</p>
 */
@ParametersAreNonnullByDefault
public class InjectingCategory extends CreateRecipeCategory<InjectingRecipe> {

	private final AnimatedInjectionChamber chamber = new AnimatedInjectionChamber();

	public InjectingCategory(Info<InjectingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, InjectingRecipe recipe, IFocusGroup focuses) {
		// Create says "three raw copper" by listing the ingredient three times, so the slot has to put
		// that count back on the stack or the panel would quietly ask for one.
		List<Ingredient> items = recipe.getIngredients();
		int count = items.size();
		builder.addSlot(RecipeIngredientRole.INPUT, 27, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStacks(Arrays.stream(items.get(0)
				.getItems())
				.map(stack -> stack.copyWithCount(count))
				.toList());

		// Above the machine, where the Combiner puts what it holds. The chemical lives in the
		// chamber's own tank rather than an item slot, but the position says the same thing: this is
		// what the machine itself carries, not what sits in the basin.
		//
		// Its own renderer, not the one Mekanism registers globally - that one is built for the
		// ingredient list and leaves the amount out of the tooltip, which in a recipe panel reads as
		// the recipe not telling you the cost.
		long amount = recipe.getRequiredChemical()
			.amount();
		builder.addSlot(RecipeIngredientRole.INPUT, 51, 5)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, recipe.getRequiredChemical()
				.getRepresentations());

		builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStack(recipe.getResultItem());
	}

	@Override
	public void draw(InjectingRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
		// A tag ingredient with nothing in it has no representation to draw; the machine still should.
		List<ChemicalStack> shown = recipe.getRequiredChemical()
			.getRepresentations();
		chamber.withChemical(shown.isEmpty() ? ChemicalStack.EMPTY : shown.getFirst())
			.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
	}
}
