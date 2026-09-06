package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedSolarNeutronActivator;
import me.moonscenty.createkinetism.content.recipe.ActivatingRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Solar Neutron Activator recipes.
 *
 * <p>A basin recipe like the vats' - one gas out of the basin, one back into it - so
 * {@link BasinRecipeCategory} lays the slots out unchanged. What differs is the picture: the basin
 * sits above the machine rather than below, because this is the one machine in the mod that works
 * upwards.</p>
 */
@ParametersAreNonnullByDefault
public class ActivatingCategory extends BasinRecipeCategory<ActivatingRecipe> {

	private final AnimatedSolarNeutronActivator activator = new AnimatedSolarNeutronActivator();

	public ActivatingCategory(Info<ActivatingRecipe> info) {
		super(info);
	}

	/** Low on the panel: the basin is drawn two blocks above the machine and needs the room. */
	@Override
	protected int machineAnchor() {
		return 71;
	}

	@Override
	protected int shadowAnchor() {
		return 69;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		activator.draw(graphics, centerX, anchorY);
	}
}
