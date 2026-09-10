package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mekanism.api.chemical.ChemicalStack;
import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedRadioactiveWasteDrum;
import me.moonscenty.createkinetism.content.recipe.DecayingRecipe;
import me.moonscenty.createkinetism.content.waste.RadioactiveWasteDrumBlockEntity;
import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.gui.CKGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * What the Radioactive Waste Drum rots, and how fast.
 *
 * <p>Most of these recipes have no result at all, which is the whole point of the machine - so the
 * panel puts the rate where an output slot would be. "1 mB/s" is the answer a player actually wants
 * from this screen: whether a drum can keep up with what is filling it. It is the same number on
 * every recipe, because the rate belongs to the drum rather than to what is in it - see
 * {@link DecayingRecipe}.</p>
 */
@ParametersAreNonnullByDefault
public class DecayingCategory extends CreateRecipeCategory<DecayingRecipe> {

	private final AnimatedRadioactiveWasteDrum drum = new AnimatedRadioactiveWasteDrum();

	public DecayingCategory(Info<DecayingRecipe> info) {
		super(info);
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, DecayingRecipe recipe, IFocusGroup focuses) {
		chemicalSlot(builder, RecipeIngredientRole.INPUT, 15, 26, recipe.input()
			.getRepresentations(),
			recipe.getInputUnit());

		recipe.output()
			.ifPresent(made -> chemicalSlot(builder, RecipeIngredientRole.OUTPUT, 139, 26,
				List.of(made), made.getAmount()));
	}

	/**
	 * A slot holding a Mekanism chemical, with its own renderer rather than the one Mekanism
	 * registers globally: that one is built for the ingredient list and leaves the amount out of the
	 * tooltip, which in a recipe panel reads as the recipe not telling you the cost.
	 */
	private void chemicalSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y,
		List<ChemicalStack> stacks, long amount) {
		builder.addSlot(role, x, y)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(amount, 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, stacks);
	}

	@Override
	protected void draw(DecayingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {

		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 47);
		drum.draw(graphics, getBackground().getWidth() / 2 - 13, 40);

		graphics.drawString(Minecraft.getInstance().font,
			CKLang.translate("jei.decaying.rate", RadioactiveWasteDrumBlockEntity.DECAY_PER_SECOND)
				.component(),
			48, 12, 0xFF3A3A3A, false);

		// Only when there is something to point at - most of these rot into nothing.
		if (recipe.output()
			.isPresent())
			CKGuiTextures.SHORT_RIGHT_ARROW.render(graphics, 96, 31);
	}
}
