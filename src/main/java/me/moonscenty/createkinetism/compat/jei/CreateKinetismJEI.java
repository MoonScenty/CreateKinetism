package me.moonscenty.createkinetism.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory.Info;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.compat.jei.category.DistillingCategory;
import me.moonscenty.createkinetism.compat.jei.category.EngineFuelCategory;
import me.moonscenty.createkinetism.compat.jei.category.PumpjackCategory;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.ItemLike;

/**
 * Recipe viewer support for the oil chain.
 *
 * <p>Only the Petrochem-derived machines are here so far - the pumpjack, the distillation column and
 * the three engines. The chambers and vats come later.</p>
 *
 * <p>The categories extend Create's own {@code CreateRecipeCategory} so they inherit its panel, slot
 * and fluid-tooltip drawing and end up looking like the rest of Create's recipe list rather than
 * like a bolted-on addon. What they do not use is Create's {@code CreateJEI} recipe-gathering
 * helpers: those are internal to Create's plugin, so the recipes are read straight off the client's
 * recipe manager here instead.</p>
 */
@JeiPlugin
public class CreateKinetismJEI implements IModPlugin {

	private final List<CreateRecipeCategory<?>> categories = new ArrayList<>();

	@Override
	public ResourceLocation getPluginUid() {
		return CreateKinetism.asResource("jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		categories.clear();

		categories.add(category("pumpjack", CKRecipeTypes.PUMPJACK, 177, 60, CKBlocks.PUMPJACK_ARM.get(),
			PumpjackCategory::new, CKBlocks.PUMPJACK_ARM.get(), CKBlocks.PUMPJACK_WELL.get(),
			CKBlocks.PUMPJACK_CRANK.get()));

		categories.add(category("distilling", CKRecipeTypes.DISTILLING, 177, 80,
			CKBlocks.DISTILLATION_CONTROLLER.get(), DistillingCategory::new,
			CKBlocks.DISTILLATION_CONTROLLER.get(), CKBlocks.STEEL_TANK.get(),
			CKBlocks.DISTILLATION_OUTPUT.get()));

		// One category per engine: the same fluid is worth different things in different engines, so
		// they must not share a list.
		categories.add(category("gasoline_engine_fuel", CKRecipeTypes.GASOLINE_ENGINE_FUEL, 177, 60,
			CKBlocks.GASOLINE_ENGINE.get(), EngineFuelCategory::new, CKBlocks.GASOLINE_ENGINE.get()));
		categories.add(category("diesel_engine_fuel", CKRecipeTypes.DIESEL_ENGINE_FUEL, 177, 60,
			CKBlocks.DIESEL_ENGINE.get(), EngineFuelCategory::new, CKBlocks.DIESEL_ENGINE.get()));
		categories.add(category("turbine_fuel", CKRecipeTypes.TURBINE_FUEL, 177, 60,
			CKBlocks.GAS_TURBINE.get(), EngineFuelCategory::new, CKBlocks.GAS_TURBINE.get()));

		registration.addRecipeCategories(categories.toArray(IRecipeCategory[]::new));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		categories.forEach(category -> category.registerRecipes(registration));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		categories.forEach(category -> category.registerCatalysts(registration));
	}

	private static <T extends Recipe<?>> CreateRecipeCategory<T> category(String name, CKRecipeTypes recipeType,
		int width, int height, ItemLike icon, CreateRecipeCategory.Factory<T> factory, ItemLike... catalysts) {

		ResourceLocation id = CreateKinetism.asResource(name);
		List<Supplier<? extends ItemStack>> catalystStacks = new ArrayList<>();
		for (ItemLike catalyst : catalysts)
			catalystStacks.add(() -> new ItemStack(catalyst));

		return factory.create(new Info<>(RecipeType.createRecipeHolderType(id),
			Component.translatable(id.getNamespace() + ".recipe." + id.getPath()),
			new EmptyBackground(width, height), new ItemIcon(() -> new ItemStack(icon)),
			recipesOf(recipeType), catalystStacks));
	}

	/**
	 * JEI asks for recipes only once a world is loaded, but it costs nothing to be defensive: on a
	 * null level this yields an empty category rather than throwing during the plugin's own startup.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Recipe<?>> Supplier<List<RecipeHolder<T>>> recipesOf(CKRecipeTypes recipeType) {
		return () -> {
			ClientLevel level = Minecraft.getInstance().level;
			if (level == null)
				return List.of();
			return (List<RecipeHolder<T>>) (List<?>) level.getRecipeManager()
				.getAllRecipesFor(recipeType.<RecipeInput, Recipe<RecipeInput>>getType());
		};
	}
}
