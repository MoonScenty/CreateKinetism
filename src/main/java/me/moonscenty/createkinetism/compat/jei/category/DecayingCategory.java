package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedRadioactiveWasteDrum;
import me.moonscenty.createkinetism.content.recipe.DecayingRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * What the Radioactive Waste Drum rots, and how fast.
 *
 * <p>Most of these recipes have no result at all, which is the whole point of the machine - so the
 * panel puts the rate where an output slot would be. "2 mB/s" is the answer a player actually wants
 * from this screen: whether a drum can keep up with what is filling it.</p>
 */
@ParametersAreNonnullByDefault
public class DecayingCategory extends CreateRecipeCategory<DecayingRecipe> {

	private final AnimatedRadioactiveWasteDrum drum = new AnimatedRadioactiveWasteDrum();

	public DecayingCategory(Info<DecayingRecipe> info) {
		super(info);
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, DecayingRecipe recipe, IFocusGroup focuses) {
		addFluidSlot(builder, 15, 26, recipe.getFluidIngredients()
			.getFirst());

		if (!recipe.getFluidResults()
			.isEmpty())
			addFluidSlot(builder, 139, 26, recipe.getFluidResults()
				.getFirst());
	}

	@Override
	protected void draw(DecayingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {

		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 47);
		drum.draw(graphics, getBackground().getWidth() / 2 - 13, 40);

		SizedFluidIngredient ingredient = recipe.getFluidIngredients()
			.getFirst();
		float perSecond = ingredient.amount() * 20f / Math.max(1, recipe.getProcessingDuration());
		String rate = perSecond == Math.rint(perSecond) ? String.valueOf((int) perSecond)
			: String.format("%.1f", perSecond);

		graphics.drawString(Minecraft.getInstance().font, CKLang.translate("jei.decaying.rate", rate)
			.component(), 48, 12, 0xFF3A3A3A, false);

		// Only when there is something to point at - most of these rot into nothing.
		if (!recipe.getFluidResults()
			.isEmpty())
			AllGuiTextures.JEI_ARROW.render(graphics, 92, 30);
	}
}
