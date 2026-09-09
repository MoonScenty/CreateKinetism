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
 * The Electrolytic Separator over its basin, for the JEI panel.
 *
 * <p>{@code AnimatedMixer} with the parts {@link me.moonscenty.createkinetism.content.vat.VatRenderer}
 * actually swaps in for this block: a turning shaft in place of the cogwheel, since this vat is driven
 * end on end rather than by a cog on the lid, and the separator's own pole and head instead of the
 * mixer's. The head does not spin here either, matching the real renderer - it only travels with the
 * pole, since this machine splits a fluid rather than stirring one.</p>
 */
public class AnimatedElectrolyticSeparator extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		blockElement(shaft(Direction.Axis.Z)).rotateBlock(0, 0, getCurrentAngle() * 2)
			.atLocal(0, 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.ELECTROLYTIC_SEPARATOR.getDefaultState()).atLocal(0, 0, 0)
			.scale(scale)
			.render(graphics);

		float animation = ((Mth.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 5) + .5f;

		blockElement(CKPartialModels.ELECTROLYTIC_SEPARATOR_POLE).atLocal(0, animation, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKPartialModels.ELECTROLYTIC_SEPARATOR_HEAD).atLocal(0, animation, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.BASIN.getDefaultState()).atLocal(0, 1.65, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
