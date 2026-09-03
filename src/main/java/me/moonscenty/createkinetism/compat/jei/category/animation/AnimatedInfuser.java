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

	private List<FluidStack> fluids;

	public AnimatedInfuser withFluids(List<FluidStack> fluids) {
		this.fluids = fluids;
		return this;
	}

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

		blockElement(CKBlocks.MECHANICAL_INFUSER.getDefaultState()).scale(scale)
			.render(graphics);

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float squeeze = cycle < 20 ? Mth.sin((float) (cycle / 20f * Math.PI)) : 0;
		squeeze *= 20;

		ms.pushPose();
		blockElement(CKPartialModels.MECHANICAL_INFUSER_TOP).scale(scale)
			.render(graphics);
		ms.translate(0, -3 * squeeze / 32f, 0);
		blockElement(CKPartialModels.MECHANICAL_INFUSER_MIDDLE).scale(scale)
			.render(graphics);
		ms.translate(0, -3 * squeeze / 32f, 0);
		blockElement(CKPartialModels.MECHANICAL_INFUSER_BOTTOM).scale(scale)
			.render(graphics);
		ms.translate(0, -3 * squeeze / 32f, 0);
		ms.popPose();

		blockElement(AllBlocks.DEPOT.getDefaultState()).atLocal(0, 2, 0)
			.scale(scale)
			.render(graphics);

		AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
		FluidStack fluidStack = fluids.get(0);

		// The infusion sitting in the tank
		ms.pushPose();
		UIRenderHelper.flipForGuiRender(ms);
		ms.scale(16, 16, 16);
		float from = 3f / 16f;
		float to = 17f / 16f;
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, from, from, from, to, to, to,
			graphics.bufferSource(), ms, LightTexture.FULL_BRIGHT, false, true);
		ms.popPose();

		// and the column of it falling onto the depot
		float width = 1 / 128f * squeeze;
		ms.translate(scale / 2f, scale * 1.5f, scale / 2f);
		UIRenderHelper.flipForGuiRender(ms);
		ms.scale(16, 16, 16);
		ms.translate(-0.5f, 0, -0.5f);
		from = -width / 2 + 0.5f;
		to = width / 2 + 0.5f;
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, from, 0, from, to, 2, to,
			graphics.bufferSource(), ms, LightTexture.FULL_BRIGHT, false, true);
		graphics.flush();
		Lighting.setupFor3DItems();

		ms.popPose();
	}
}
