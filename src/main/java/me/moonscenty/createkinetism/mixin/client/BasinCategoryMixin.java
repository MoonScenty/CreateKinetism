package me.moonscenty.createkinetism.mixin.client;

import com.simibubi.create.compat.jei.category.BasinCategory;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;

import me.moonscenty.createkinetism.content.heat.CKHeatLevels;
import me.moonscenty.createkinetism.registry.CKBlocks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.item.ItemStack;

/**
 * The heater slots under a basin recipe in JEI, for the two new heat levels.
 *
 * <p>The heat section is the last thing Create's {@code setRecipe} does, and it decides from
 * {@code testBlazeBurner} whether to add a Blaze Burner and a Blaze Cake. For Sodium Heated or Chilled
 * that would be the wrong block and a fuel that does nothing, so the method stops there and adds the
 * block that actually gives that heat, in the Blaze Burner's slot. No fuel slot: neither block burns
 * an item a recipe could point at.</p>
 */
@Mixin(value = BasinCategory.class, remap = false)
public abstract class BasinCategoryMixin {

	@Inject(
		method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lcom/simibubi/create/content/processing/basin/BasinRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",
		at = @At(value = "INVOKE",
			target = "Lcom/simibubi/create/content/processing/basin/BasinRecipe;getRequiredHeat()Lcom/simibubi/create/content/processing/recipe/HeatCondition;"),
		cancellable = true)
	private void createkinetism$newHeaterSlots(IRecipeLayoutBuilder builder, BasinRecipe recipe, IFocusGroup focuses,
		CallbackInfo ci) {
		HeatCondition heat = recipe.getRequiredHeat();
		ItemStack heater;
		if (heat == CKHeatLevels.SODIUM_HEATED_CONDITION)
			heater = CKBlocks.SODIUM_BURNER.asStack();
		else if (heat == CKHeatLevels.CHILLED_CONDITION)
			heater = CKBlocks.STRAY_CHILLER.asStack();
		else
			return;

		builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81)
			.addItemStack(heater);
		ci.cancel();
	}
}
