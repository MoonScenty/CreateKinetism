package me.moonscenty.createkinetism.compat.jei.category;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedCombiner;
import me.moonscenty.createkinetism.content.recipe.CombinerRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.createmod.catnip.data.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import org.apache.commons.lang3.mutable.MutableInt;

/**
 * A combining recipe, laid out exactly like Create's Deploying.
 *
 * <p>The two machines pose a player the same question - what goes underneath, and what does the
 * machine hold - so they answer it the same way: the material on the left, the held item above the
 * machine, the result on the right. Nothing here is captioned, because the positions already say
 * it.</p>
 */
public class CombiningCategory extends CreateRecipeCategory<CombinerRecipe> {

	private final AnimatedCombiner combiner = new AnimatedCombiner();

	public CombiningCategory(Info<CombinerRecipe> info) {
		super(info);
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, CombinerRecipe recipe, IFocusGroup focuses) {
		// Eight cobblestone are eight repeats of one ingredient; condensing turns that into one slot
		// with a count on it rather than eight slots.
		List<Pair<Ingredient, MutableInt>> condensed = ItemHelper.condenseIngredients(recipe.getIngredients());
		int i = 0;
		for (Pair<Ingredient, MutableInt> pair : condensed) {
			List<ItemStack> stacks = new ArrayList<>();
			for (ItemStack stack : pair.getFirst()
				.getItems()) {
				ItemStack copy = stack.copy();
				copy.setCount(pair.getSecond()
					.getValue());
				stacks.add(copy);
			}
			builder.addSlot(RecipeIngredientRole.INPUT, 27 + i * 19, 51)
				.setBackground(getRenderedSlot(), -1, -1)
				.addItemStacks(stacks);
			i++;
		}

		// Above the machine, where the deployer puts the item it is holding. Consumed, so INPUT.
		builder.addSlot(RecipeIngredientRole.INPUT, 51, 5)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.getInfusion());

		List<ProcessingOutput> results = recipe.getRollableResults();
		boolean single = results.size() == 1;
		for (int r = 0; r < results.size(); r++) {
			ProcessingOutput output = results.get(r);
			int xOffset = r % 2 == 0 ? 0 : 19;
			int yOffset = (r / 2) * -19;
			builder.addSlot(RecipeIngredientRole.OUTPUT, single ? 132 : 132 + xOffset, 51 + yOffset)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
		}
	}

	@Override
	protected void draw(CombinerRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29 + (recipe.getRollableResults()
			.size() > 2 ? -19 : 0));
		combiner.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
	}
}
