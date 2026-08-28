package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.moonscenty.createkinetism.content.recipe.DistillingRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One column run: crude in on the left, every cut it splits into on the right.
 *
 * <p>The mode is the important line here, because it is what the whole oil chain progresses along -
 * the same crude gives more, and heavier, cuts as you move from flash to atmospheric to vacuum. It
 * is drawn as text above the arrow rather than as a slot, since it is a property of the column
 * rather than an ingredient.</p>
 */
public class DistillingCategory extends CreateRecipeCategory<DistillingRecipe> {

	public DistillingCategory(Info<DistillingRecipe> info) {
		super(info);
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, DistillingRecipe recipe, IFocusGroup focuses) {
		if (!recipe.getFluidIngredients()
			.isEmpty())
			addFluidSlot(builder, 15, 39, recipe.getFluidIngredients()
				.get(0));

		// Cuts stack downwards in two columns, the way the tower's stages read bottom to top.
		List<FluidStack> results = recipe.getFluidResults();
		for (int i = 0; i < results.size(); i++) {
			int x = 121 + (i % 2) * 19;
			int y = 20 + (i / 2) * 19;
			addFluidSlot(builder, x, y, results.get(i));
		}
	}

	@Override
	protected void draw(DistillingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		AllGuiTextures.JEI_LONG_ARROW.render(graphics, 51, 44);

		var font = Minecraft.getInstance().font;

		graphics.drawString(font, CKLang.translate(recipe.getMode()
			.getRawTranslationKey())
			.component(), 51, 28, 0xFF3A3A3A, false);

		int duration = recipe.getProcessingDuration();
		if (duration > 0)
			graphics.drawString(font, CKLang.translate("jei.distilling.time", duration / 20f)
				.component(), 51, 62, 0xFF5A5A5A, false);
	}
}
