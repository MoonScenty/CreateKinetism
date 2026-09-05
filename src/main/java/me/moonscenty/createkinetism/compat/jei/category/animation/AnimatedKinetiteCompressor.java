package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/**
 * The Kinetite Compressor at work, for the JEI panel.
 *
 * <p>Both halves are drawn: the front block carries the frame and the spinning head, the cradle
 * behind it the ram. Two shafts turn, on the two axes the real machine is driven on, because a panel
 * showing one shaft would hide the whole point of the block.</p>
 *
 * <p>The ram runs a plain loop rather than the block entity's recipe-timed curve - JEI has no process
 * to time against, the same reason the other machines here bob on a fixed cycle.</p>
 */
public class AnimatedKinetiteCompressor extends AnimatedKinetics {

	/** Ten pixels, the travel the model was drawn to. */
	private static final float TRAVEL = 10 / 16f;

	/**
	 * The middle of the block behind, in the default state's own space.
	 *
	 * <p>The machine is modelled facing north and two blocks deep, so the cradle occupies z 1 to 2 and
	 * its centre - the pivot the cross axle turns on - sits at 1.5.</p>
	 */
	private static final Vec3 CRADLE_CENTRE = new Vec3(0.5, 0.5, 1.5);

	/**
	 * Half a block forward, so the pair of blocks is centred on the anchor rather than the front one.
	 *
	 * <p>Every other machine here is one block, and its middle is half a block from its origin - which
	 * is what the panel's anchor and its shadow are placed against. This one runs z 0 to 2, so its
	 * middle is a whole block out, and drawing it unshifted hangs it off to one side no matter how the
	 * anchor is nudged. Moving it back half a block puts its centre where a one-block machine's would
	 * be, and the usual placement then works.</p>
	 */
	private static final float CENTRED = -0.5f;

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		// A quarter turn past Create's usual 22.5, so the machine's long axis runs across the panel
		// instead of away from the viewer. Two blocks seen end-on read as one block; seen side-on they
		// read as the machine it is.
		ms.mulPose(Axis.YP.rotationDegrees(112.5f));

		double scale = 22.5;

		// No generic Create shaft here: this machine draws its own stub, and laying Create's on top of
		// it is what left an axle poking out of the front.
		// The default state faces north, so the front shaft runs along Z and the cross axle along X.
		blockElement(CKPartialModels.KINETITE_COMPRESSOR_INPUT_SHAFT).rotateBlock(0, 0, getCurrentAngle())
			.atLocal(0, 0, CENTRED)
			.scale(scale)
			.render(graphics);
		blockElement(CKPartialModels.KINETITE_COMPRESSOR_ROTATING_HEAD).rotateBlock(0, 0, getCurrentAngle())
			.atLocal(0, 0, CENTRED)
			.scale(scale)
			.render(graphics);
		// Not rotateBlock: that pivots on this block's centre, and the cross axle is drawn a block
		// further back, so it would swing round in an arc instead of turning on the spot. Same trap
		// the world renderer had - see KinetiteCompressorRenderer#spinBehind.
		blockElement(CKPartialModels.KINETITE_COMPRESSOR_OUTPUT_SHAFT).rotate(getCurrentAngle(), 0, 0)
			.withRotationOffset(CRADLE_CENTRE)
			.atLocal(0, 0, CENTRED)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.KINETITE_COMPRESSOR.getDefaultState()).atLocal(0, 0, CENTRED)
			.scale(scale)
			.render(graphics);

		blockElement(CKPartialModels.KINETITE_COMPRESSOR_MOVING_HEAD)
			.atLocal(0, 0, CENTRED - ramOffset())
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}

	/** Out, touch, and back - the cycle the real machine runs, on a fixed loop. */
	private float ramOffset() {
		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 60 / 60;
		return (cycle <= .5f ? cycle : 1 - cycle) * 2 * TRAVEL;
	}
}
