package me.moonscenty.createkinetism.content.nutrition;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.item.SmartInventory;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.foundation.MekanismFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Mekanism's Nutritional Liquifier, as a mixer over a basin.
 *
 * <p>The Liquifier has no recipe files. It reads how filling a food claims to be and gives back that
 * much Nutritional Paste - {@code nutrition x 50} mB - plus whatever the food leaves behind once
 * eaten, a bowl from a stew or a bottle from honey. This machine does exactly the same, so every food
 * in the game works without a file per item, other mods' included, and the amounts match what
 * Mekanism's own machine would give.</p>
 *
 * <p>The paste goes into the basin's fluid output, where a pipe or a Mekanism Canteen takes it; the
 * leftover container goes into the basin's item output.</p>
 *
 * <p>A {@code nutritional_liquifying} recipe still wins if one matches, so a pack can name an
 * exception without touching this class.</p>
 */
public class NutritionalLiquidMixerBlockEntity extends VatBlockEntity {

	/**
	 * Millibuckets of paste per point of nutrition. Mekanism's Liquifier hard-codes the same 50 when it
	 * builds its recipe, so bread (5) gives 250 mB here as it does there.
	 */
	public static final int PASTE_PER_NUTRITION = 50;

	private static final int DURATION = 100;

	public NutritionalLiquidMixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Written recipes first, then the food rule.
	 *
	 * <p>Both go through {@link #matchBasinRecipe}, so the synthesised one is held to the same
	 * standard as a real one - the basin's filter, its output space and its heat all still apply. A
	 * food whose paste would not fit the basin's output tank simply waits.</p>
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
	 * <p>Nutrition zero is skipped, as the Liquifier skips it: an item that fills nothing gives no
	 * paste.</p>
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
			return liquify(BasinRecipe::new, CreateKinetism.asResource("nutritional_liquifying/food"), stack, food);
		}
		return null;
	}

	/**
	 * The recipe one food makes: that food in, {@code nutrition x 50} mB of paste out, and the eaten
	 * container back if it leaves one.
	 *
	 * <p>Shared with the JEI list, so what the panel shows and what the machine does cannot disagree.</p>
	 */
	public static <R extends StandardProcessingRecipe<?>> R liquify(StandardProcessingRecipe.Factory<R> factory,
		ResourceLocation id, ItemStack food, FoodProperties properties) {
		StandardProcessingRecipe.Builder<R> builder = new StandardProcessingRecipe.Builder<>(factory, id)
			.withItemIngredients(Ingredient.of(food.getItem()))
			.withFluidOutputs(new FluidStack(MekanismFluids.NUTRITIONAL_PASTE.get(),
				properties.nutrition() * PASTE_PER_NUTRITION))
			.duration(DURATION);
		ItemStack leftover = properties.usingConvertsTo()
			.orElse(ItemStack.EMPTY);
		if (!leftover.isEmpty())
			builder.withItemOutputs(new ProcessingOutput(leftover.copy(), 1));
		return builder.build();
	}
}
