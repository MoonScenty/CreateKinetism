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
 * The Combiner over its basin, for the JEI panel.
 *
 * <p>Built like Create's {@code AnimatedMixer} but with our block and our whisk, so the panel shows
 * the machine the recipe actually needs. The whisk travels without turning, which is how the
 * Combiner behaves in world - only the cogwheel spins.</p>
 */
public class AnimatedCombiner extends AnimatedKinetics {

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

		blockElement(CKBlocks.COMBINER.getDefaultState()).scale(scale)
			.render(graphics);

		float travel = ((Mth.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 5) + .5f;

		blockElement(CKPartialModels.COMBINER_POLE).atLocal(0, travel, 0)
			.scale(scale)
			.render(graphics);

		// No rotateBlock on the head: the Combiner's whisk goes up and down and never spins.
		blockElement(CKPartialModels.COMBINER_HEAD).atLocal(0, travel, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.BASIN.getDefaultState()).atLocal(0, 1.65, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
