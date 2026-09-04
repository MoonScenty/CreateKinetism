package me.moonscenty.createkinetism.compat.jei.category;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.mutable.MutableInt;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;

import me.moonscenty.createkinetism.content.recipe.VatRecipe;

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
 * The shared layout for every basin recipe in this mod, laid out the way Create lays out its own.
 *
 * <p>A {@code VatRecipe} takes up to two items and two fluids and gives back up to four items and
 * two fluids - a real basin's shape - so the slot arithmetic is the same whichever machine runs it.
 * Create's own {@code BasinCategory} is the template; its heat-condition half is left out because
 * nothing this mod runs through a basin asks for heat - see {@code VatRecipe.canRequireHeat}.</p>
 *
 * <p>Subclasses supply one thing: which machine to draw under the slots.</p>
 */
@ParametersAreNonnullByDefault
public abstract class BasinRecipeCategory<T extends VatRecipe> extends CreateRecipeCategory<T> {

	protected BasinRecipeCategory(Info<T> info) {
		super(info);
	}

	/**
	 * Draws the machine itself. {@code anchorY} is where Create's own Mixing panel would put a
	 * machine that hangs its basin underneath; a machine that carries its basin above wants the
	 * mirrored value instead.
	 */
	protected abstract void drawMachine(GuiGraphics graphics, int centerX, int anchorY);

	/** How far down the panel the machine sits. Basin-below machines want less room under them. */
	protected int machineAnchor() {
		return 69;
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
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
	protected void draw(T recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		int vRows = (1 + recipe.getFluidResults()
			.size()
			+ recipe.getRollableResults()
				.size()) / 2;
		if (vRows <= 2)
			AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * (vRows - 1) + 32);

		AllGuiTextures.JEI_SHADOW.render(graphics, 81, 68);

		drawMachine(graphics, getBackground().getWidth() / 2 + 3, machineAnchor());
	}
}
