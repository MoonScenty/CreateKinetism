package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The Pressurized Reaction Chamber on its basin, for the JEI panel.
 *
 * <p>Two things set it apart from the chambers next to it in the list. The basin sits one block
 * down rather than two, because this machine stands on it; and the only moving part is the shaft
 * running through the housing, which is also the only thing the real renderer draws - everything
 * else is in the blockstate.</p>
 */
public class AnimatedPressurizedReactionChamber extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		blockElement(CKBlocks.PRESSURIZED_REACTION_CHAMBER.getDefaultState()).scale(scale)
			.render(graphics);

		// The default state runs along X, which is the axis the shaft model is baked on - so it spins
		// about X here, and the block never needs the quarter turn the real renderer gives a Z one.
		blockElement(CKPartialModels.REACTION_CHAMBER_SHAFT).rotateBlock(getCurrentAngle() * 2, 0, 0)
			.scale(scale)
			.render(graphics);

		// One block, not the two every other vat leaves: the chamber has no gap under it.
		blockElement(AllBlocks.BASIN.getDefaultState()).atLocal(0, 1, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
