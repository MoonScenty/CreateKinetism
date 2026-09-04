package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.content.recipe.EvaporatingRecipe;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Evaporation Plant recipes. The slot layout is the shared Basin one - a recipe's shape does not
 * care that the plant runs it as a slow in-place conversion rather than a Basin cycle - but there is
 * no animation to draw yet, just the block's own icon standing in for it.
 */
@ParametersAreNonnullByDefault
public class EvaporatingCategory extends BasinRecipeCategory<EvaporatingRecipe> {

	public EvaporatingCategory(Info<EvaporatingRecipe> info) {
		super(info);
	}

	@Override
	protected int machineAnchor() {
		return 50;
	}

	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
		graphics.renderItem(new ItemStack(CKBlocks.EVAPORATION_PLANT.get()), centerX - 8, anchorY - 8);
	}
}
