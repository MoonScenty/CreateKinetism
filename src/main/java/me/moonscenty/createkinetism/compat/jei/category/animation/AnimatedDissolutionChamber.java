package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.content.dissolution.DissolutionChamberBlockEntity;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Dissolution Chamber, rocking its basin, for the JEI panel.
 *
 * <p>Built like {@code AnimatedPurificationVibrator}, and the two are worth reading together: the
 * vibrator shakes its head straight up and down, this one tips it. The panel is where a player finds
 * out which of the two they are looking at, so the difference has to survive into the animation.</p>
 *
 * <p>The rocking uses the same cubed sine the real block entity does, so the panel and the machine
 * in the world move alike - eased at the ends of the stroke, quick through the middle.</p>
 */
public class AnimatedDissolutionChamber extends AnimatedKinetics {

	/** The table's underside, where it meets the piston - the same pivot the world renderer uses. */
	private static final double PIVOT_Y = 12 / 16d;

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		// The default state is built along Z (see the blockstate's axis=z variant), so that is the
		// shaft's own rotation axis - and the axis the table tips about.
		blockElement(shaft(Direction.Axis.Z)).rotateBlock(0, 0, getCurrentAngle())
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.DISSOLUTION_CHAMBER.getDefaultState()).scale(scale)
			.render(graphics);

		float phase = Mth.sin(AnimationTickHolder.getRenderTime() / 16f);
		float angle = phase * phase * phase * DissolutionChamberBlockEntity.ROCK_ANGLE;

		// The table and the basin have to tip as one body, so both turn about the same point rather
		// than about their own centres. rotateBlock would pin each to its own centre - which leaves the
		// basin spinning in place instead of riding the table - so the pivot is given explicitly.
		//
		// rotationOffset is applied in the element's own local space, so the shared pivot is written
		// once for the table and again for the basin with a block subtracted, that being how far the
		// basin's origin sits above the table's.
		blockElement(CKPartialModels.DISSOLUTION_CHAMBER_HEAD).rotate(0, 0, angle)
			.withRotationOffset(new Vec3(0.5, PIVOT_Y, 0.5))
			.scale(scale)
			.render(graphics);

		// atLocal's Y runs opposite to world space - see AnimatedPurificationVibrator - so the basin
		// riding above the machine takes a negative offset.
		blockElement(AllBlocks.BASIN.getDefaultState()).rotate(0, 0, angle)
			.withRotationOffset(new Vec3(0.5, PIVOT_Y - 1, 0.5))
			.atLocal(0, -1, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
