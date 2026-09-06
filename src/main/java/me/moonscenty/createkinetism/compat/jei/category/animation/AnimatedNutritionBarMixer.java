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
 * The Nutrition Bar Mixer over its basin, for the JEI panel.
 *
 * <p>Create's own {@code AnimatedMixer} would draw Create's Mechanical Mixer here, which is a
 * different block with different textures - the panel would be naming the wrong machine. This is
 * {@link AnimatedCombiner} with our block and our whisk instead.</p>
 *
 * <p>The whisk turns as well as travels, matching what
 * {@link me.moonscenty.createkinetism.content.vat.VatRenderer} does for this block in world -
 * unlike the other vats, this one is actually mixing.</p>
 */
public class AnimatedNutritionBarMixer extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		blockElement(cogwheel()).rotateBlock(0, getCurrentAngle() * 2, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.NUTRITION_BAR_MIXER.getDefaultState()).scale(scale)
			.render(graphics);

		float travel = ((Mth.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 5) + .5f;

		blockElement(CKPartialModels.NUTRITION_BAR_MIXER_POLE).atLocal(0, travel, 0)
			.scale(scale)
			.render(graphics);

		// Four times the cogwheel's, which is the ratio Create's own mixer panel uses.
		blockElement(CKPartialModels.NUTRITION_BAR_MIXER_HEAD).rotateBlock(0, getCurrentAngle() * 4, 0)
			.atLocal(0, travel, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.BASIN.getDefaultState()).atLocal(0, 1.65, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
