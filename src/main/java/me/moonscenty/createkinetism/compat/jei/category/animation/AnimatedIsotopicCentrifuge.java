package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.content.centrifuge.IsotopicCentrifugeBlockEntity;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;

/**
 * The Isotopic Centrifuge, swinging its basin, for the JEI panel.
 *
 * <p>{@link AnimatedDissolutionChamber} is the one to read alongside this: the two machines share a
 * model, and the panel is the only place a player can tell them apart before building one. So the
 * difference has to be visible here - that one tips its table a few degrees, this one swings it a
 * quarter turn each way.</p>
 *
 * <p>Turning about Y makes this simpler than the rocking version. Both the table and the basin turn
 * about the same vertical line through the block's centre, which is exactly what {@code rotateBlock}
 * pins them to, so neither needs an explicit pivot.</p>
 */
public class AnimatedIsotopicCentrifuge extends AnimatedKinetics {

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

		blockElement(CKBlocks.ISOTOPIC_CENTRIFUGE.getDefaultState()).scale(scale)
			.render(graphics);

		float angle = IsotopicCentrifugeBlockEntity.swingAngle(AnimationTickHolder.getRenderTime() / 40f);

		blockElement(CKPartialModels.ISOTOPIC_CENTRIFUGE_HEAD).rotateBlock(0, angle, 0)
			.scale(scale)
			.render(graphics);

		// atLocal's Y runs opposite to world space - see AnimatedDissolutionChamber - so the basin
		// riding above the machine takes a negative offset.
		blockElement(CKPartialModels.CLOSED_BASIN).rotateBlock(0, angle, 0)
			.atLocal(0, -1, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
