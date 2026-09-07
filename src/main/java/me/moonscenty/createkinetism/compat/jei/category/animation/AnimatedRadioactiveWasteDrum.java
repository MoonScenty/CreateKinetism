package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The drum, for the JEI panel.
 *
 * <p>Nothing moves - the block has no parts and takes no rotation, and drawing it turning would be
 * a lie about what it needs. {@link AnimatedKinetics} is still the right base: what is wanted here
 * is its block-in-a-panel projection, not its animation.</p>
 */
public class AnimatedRadioactiveWasteDrum extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		blockElement(CKBlocks.RADIOACTIVE_WASTE_DRUM.getDefaultState()).scale(23)
			.render(graphics);

		ms.popPose();
	}
}
