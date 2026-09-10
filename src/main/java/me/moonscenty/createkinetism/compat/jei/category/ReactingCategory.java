package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedPressurizedReactionChamber;
import me.moonscenty.createkinetism.content.recipe.ReactingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Pressurized Reaction Chamber recipes.
 *
 * <p>A basin panel with one extra slot. The item and the gas sit in the basin like any other vat's
 * inputs, and both products come back to it, so {@link BasinRecipeCategory} lays all four of those
 * out. What it cannot know about is the reaction fluid, which lives in the chamber's own tank - that
 * goes above the machine, where the Combiner and the Injection Chamber put what they hold, because
 * it is the same statement: this is the machine's, not the basin's.</p>
 */
@ParametersAreNonnullByDefault
public class ReactingCategory extends BasinRecipeCategory<ReactingRecipe> {

	private final AnimatedPressurizedReactionChamber chamber = new AnimatedPressurizedReactionChamber();

	public ReactingCategory(Info<ReactingRecipe> info) {
		super(info);
	}

	/** The chambers' 34, nudged down - this machine stands on its basin instead of over it. */
	@Override
	protected int machineAnchor() {
		return 37;
	}

	@Override
	protected int shadowAnchor() {
		return 58;
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, ReactingRecipe recipe, IFocusGroup focuses) {
		super.setRecipe(builder, recipe, focuses);
		// The chamber's own gas, in the slot the reactant fluid used to sit in.
		builder.addSlot(RecipeIngredientRole.INPUT, 67, 5)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL,
				new ChemicalStackRenderer(recipe.getRequiredAmount(), 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, recipe.getChemicalInput()
				.getRepresentations());

		recipe.getChemicalOutput()
			.ifPresent(made -> builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 5)
				.setBackground(getRenderedSlot(), -1, -1)
				.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL,
					new ChemicalStackRenderer(made.getAmount(), 16, 16))
				.addIngredient(MekanismJEI.TYPE_CHEMICAL, made));
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		chamber.draw(graphics, centerX, anchorY);
	}
}
