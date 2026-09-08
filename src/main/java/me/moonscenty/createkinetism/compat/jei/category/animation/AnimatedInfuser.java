package me.moonscenty.createkinetism.compat.jei.category.animation;

import java.util.List;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Mechanical Infuser over its depot, for the JEI panel.
 *
 * <p>Create's {@code AnimatedSpout} draws Create's spout, which would put the wrong machine beside a
 * recipe only ours can run. This is the same animation over our block, plus the cogwheel: the
 * infuser is the one spout in the game that will not work without a shaft turning, and the panel
 * should say so.</p>
 */
public class AnimatedInfuser extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 100);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));
		int scale = 20;

		blockElement(cogwheel()).rotateBlock(0, getCurrentAngle() * 2, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.MECHANICAL_METALLURGIC_INFUSER.getDefaultState()).scale(scale)
			.render(graphics);

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float squeeze = cycle < 20 ? Mth.sin((float) (cycle / 20f * Math.PI)) : 0;
		squeeze *= 20;

		ms.pushPose();
		blockElement(CKPartialModels.MECHANICAL_METALLURGIC_INFUSER_TOP).scale(scale)
			.render(graphics);
		ms.translate(0, -3 * squeeze / 32f, 0);
		blockElement(CKPartialModels.MECHANICAL_METALLURGIC_INFUSER_MIDDLE).scale(scale)
			.render(graphics);
		ms.translate(0, -3 * squeeze / 32f, 0);
		blockElement(CKPartialModels.MECHANICAL_METALLURGIC_INFUSER_BOTTOM).scale(scale)
			.render(graphics);
		ms.translate(0, -3 * squeeze / 32f, 0);
		ms.popPose();

		blockElement(AllBlocks.DEPOT.getDefaultState()).atLocal(0, 2, 0)
			.scale(scale)
			.render(graphics);

		// The infusion itself is not drawn here. It is a Mekanism chemical, and the renderer this used
		// takes a FluidStack - an infuse type has no fluid form to hand it. The panel names the
		// chemical in its own slot instead.
		AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
		graphics.flush();
		Lighting.setupFor3DItems();

		ms.popPose();
	}
}
