package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedPressurizedReactionChamber;
import me.moonscenty.createkinetism.content.recipe.ReactingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;

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

	@Override
	protected int machineAnchor() {
		return 34;
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, ReactingRecipe recipe, IFocusGroup focuses) {
		super.setRecipe(builder, recipe, focuses);
		addFluidSlot(builder, 83, 5, recipe.getReactant());
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		chamber.draw(graphics, centerX, anchorY);
	}
}
