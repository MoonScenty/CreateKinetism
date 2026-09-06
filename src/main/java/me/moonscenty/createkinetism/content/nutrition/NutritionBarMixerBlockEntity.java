package me.moonscenty.createkinetism.content.nutrition;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.item.SmartInventory;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.registry.CKItems;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A vat with no recipe list.
 *
 * <p>Mekanism's Nutritional Liquifier does not care what the food is - it reads how filling the item
 * claims to be and gives back that much paste. Writing that as recipes would mean one file per food
 * item in the game and another for every food any other mod adds, and it would still be wrong the
 * day someone changes a number. So the rule is read off the item instead: <b>one bar per point of
 * nutrition</b>, which is the same unit the hunger bar counts in.</p>
 *
 * <p>The conversion is deliberately lossless. What the machine sells is not more food - it is food
 * in a form that keeps, stacks, and can be spent automatically by something worn.</p>
 *
 * <p>A {@code nutrition_bar_cooking} recipe still wins if one matches, so a pack can name an exception
 * (a cake worth more than its nutrition, an inedible thing that should still yield bars) without
 * touching this class.</p>
 */
public class NutritionBarMixerBlockEntity extends VatBlockEntity {

	/** Bars per point of nutrition. Bread is 5, a steak 8. */
	private static final int BARS_PER_NUTRITION = 1;

	public NutritionBarMixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Written recipes first, then the food rule.
	 *
	 * <p>Both go through {@link #matchBasinRecipe}, so the synthesised one is held to the same
	 * standard as a real one - the basin's filter, its output space and its heat all still apply.</p>
	 */
	@Override
	protected List<Recipe<?>> getMatchingRecipes() {
		List<Recipe<?>> written = super.getMatchingRecipes();
		if (!written.isEmpty())
			return written;

		List<Recipe<?>> synthesised = new ArrayList<>();
		BasinRecipe fromFood = findFood();
		if (fromFood != null && matchBasinRecipe(fromFood))
			synthesised.add(fromFood);
		return synthesised;
	}

	/**
	 * The first thing in the basin that a player could have eaten, as a one-off recipe.
	 *
	 * <p>Nutrition zero is skipped rather than rounded up to one: an item that fills nothing is not
	 * worth a bar, and letting it through would turn every such item into a free one.</p>
	 */
	private BasinRecipe findFood() {
		BasinBlockEntity basin = getBasin().orElse(null);
		if (basin == null)
			return null;

		SmartInventory inputs = basin.getInputInventory();
		for (int slot = 0; slot < inputs.getSlots(); slot++) {
			ItemStack stack = inputs.getItem(slot);
			if (stack.isEmpty())
				continue;
			FoodProperties food = stack.getFoodProperties(null);
			if (food == null || food.nutrition() <= 0)
				continue;

			int bars = Mth.clamp(food.nutrition() * BARS_PER_NUTRITION, 1, 64);
			return new StandardProcessingRecipe.Builder<>(BasinRecipe::new,
				CreateKinetism.asResource("nutrition_bar_cooking/food"))
					.withItemIngredients(Ingredient.of(stack.getItem()))
					.withSingleItemOutput(CKItems.NUTRITION_BAR.asStack(bars))
					.duration(100)
					.build();
		}
		return null;
	}
}
