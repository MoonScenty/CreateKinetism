package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The Mechanical Chemistry Infuser for the JEI panel.
 *
 * <p>One block now, so there is nothing to animate but the shaft under it - which is also the only
 * part of the real block the renderer moves. No basin and no cogwheel: this machine takes its drive
 * straight up through its own underside.</p>
 *
 * <p>The gases are not drawn here. The panel's own slots name them, and a picture of a block with
 * three tinted windows at this size would read as noise.</p>
 */
public class AnimatedChemistryInfuser extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 100);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));
		int scale = 20;

		blockElement(CKPartialModels.CHEMISTRY_INFUSER_SHAFT).rotateBlock(0, getCurrentAngle() * 2, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.MECHANICAL_CHEMISTRY_INFUSER.getDefaultState()).scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
