package me.moonscenty.createkinetism.compat.jei.category;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedInjectionChamber;
import me.moonscenty.createkinetism.content.recipe.InjectingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

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
		builder.addSlot(RecipeIngredientRole.INPUT, 27, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.getIngredients()
				.get(0));

		// Above the machine, where the Combiner puts what it holds. The gas lives in the chamber's own
		// tank rather than an item slot, but the position says the same thing: this is what the
		// machine itself carries, not what sits in the basin.
		addFluidSlot(builder, 51, 5, recipe.getRequiredFluid());

		builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStack(recipe.getResultItem());
	}

	@Override
	public void draw(InjectingRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
		chamber.withFluids(Arrays.asList(recipe.getRequiredFluid()
			.getFluids()))
			.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
	}
}
