package me.moonscenty.createkinetism.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import mekanism.api.chemical.ChemicalStack;
import mekanism.client.render.MekanismRenderer;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.FluidRenderHelper;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Draws a box of Mekanism chemical, the way Create draws a box of fluid.
 *
 * <p>Create's own renderer takes a {@code FluidStack} and there is no fluid form of an infuse type
 * to hand it, which is why the Metallurgic Infuser lost its contents and its falling stream when it
 * moved off fluids. Nothing about the geometry needed a fluid, though - only a sprite and a colour -
 * and {@link FluidRenderHelper#renderStillTiledFace} takes exactly those. So this is Catnip's own
 * {@code renderFluidBox} with its two fluid lookups swapped for Mekanism's.</p>
 *
 * <p>Client only: {@link MekanismRenderer} lives in Mekanism's client package, and everything that
 * calls this is a block entity renderer or a JEI panel.</p>
 */
public class ChemicalBoxRenderer {

	/**
	 * @param renderBottom whether to draw the underside. False for something sitting in a housing,
	 *                     where the bottom face is never visible and costs four vertices anyway.
	 */
	public static void renderChemicalBox(ChemicalStack stack, float xMin, float yMin, float zMin,
		float xMax, float yMax, float zMax, MultiBufferSource buffer, PoseStack ms, int light,
		boolean renderBottom) {
		if (stack.isEmpty())
			return;

		TextureAtlasSprite sprite = MekanismRenderer.getChemicalTexture(stack);
		// The sprite is greyscale and tinted at draw time, the same as Mekanism's own tanks. Opaque:
		// getTint carries no alpha of its own.
		int color = 0xFF000000 | stack.getChemical()
			.getTint();
		VertexConsumer builder = FluidRenderHelper.getFluidBuilder(buffer);

		for (Direction side : Iterate.directions) {
			if (side == Direction.DOWN && !renderBottom)
				continue;

			boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
			if (side.getAxis()
				.isHorizontal()) {
				if (side.getAxis() == Direction.Axis.X)
					FluidRenderHelper.renderStillTiledFace(side, zMin, yMin, zMax, yMax,
						positive ? xMax : xMin, builder, ms, light, color, sprite);
				else
					FluidRenderHelper.renderStillTiledFace(side, xMin, yMin, xMax, yMax,
						positive ? zMax : zMin, builder, ms, light, color, sprite);
			} else {
				FluidRenderHelper.renderStillTiledFace(side, xMin, zMin, xMax, zMax,
					positive ? yMax : yMin, builder, ms, light, color, sprite);
			}
		}
	}
}
