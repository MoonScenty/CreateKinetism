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
 * The Injection Chamber over its basin, for the JEI panel.
 *
 * <p>Built like {@code AnimatedCombiner}: our own cog and head over Create's basin, not a Create
 * animation that would draw the wrong machine or the wrong surface underneath it. The head and its
 * chevrons plunge together on a continuous idle bob rather than the real block entity's recipe-timed
 * curve - JEI has no process to time against, the same reason the Combiner's whisk just bobs on a
 * fixed loop instead of replaying {@code VatBlockEntity}'s real cycle.</p>
 */
public class AnimatedInjectionChamber extends AnimatedKinetics {

	private List<FluidStack> fluids;

	public AnimatedInjectionChamber withFluids(List<FluidStack> fluids) {
		this.fluids = fluids;
		return this;
	}

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		blockElement(CKPartialModels.INJECTION_CHAMBER_COG).rotateBlock(0, getCurrentAngle() * 2, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.INJECTION_CHAMBER.getDefaultState()).scale(scale)
			.render(graphics);

		// 7/16 rest plus up to 10/16 more at the peak - the real plunge's range, on a continuous loop
		// rather than its recipe-timed curve.
		float plunge = 7 / 16f + (Mth.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 2f * (10 / 16f);

		blockElement(CKPartialModels.INJECTION_CHAMBER_HEAD).atLocal(0, plunge, 0)
			.scale(scale)
			.render(graphics);

		// On the body, not the plunging head - it marks where the gas goes in, not the plunger, so it
		// stays put while the head moves. Flipped the same way the real renderer flips it: 180 around
		// X turns the baked-upward chevron downward without mirroring it left-right.
		blockElement(CKPartialModels.INJECTION_CHAMBER_ARROWS).rotateBlock(180, 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.BASIN.getDefaultState()).atLocal(0, 1.65 + 5.5f / 16f, 0)
			.scale(scale)
			.render(graphics);

		AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
		FluidStack fluidStack = fluids.get(0);

		// The gas sitting in the chamber's own tank, static - the tank is in the housing, not on the
		// plunger.
		ms.pushPose();
		UIRenderHelper.flipForGuiRender(ms);
		ms.scale(16, 16, 16);
		float from = 3f / 16f;
		float to = 17f / 16f;
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, from, from, from, to, to, to,
			graphics.bufferSource(), ms, LightTexture.FULL_BRIGHT, false, true);
		ms.popPose();
		graphics.flush();
		Lighting.setupFor3DItems();

		ms.popPose();
	}
}
