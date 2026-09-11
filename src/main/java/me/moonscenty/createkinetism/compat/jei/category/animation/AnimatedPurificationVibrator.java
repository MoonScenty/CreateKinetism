package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

		// HORIZONTAL_AXIS lists X before Z, so the default state is axis=x and the blockstate turns
		// the body 90 degrees for it. The shaft has to be drawn on that same axis or it comes out
		// crossing the machine it is supposed to run through.
		blockElement(shaft(Direction.Axis.X)).rotateBlock(getCurrentAngle(), 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.PURIFICATION_VIBRATOR.getDefaultState()).scale(scale)
			.render(graphics);

		float shake = Mth.sin(AnimationTickHolder.getRenderTime() / 4f) * (1 / 16f);

		blockElement(CKPartialModels.PURIFICATION_VIBRATOR_HEAD).atLocal(0, -shake, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKPartialModels.CLOSED_BASIN).atLocal(0, -(1 + shake), 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
