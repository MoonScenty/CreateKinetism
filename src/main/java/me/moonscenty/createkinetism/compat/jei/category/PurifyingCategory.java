package me.moonscenty.createkinetism.compat.jei.category;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.mutable.MutableInt;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedPurificationVibrator;
import me.moonscenty.createkinetism.content.recipe.PurifyingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.createmod.catnip.data.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Purification Vibrator recipes, laid out the way Create lays out its own basin recipes.
 *
 * <p>{@code PurifyingRecipe} takes up to two items and two fluids and gives back up to four items
 * and two fluids - a real basin's shape, which is exactly what this machine still is on the inside
 * even though the basin itself now rides on top of it rather than sitting underneath. Create's own
 * {@code BasinCategory} is the right template; the heat-condition half of it is left out; nothing
 * this mod runs through a basin ever asks for heat - see {@code VatRecipe.canRequireHeat}.</p>
 */
@ParametersAreNonnullByDefault
public class PurifyingCategory extends CreateRecipeCategory<PurifyingRecipe> {

	private final AnimatedPurificationVibrator vibrator = new AnimatedPurificationVibrator();

	public PurifyingCategory(Info<PurifyingRecipe> info) {
		super(info);
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, PurifyingRecipe recipe, IFocusGroup focuses) {
		List<Pair<Ingredient, MutableInt>> condensedIngredients =
			ItemHelper.condenseIngredients(recipe.getIngredients());

		int size = condensedIngredients.size() + recipe.getFluidIngredients()
			.size();
		int xOffset = size < 3 ? (3 - size) * 19 / 2 : 0;
		int i = 0;

		for (Pair<Ingredient, MutableInt> pair : condensedIngredients) {
			List<ItemStack> stacks = new ArrayList<>();
			for (ItemStack itemStack : pair.getFirst()
				.getItems()) {
				ItemStack copy = itemStack.copy();
				copy.setCount(pair.getSecond()
					.getValue());
				stacks.add(copy);
			}

			builder.addSlot(RecipeIngredientRole.INPUT, 17 + xOffset + (i % 3) * 19, 51 - (i / 3) * 19)
				.setBackground(getRenderedSlot(), -1, -1)
				.addItemStacks(stacks);
			i++;
		}
		for (SizedFluidIngredient fluidIngredient : recipe.getFluidIngredients()) {
			addFluidSlot(builder, 17 + xOffset + (i % 3) * 19, 51 - (i / 3) * 19, fluidIngredient);
			i++;
		}

		size = recipe.getRollableResults()
			.size()
			+ recipe.getFluidResults()
				.size();
		i = 0;

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
	protected void draw(PurifyingRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX,
		double mouseY) {
		int vRows = (1 + recipe.getFluidResults()
			.size()
			+ recipe.getRollableResults()
				.size()) / 2;
		if (vRows <= 2)
			AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * (vRows - 1) + 32);

		AllGuiTextures.JEI_SHADOW.render(graphics, 81, 68);

		// Create's own Mixing panel anchors its machine at y=34 in this same 103-tall background,
		// leaving 103-34=69px below for a basin that hangs underneath it. Ours hangs its basin above
		// instead, so the anchor is mirrored to the same 69, trading that clearance to the other side.
		vibrator.draw(graphics, getBackground().getWidth() / 2 + 3, 69);
	}
}
