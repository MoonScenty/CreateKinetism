package me.moonscenty.createkinetism.content.recipe;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

/**
 * Base class for the item-in / item-out machines. Mirrors Create's
 * {@code AbstractCrushingRecipe} (millstone / crushing wheels), with one addition: an ingredient may
 * be listed several times to demand several items.
 *
 * <p>That is what makes the Mekanism machines with a real input cost work. Create's processing
 * recipes carry plain {@code Ingredient}s with no count, so "8 cobblestone plus one dust" is spelled
 * as the cobblestone ingredient repeated eight times, and {@link #resolve} folds those repeats back
 * into a per-slot consumption count.</p>
 */
@ParametersAreNonnullByDefault
public abstract class ChamberRecipe extends StandardProcessingRecipe<RecipeInput> {

	protected ChamberRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeParams params) {
		super(typeInfo, params);
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	public boolean matches(RecipeInput inv, Level level) {
		return resolve(inv) != null;
	}

	/**
	 * Work out how many items this recipe would take out of each slot.
	 *
	 * @return one entry per slot, or null when the inventory cannot satisfy the recipe
	 */
	@Nullable
	public int[] resolve(RecipeInput inv) {
		if (inv.isEmpty())
			return null;

		int[] consumed = new int[inv.size()];

		ingredients:
		for (Ingredient ingredient : ingredients) {
			for (int slot = 0; slot < inv.size(); slot++) {
				ItemStack stack = inv.getItem(slot);
				if (stack.isEmpty())
					continue;
				// this slot has already been promised to as many items as it holds
				if (consumed[slot] >= stack.getCount())
					continue;
				if (!ingredient.test(stack))
					continue;
				consumed[slot]++;
				continue ingredients;
			}
			return null;
		}

		return consumed;
	}
}
