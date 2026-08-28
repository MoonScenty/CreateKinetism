package me.moonscenty.createkinetism.compat.jei.category;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import me.moonscenty.createkinetism.content.recipe.EngineFuelRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * What one engine gets out of one fuel.
 *
 * <p>Shared by all three engines - they differ only in which recipe type they read, so the same
 * category is registered once per engine with its own block as the catalyst. That keeps a fuel's
 * figures for the turbine from being confused with the same fluid's figures in another engine.</p>
 *
 * <p>There is no output slot. A fuel recipe produces rotation, not an item, so the three numbers on
 * the right are the whole result: how fast it burns, how much stress it supports, and how quickly it
 * turns. This is the only place a player can see that steam and LPG are not interchangeable.</p>
 */
public class EngineFuelCategory extends CreateRecipeCategory<EngineFuelRecipe> {

	public EngineFuelCategory(Info<EngineFuelRecipe> info) {
		super(info);
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, EngineFuelRecipe recipe, IFocusGroup focuses) {
		if (!recipe.getFluidIngredients()
			.isEmpty())
			addFluidSlot(builder, 15, 26, recipe.getFluidIngredients()
				.get(0));
	}

	@Override
	protected void draw(EngineFuelRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		var font = Minecraft.getInstance().font;

		graphics.drawString(font, CKLang.translate("jei.engine_fuel.consumption", recipe.getConsumptionRate())
			.component(), 48, 16, 0xFF5A5A5A, false);
		graphics.drawString(font, CKLang.translate("jei.engine_fuel.stress", (int) recipe.getStress())
			.component(), 48, 30, 0xFF3A3A3A, false);
		graphics.drawString(font, CKLang.translate("jei.engine_fuel.rpm", recipe.getRpm())
			.component(), 48, 44, 0xFF3A3A3A, false);
	}
}
