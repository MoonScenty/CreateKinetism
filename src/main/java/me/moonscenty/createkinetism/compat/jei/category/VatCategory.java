package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.animations.AnimatedMixer;

import me.moonscenty.createkinetism.content.recipe.VatRecipe;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Every vat that is not a machine of its own - washing, crystallizing, oxidizing, chemical infusing,
 * separating, evaporating.
 *
 * <p>They share one category class because they share one block: what tells them apart is the recipe
 * type, and JEI already puts that in the tab title and the catalyst. Create's own Mixer animation is
 * the right picture for all of them, since a vat <em>is</em> a mixer with a different recipe type.</p>
 *
 * <p>The anchor is Create's own 34 rather than the mirrored 69 the basin-carrying machines use: a vat
 * hangs its basin underneath, the way Create's Mixing panel does.</p>
 */
@ParametersAreNonnullByDefault
public class VatCategory extends BasinRecipeCategory<VatRecipe> {

	private final AnimatedMixer mixer = new AnimatedMixer();

	public VatCategory(Info<VatRecipe> info) {
		super(info);
	}

	@Override
	protected int machineAnchor() {
		return 34;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		mixer.draw(graphics, centerX, anchorY);
	}
}
