package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;

/**
 * The Mechanical Enricher, turning, for the JEI panel.
 *
 * <p>Create's {@code AnimatedPress} would do the job structurally - the machines move the same way -
 * but it draws {@code AllBlocks.MECHANICAL_PRESS} and Create's press head, so the recipe panel would
 * show a Mechanical Press for a recipe only the Enricher can run. This is the same animation with
 * our own block and head.</p>
 */
public class AnimatedEnricher extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 24;

		// The default state faces north, so its rotation axis - and the shaft through it - is Z.
		blockElement(shaft(Direction.Axis.Z)).rotateBlock(0, 0, getCurrentAngle())
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.MECHANICAL_ENRICHER.getDefaultState()).scale(scale)
			.render(graphics);

		blockElement(CKPartialModels.MECHANICAL_ENRICHER_HEAD).atLocal(0, -headOffset(), 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}

	/** Create's press curve: a cubic drop, a pause at the bottom, then a linear lift. */
	private float headOffset() {
		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		if (cycle < 10) {
			float progress = cycle / 10;
			return -(progress * progress * progress);
		}
		if (cycle < 15)
			return -1;
		if (cycle < 20)
			return -1 + (1 - ((20 - cycle) / 5));
		return 0;
	}
}
