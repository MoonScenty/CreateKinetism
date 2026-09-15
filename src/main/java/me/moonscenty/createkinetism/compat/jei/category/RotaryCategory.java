package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.RotaryRecipe;
import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedMechanicalCondensentrator;
import me.moonscenty.createkinetism.content.heat.CKHeatLevels;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Mekanism's rotary recipes, as the Mechanical Condensentrator runs them - one category per
 * direction, since which one runs depends on the heater rather than on the recipe:
 * condensentrating (gas to liquid) over a Stray Chiller, decondensentrating (liquid to gas) over
 * anything Heated or hotter. A Mekanism rotary recipe that goes both ways shows up in both.
 *
 * <p>Laid out like Create's own Mixing panel, because the stack is the same shape: machine, basin,
 * heater, top to bottom. Input on the left and output on the right where a basin recipe puts them,
 * the heat requirement on Create's heat bar, and the heater drawn through {@link AnimatedBlazeBurner}
 * so Chilled comes out as the chiller (see {@code AnimatedBlazeBurnerMixin}).</p>
 *
 * <p>Create puts the mixer 21px above the burner with the basin 1.65 blocks under the mixer; here the
 * basin is exactly one block under the machine, so the machine sits about a third of a block (8px)
 * above the heater's anchor instead, which leaves the heater and shadow where Create's are.</p>
 */
@ParametersAreNonnullByDefault
public class RotaryCategory extends CreateRecipeCategory<RotaryRecipe> {

	private static final int HEATER_ANCHOR = 55;
	private static final int MACHINE_ANCHOR = HEATER_ANCHOR - 8;

	private final boolean condensentrating;
	private final AnimatedMechanicalCondensentrator machine = new AnimatedMechanicalCondensentrator();
	private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();

	private RotaryCategory(Info<RotaryRecipe> info, boolean condensentrating) {
		super(info);
		this.condensentrating = condensentrating;
	}

	/** Gas to liquid, over a chiller. */
	public static RotaryCategory condensentrating(Info<RotaryRecipe> info) {
		return new RotaryCategory(info, true);
	}

	/** Liquid to gas, over a burner. */
	public static RotaryCategory decondensentrating(Info<RotaryRecipe> info) {
		return new RotaryCategory(info, false);
	}

	private HeatCondition requiredHeat() {
		return condensentrating ? CKHeatLevels.CHILLED_CONDITION : HeatCondition.HEATED;
	}

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, RotaryRecipe recipe, IFocusGroup focuses) {
		// A single slot either side, placed where the basin layout centres a one-slot row.
		int inX = 36;
		int outX = 142;
		int y = 51;

		if (condensentrating) {
			builder.addSlot(RecipeIngredientRole.INPUT, inX, y)
				.setBackground(getRenderedSlot(), -1, -1)
				.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(recipe.getChemicalInput()
					.amount(), 16, 16))
				.addIngredients(MekanismJEI.TYPE_CHEMICAL, recipe.getChemicalInput()
					.getRepresentations());
			for (FluidStack output : recipe.getFluidOutputDefinition())
				addFluidSlot(builder, outX, y, output);
		} else {
			addFluidSlot(builder, inX, y, recipe.getFluidInput()
				.ingredient());
			for (ChemicalStack output : recipe.getChemicalOutputDefinition())
				builder.addSlot(RecipeIngredientRole.OUTPUT, outX, y)
					.setBackground(getRenderedSlot(), -1, -1)
					.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(output.getAmount(), 16, 16))
					.addIngredient(MekanismJEI.TYPE_CHEMICAL, output);
		}
	}

	@Override
	protected void draw(RotaryRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX,
		double mouseY) {
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 32);

		// A burner glows; a chiller only casts a shadow.
		(condensentrating ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT).render(graphics, 81, 88);

		HeatCondition heat = requiredHeat();
		AllGuiTextures.JEI_HEAT_BAR.render(graphics, 4, 80);
		graphics.drawString(Minecraft.getInstance().font, CreateLang.translateDirect(heat.getTranslationKey()), 9, 86,
			heat.getColor(), false);

		int centerX = getBackground().getWidth() / 2 + 3;
		heater.withHeat(heat.visualizeAsBlazeBurner())
			.draw(graphics, centerX, HEATER_ANCHOR);
		machine.draw(graphics, centerX, MACHINE_ANCHOR);
	}
}
