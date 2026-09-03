package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedEnricher;
import me.moonscenty.createkinetism.content.recipe.ChamberRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Ore into dust under the Mechanical Enricher.
 *
 * <p>Laid out as Create lays out its own pressing recipes, because the enricher <em>is</em> a press -
 * the item sits on a depot below and the head comes down on it. The animation is ours rather than
 * Create's, so the panel shows the machine the recipe actually needs.</p>
 */
public class EnrichingCategory extends CreateRecipeCategory<ChamberRecipe> {

	private final AnimatedEnricher enricher = new AnimatedEnricher();

	public EnrichingCategory(Info<ChamberRecipe> info) {
		super(info);
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, ChamberRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 27, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.getIngredients()
				.get(0));

		List<ProcessingOutput> results = recipe.getRollableResults();
		int i = 0;
		for (ProcessingOutput output : results) {
			builder.addSlot(RecipeIngredientRole.OUTPUT, 131 + 19 * i, 50)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
			i++;
		}
	}

	@Override
	protected void draw(ChamberRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 61, 41);
		AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 54);
		enricher.draw(graphics, getBackground().getWidth() / 2 - 17, 22);
	}
}
