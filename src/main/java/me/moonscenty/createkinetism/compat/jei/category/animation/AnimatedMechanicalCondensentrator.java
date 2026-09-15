package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;

/**
 * The Mechanical Condensentrator for the JEI panel: the housing, the table hanging under it and the
 * basin a block further down, table and basin spinning clockwise together as they do in world.
 *
 * <p>{@link AnimatedIsotopicCentrifuge} with the basin moved from above to below - {@code atLocal}'s Y
 * runs opposite to world space, so a block down is {@code +1}. The heater under the basin is not part
 * of this: the category draws it with Create's own burner animation, which puts the right block there
 * for either heat.</p>
 */
public class AnimatedMechanicalCondensentrator extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		// Default state is axis=x - see AnimatedIsotopicCentrifuge - so the shaft runs along X too.
		blockElement(shaft(Direction.Axis.X)).rotateBlock(getCurrentAngle(), 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.MECHANICAL_CONDENSENTRATOR.getDefaultState()).scale(scale)
			.render(graphics);

		// Clockwise seen from above, like the block's own spin.
		float angle = -getCurrentAngle();

		blockElement(CKPartialModels.MECHANICAL_CONDENSENTRATOR_HEAD).rotateBlock(0, angle, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKPartialModels.CLOSED_BASIN).rotateBlock(0, angle, 0)
			.atLocal(0, 1, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
