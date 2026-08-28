package me.moonscenty.createkinetism.compat.jei.category;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.moonscenty.createkinetism.content.recipe.PumpjackRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Which fluid a well brings up in a given biome, and how much per stroke.
 *
 * <p>There is no input item or fluid to show - the well makes its output out of the ground - so the
 * biome takes the place of the input, written out as text because a biome is not an ingredient JEI
 * can render.</p>
 */
public class PumpjackCategory extends CreateRecipeCategory<PumpjackRecipe> {

	public PumpjackCategory(Info<PumpjackRecipe> info) {
		super(info);
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, PumpjackRecipe recipe, IFocusGroup focuses) {
		addFluidSlot(builder, 133, 27, recipe.getFluidResult());
	}

	@Override
	protected void draw(PumpjackRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		AllGuiTextures.JEI_ARROW.render(graphics, 90, 32);

		var font = Minecraft.getInstance().font;

		// The biome id can be long enough to run past the panel, so it wraps onto its own line under
		// the label rather than sitting beside it.
		graphics.drawString(font, CKLang.translate("jei.pumpjack.biome")
			.component(), 8, 20, 0xFF5A5A5A, false);
		graphics.drawString(font, recipe.getBiomeDescription(), 8, 32, 0xFF3A3A3A, false);

		graphics.drawString(font, CKLang.translate("jei.pumpjack.yield", recipe.getFluidResult()
			.getAmount())
			.style(ChatFormatting.DARK_GRAY)
			.component(), 8, 48, 0xFF5A5A5A, false);
	}
}
