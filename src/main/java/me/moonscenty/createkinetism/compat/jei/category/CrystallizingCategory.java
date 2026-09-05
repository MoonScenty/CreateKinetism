package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedCrystallizationChamber;
import me.moonscenty.createkinetism.content.recipe.VatRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Crystallization Chamber recipes.
 *
 * <p>{@link VatCategory}'s panel - it is a basin recipe like every other vat's, hung underneath the
 * machine - with the machine picture swapped, exactly as {@link OxidizingCategory} does it. The block
 * stopped being Create's mixer when it took the Injection Chamber's housing.</p>
 */
@ParametersAreNonnullByDefault
public class CrystallizingCategory extends BasinRecipeCategory<VatRecipe> {

	private final AnimatedCrystallizationChamber chamber = new AnimatedCrystallizationChamber();

	public CrystallizingCategory(Info<VatRecipe> info) {
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
