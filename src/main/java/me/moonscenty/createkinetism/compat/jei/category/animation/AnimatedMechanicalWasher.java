package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The Mechanical Washer, auger turning, for the JEI panel.
 *
 * <p>No basin is drawn: unlike every other machine with a panel in this mod, this one holds its
 * fluid in its own vessel. The panel showing a bare machine rather than a machine over a basin is
 * how a player finds that out.</p>
 */
public class AnimatedMechanicalWasher extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		blockElement(CKBlocks.MECHANICAL_WASHER.getDefaultState()).scale(scale)
			.render(graphics);

		// The auger turns about Y, which is the shaft's own axis - it is driven from below.
		blockElement(CKPartialModels.MECHANICAL_WASHER_PROPELLER).rotateBlock(0, getCurrentAngle() * 2, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
