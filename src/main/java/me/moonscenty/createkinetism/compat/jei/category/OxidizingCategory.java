package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedOxidationChamber;
import me.moonscenty.createkinetism.content.recipe.VatRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Oxidation Chamber recipes.
 *
 * <p>The same panel {@link VatCategory} draws - it is a basin recipe like every other vat's, hung
 * underneath the machine - with the machine picture swapped. The block stopped being Create's mixer
 * when it took the Injection Chamber's housing, so drawing a mixer here would name the wrong
 * machine.</p>
 */
@ParametersAreNonnullByDefault
public class OxidizingCategory extends BasinRecipeCategory<VatRecipe> {

	private final AnimatedOxidationChamber chamber = new AnimatedOxidationChamber();

	public OxidizingCategory(Info<VatRecipe> info) {
		super(info);
	}

	@Override
	protected int machineAnchor() {
		return 34;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		chamber.draw(graphics, centerX, anchorY);
	}
}
