package me.moonscenty.createkinetism.content.recipe;

import java.util.ArrayList;
import java.util.List;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.MekanismRecipeType;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Mekanism's own infuser recipes, borrowed for ours.
 *
 * <p>The Mechanical Metallurgic Infuser is the same machine as Mekanism's Metallurgic Infuser with a
 * shaft bolted to it, so writing our own copy of its thirty-seven recipes would be writing the same
 * data twice and letting the two drift. Instead {@code mekanism:metallurgic_infusing} and
 * {@code mekanism:chemical_conversion} are read straight off the recipe manager and wrapped in our
 * own recipe classes - control circuits, alloys, bronze, steel, netherite dust, the mossy blocks,
 * all of it - and anything a pack adds to Mekanism turns up here too.</p>
 *
 * <p>Recipes written against our own types still load and are listed first, so a pack can still say
 * something this machine does that Mekanism's does not.</p>
 *
 * <p>Rebuilt when the recipe manager is swapped out, which is what a datapack reload does. Comparing
 * the manager by identity is enough: a reload always produces a new one.</p>
 */
public class MekanismRecipes {

	private static RecipeManager builtFrom;
	private static List<RecipeHolder<InfusingRecipe>> infusing = List.of();
	private static List<RecipeHolder<ConvertingRecipe>> converting = List.of();

	public static List<RecipeHolder<InfusingRecipe>> infusing(Level level) {
		rebuildIfStale(level);
		return infusing;
	}

	public static List<RecipeHolder<ConvertingRecipe>> converting(Level level) {
		rebuildIfStale(level);
		return converting;
	}

	private static synchronized void rebuildIfStale(Level level) {
		RecipeManager manager = level.getRecipeManager();
		if (manager == builtFrom)
			return;
		builtFrom = manager;
		infusing = buildInfusing(level, manager);
		converting = buildConverting(level, manager);
	}

	private static List<RecipeHolder<InfusingRecipe>> buildInfusing(Level level, RecipeManager manager) {
		List<RecipeHolder<InfusingRecipe>> all = new ArrayList<>(
			manager.getAllRecipesFor(CKRecipeTypes.INFUSING.<SingleRecipeInput, InfusingRecipe>getType()));

		for (RecipeHolder<ItemStackChemicalToItemStackRecipe> holder : MekanismRecipeType.METALLURGIC_INFUSING
			.getRecipes(level)) {
			ItemStackChemicalToItemStackRecipe recipe = holder.value();
			if (recipe.isIncomplete())
				continue;

			// Our machine takes one item off the belt per pass, so a recipe wanting several of the same
			// item cannot be honoured - running it anyway would hand the extras out free.
			Ingredient item = single(recipe.getItemInput());
			if (item == null)
				continue;

			List<ItemStack> outputs = recipe.getOutputDefinition();
			if (outputs.isEmpty())
				continue;

			all.add(new RecipeHolder<>(holder.id(), new InfusingRecipe(item, recipe.getChemicalInput(),
				outputs.getFirst()
					.copy(),
				InfusingRecipe.DEFAULT_PROCESSING_TIME)));
		}
		return List.copyOf(all);
	}

	private static List<RecipeHolder<ConvertingRecipe>> buildConverting(Level level, RecipeManager manager) {
		List<RecipeHolder<ConvertingRecipe>> all = new ArrayList<>(
			manager.getAllRecipesFor(CKRecipeTypes.CONVERTING.<SingleRecipeInput, ConvertingRecipe>getType()));

		// Mekanism converts solids into every chemical it has, not just the eight infuse types - flint
		// into oxygen, sulfur dust into sulfuric acid. Those are for its other machines. Taking them
		// here would let one dropped flint fill the tank with something no infusion recipe can spend,
		// and the tank holds one chemical at a time, so the machine would sit jammed.
		List<ChemicalStackIngredient> wanted = infusing.stream()
			.map(holder -> holder.value()
				.chemicalInput())
			.toList();

		for (RecipeHolder<ItemStackToChemicalRecipe> holder : MekanismRecipeType.CHEMICAL_CONVERSION
			.getRecipes(level)) {
			ItemStackToChemicalRecipe recipe = holder.value();
			if (recipe.isIncomplete())
				continue;

			Ingredient item = single(recipe.getInput());
			if (item == null)
				continue;

			List<ChemicalStack> outputs = recipe.getOutputDefinition();
			if (outputs.isEmpty())
				continue;
			ChemicalStack output = outputs.getFirst();
			if (wanted.stream()
				.noneMatch(ingredient -> ingredient.testType(output)))
				continue;

			all.add(new RecipeHolder<>(holder.id(), new ConvertingRecipe(item, output.copy())));
		}
		return List.copyOf(all);
	}

	/** The plain ingredient behind a Mekanism one, or null if it asks for more than a single item. */
	private static Ingredient single(ItemStackIngredient ingredient) {
		return ingredient.ingredient()
			.count() == 1 ? ingredient.ingredient()
				.ingredient() : null;
	}
}
