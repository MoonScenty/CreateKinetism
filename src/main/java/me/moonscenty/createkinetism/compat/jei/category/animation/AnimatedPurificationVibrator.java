package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * The Purification Vibrator, shaking its basin, for the JEI panel.
 *
 * <p>Built like {@code AnimatedEnricher}: our own block and head rather than reusing a Create
 * animation that would draw the wrong machine. The shaft spins - this machine is driven front-to-back
 * rather than by a cogwheel on top, and the panel should say so - while the head and the installed
 * basin ride the same small tremor the real machine uses while it works.</p>
 *
 * <p>{@code atLocal}'s Y runs opposite to world space: catnip's {@code GuiRenderBuilder} applies it
 * before the pose stack is flipped for GUI rendering, so a positive value moves an element
 * <em>down</em> on screen - which is why the Combiner's basin, sitting below its machine, uses a
 * positive offset in its own JEI animation. Ours sits above, so it needs a negative one.</p>
 */
public class AnimatedPurificationVibrator extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		// The default state is built along Z (see the blockstate's axis=z variant), so that is the
		// shaft's own rotation axis.
		blockElement(shaft(Direction.Axis.Z)).rotateBlock(0, 0, getCurrentAngle())
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.PURIFICATION_VIBRATOR.getDefaultState()).scale(scale)
			.render(graphics);

		float shake = Mth.sin(AnimationTickHolder.getRenderTime() / 4f) * (1 / 16f);

		blockElement(CKPartialModels.PURIFICATION_VIBRATOR_HEAD).atLocal(0, -shake, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.BASIN.getDefaultState()).atLocal(0, -(1 + shake), 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
