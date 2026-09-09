package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import mekanism.client.recipe_viewer.jei.ChemicalStackRenderer;
import mekanism.client.recipe_viewer.jei.MekanismJEI;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedMechanicalElectrolyzer;
import me.moonscenty.createkinetism.content.recipe.SeparatingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Mechanical Electrolyzer recipes: one fluid split into two chemicals, one per side of the shaft.
 *
 * <p>The layout {@link BasinRecipeCategory} draws for every plain vat, with the usual fluid/item
 * output slots replaced by two chemical ones - the products never touch the basin at all, see
 * {@link me.moonscenty.createkinetism.content.vat.MechanicalElectrolyzerBlockEntity}. Used to share
 * {@link VatCategory} with the other plain vats before it grew chemical outputs.</p>
 */
@ParametersAreNonnullByDefault
public class SeparatingCategory extends BasinRecipeCategory<SeparatingRecipe> {

	private final AnimatedMechanicalElectrolyzer separator = new AnimatedMechanicalElectrolyzer();

	public SeparatingCategory(Info<SeparatingRecipe> info) {
		super(info);
	}

	@Override
	protected int extraOutputSlots(SeparatingRecipe recipe) {
		return recipe.chemicalResults()
			.size();
	}

	/**
	 * Own renderer rather than the one Mekanism registers globally: that one is built for the
	 * ingredient list and leaves the amount out of the tooltip - see {@link OxidizingCategory}.
	 */
	@Override
	protected void addExtraOutputSlot(IRecipeLayoutBuilder builder, SeparatingRecipe recipe, int index, int x, int y) {
		var chemical = recipe.chemicalResults()
			.get(index);
		builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
			.setBackground(getRenderedSlot(), -1, -1)
			.setCustomRenderer(MekanismJEI.TYPE_CHEMICAL, new ChemicalStackRenderer(chemical.getAmount(), 16, 16))
			.addIngredients(MekanismJEI.TYPE_CHEMICAL, List.of(chemical));
	}

	@Override
	protected int machineAnchor() {
		return 34;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		separator.draw(graphics, centerX, anchorY);
	}
}
