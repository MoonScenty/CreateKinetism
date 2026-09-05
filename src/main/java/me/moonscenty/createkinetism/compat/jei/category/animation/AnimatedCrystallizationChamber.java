package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * The Crystallization Chamber over its basin, for the JEI panel.
 *
 * <p>{@link AnimatedInjectionChamber} without the fluid pass, the same as
 * {@link AnimatedOxidationChamber}: same housing, same plunge, but nothing is drawn inside it because
 * this machine holds nothing of its own - the slurry it eats and the crystal it makes both sit in the
 * basin, which draws its own contents.</p>
 */
public class AnimatedCrystallizationChamber extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		blockElement(CKPartialModels.CRYSTALLIZATION_CHAMBER_COG).rotateBlock(0, getCurrentAngle() * 2, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.CRYSTALLIZATION_CHAMBER.getDefaultState()).scale(scale)
			.render(graphics);

		// 7/16 rest plus up to 10/16 more at the peak - the real plunge's range, on a continuous loop
		// rather than its recipe-timed curve.
		float plunge = 7 / 16f + (Mth.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 2f * (10 / 16f);

		blockElement(CKPartialModels.CRYSTALLIZATION_CHAMBER_HEAD).atLocal(0, plunge, 0)
			.scale(scale)
			.render(graphics);

		// Flipped the same way the real renderer flips it: 180 around X turns the baked-upward chevron
		// downward without mirroring it left-right.
		blockElement(CKPartialModels.CRYSTALLIZATION_CHAMBER_ARROWS).rotateBlock(180, 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.BASIN.getDefaultState()).atLocal(0, 1.65 + 5.5f / 16f, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
